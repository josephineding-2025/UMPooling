-- Ticket snapshots, retry-safe reservations and pre-start passenger cancellation.
begin;
alter table public.trip_offers add column if not exists quote_version bigint not null default 0;
create or replace function public.bump_offer_quote_version()
returns trigger language plpgsql security definer set search_path=''
as $function$
begin
    if row(new.cost_per_seat,new.departure_time,new.origin_zone,new.destination_zone,new.distance_km,
           new.origin_geo::text,new.destination_geo::text)
       is distinct from row(old.cost_per_seat,old.departure_time,old.origin_zone,old.destination_zone,old.distance_km,
           old.origin_geo::text,old.destination_geo::text) then
        new.quote_version:=old.quote_version+1;
    else new.quote_version:=old.quote_version;end if;
    return new;
end;
$function$;
revoke all on function public.bump_offer_quote_version() from public,anon,authenticated;
create trigger bump_offer_quote_version before update on public.trip_offers
    for each row execute function public.bump_offer_quote_version();
alter table public.bookings
    add column if not exists booking_reference text,
    add column if not exists cost_per_seat_snapshot numeric,
    add column if not exists origin_zone_snapshot text,
    add column if not exists destination_zone_snapshot text,
    add column if not exists departure_time_snapshot timestamptz;
create or replace function public.snapshot_booking_ticket()
returns trigger language plpgsql security definer set search_path=''
as $function$
declare o public.trip_offers%rowtype;
begin
    select * into strict o from public.trip_offers where trip_offer_id=new.trip_offer_id;
    new.booking_reference:='UMP-'||upper(replace(new.booking_id::text,'-',''));
    new.cost_per_seat_snapshot:=o.cost_per_seat;
    new.origin_zone_snapshot:=o.origin_zone;
    new.destination_zone_snapshot:=o.destination_zone;
    new.departure_time_snapshot:=o.departure_time;
    return new;
end;
$function$;
revoke all on function public.snapshot_booking_ticket() from public,anon,authenticated;
create trigger snapshot_booking_ticket before insert on public.bookings
    for each row execute function public.snapshot_booking_ticket();
-- Existing tickets are not backfilled from mutable offers: their original price
-- is unknown. Consumers must label missing historical snapshots as unavailable.

create or replace function public.book_seat_transaction(
    p_trip_offer_id uuid,p_passenger_id uuid,p_seats integer,
    p_pickup_lng double precision,p_pickup_lat double precision
) returns uuid language plpgsql security definer set search_path=''
as $function$
declare
    v_user uuid:=auth.uid(); o public.trip_offers%rowtype;
    b public.bookings%rowtype; v_id uuid; v_schema text;
begin
    if v_user is null then raise exception 'Sign in before booking'; end if;
    if p_passenger_id is distinct from v_user then raise exception 'You cannot book for another user'; end if;
    if p_seats is null or p_seats<=0 then raise exception 'Seats must be a positive integer'; end if;
    if p_pickup_lng is null or not(p_pickup_lng between -180 and 180)
        or p_pickup_lat is null or not(p_pickup_lat between -90 and 90) then
        raise exception 'Valid pickup coordinates are required'; end if;
    select * into o from public.trip_offers where trip_offer_id=p_trip_offer_id for update;
    if not found then raise exception 'Trip offer not found'; end if;
    if o.driver_id=v_user then raise exception 'Drivers cannot book their own offer'; end if;
    select * into b from public.bookings where trip_offer_id=p_trip_offer_id and passenger_id=v_user;
    if found then
        if b.status in ('confirmed','completed') and b.seats_booked=p_seats then return b.booking_id; end if;
        raise exception 'A booking already exists for this offer; open its ticket';
    end if;
    if o.status::text is distinct from 'open' or exists(
        select 1 from public.trips where trip_offer_id=p_trip_offer_id and status::text<>'cancelled'
    ) then raise exception 'Trip offer is not open'; end if;
    if o.departure_time<=now() then raise exception 'Trip offer has already departed'; end if;
    if o.available_seats<p_seats then raise exception 'Not enough seats available'; end if;
    select n.nspname into strict v_schema from pg_catalog.pg_extension e
        join pg_catalog.pg_namespace n on n.oid=e.extnamespace where e.extname='postgis';
    execute format('insert into public.bookings(trip_offer_id,passenger_id,seats_booked,pickup_geo,status)
        values($1,$2,$3,%1$I.st_setsrid(%1$I.st_makepoint($4,$5),4326)::%1$I.geography,''confirmed'') returning booking_id',v_schema)
        into v_id using p_trip_offer_id,v_user,p_seats,p_pickup_lng,p_pickup_lat;
    update public.trip_offers set available_seats=available_seats-p_seats,
        status=(case when available_seats-p_seats=0 then 'full' else 'open' end)::public.offer_status
        where trip_offer_id=p_trip_offer_id;
    return v_id;
end;
$function$;
revoke all on function public.book_seat_transaction(uuid,uuid,integer,double precision,double precision) from public,anon;
grant execute on function public.book_seat_transaction(uuid,uuid,integer,double precision,double precision) to authenticated;

create or replace function public.cancel_booking(p_booking_id uuid)
returns integer language plpgsql security definer set search_path=''
as $function$
declare v_user uuid:=auth.uid(); v_offer uuid; o public.trip_offers%rowtype; b public.bookings%rowtype;
begin
    if v_user is null then raise exception 'Sign in first'; end if;
    select trip_offer_id into v_offer from public.bookings where booking_id=p_booking_id;
    if not found then raise exception 'Booking not found'; end if;
    -- Same offer-first locking order as reservation/start_trip avoids deadlocks.
    select * into o from public.trip_offers where trip_offer_id=v_offer for update;
    select * into b from public.bookings where booking_id=p_booking_id for update;
    if b.passenger_id is distinct from v_user then raise exception 'Only the passenger can cancel this booking'; end if;
    if b.status='cancelled' then return 0; end if;
    if b.status is distinct from 'confirmed' or b.trip_id is not null or not coalesce(o.status::text in ('open','full'),false)
       or exists(select 1 from public.trips where trip_offer_id=v_offer and status::text<>'cancelled') then
        raise exception 'Only a booking before trip start can be cancelled'; end if;
    update public.bookings set status='cancelled' where booking_id=p_booking_id;
    update public.trip_offers set available_seats=available_seats+b.seats_booked,status='open'::public.offer_status
        where trip_offer_id=v_offer;
    return b.seats_booked;
end;
$function$;
revoke all on function public.cancel_booking(uuid) from public,anon;
grant execute on function public.cancel_booking(uuid) to authenticated;
-- A real UI quotes an offer version. Reject changed route/price/departure,
-- while allowing retries to retrieve an already-created reservation unchanged.
create or replace function public.book_seat_with_quote(
    p_trip_offer_id uuid,p_seats integer,p_pickup_lng double precision,
    p_pickup_lat double precision,p_quote_version bigint
) returns uuid language plpgsql security definer set search_path=''
as $function$
declare o public.trip_offers%rowtype;v_user uuid:=auth.uid();
begin
    if v_user is null then raise exception 'Sign in before booking';end if;
    select * into o from public.trip_offers where trip_offer_id=p_trip_offer_id for update;
    if not found then raise exception 'Trip offer not found';end if;
    if not exists(select 1 from public.bookings where trip_offer_id=p_trip_offer_id
        and passenger_id=v_user and seats_booked=p_seats and status in('confirmed','completed'))
        and o.quote_version is distinct from p_quote_version then
        raise exception 'Offer details changed; refresh before booking';end if;
    return public.book_seat_transaction(p_trip_offer_id,v_user,p_seats,p_pickup_lng,p_pickup_lat);
end;
$function$;
revoke all on function public.book_seat_with_quote(uuid,integer,double precision,double precision,bigint) from public,anon;
grant execute on function public.book_seat_with_quote(uuid,integer,double precision,double precision,bigint) to authenticated;
commit;
