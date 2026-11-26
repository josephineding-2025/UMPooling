-- FR3.1/FR3.2/FR5.3. Requires the existing schema and migrations 1–8.
-- The version ending 09 belongs to Member 5's matching integration.
-- No historical trip is automatically credited by this migration.
begin;

-- One server contract for the configurable contribution cap. The old fixed
-- RM10 check must not silently override a newly configured cap.
alter table public.trip_offers drop constraint if exists "trip_offers_costPerSeat_check";
alter table public.trip_offers drop constraint if exists trip_offers_cost_per_seat_check;
alter table public.trip_offers add constraint trip_offers_nonnegative_cost
    check (cost_per_seat >= 0 and cost_per_seat <> 'NaN'::numeric) not valid;
create or replace function public.max_cost_per_seat()
returns numeric language plpgsql stable security definer set search_path = ''
as $function$
declare v_cap numeric;
begin
    select value into v_cap from public.app_config where key = 'max_cost_per_seat';
    if v_cap is null or not (v_cap between 0 and 999.99) then
        raise exception 'Configure a valid max_cost_per_seat between 0 and 999.99';
    end if;
    return v_cap;
end;
$function$;
revoke all on function public.max_cost_per_seat() from public, anon;
grant execute on function public.max_cost_per_seat() to authenticated;
create or replace function public.enforce_cost_cap()
returns trigger language plpgsql security definer set search_path = ''
as $function$
begin
    if new.cost_per_seat is null or not (new.cost_per_seat between 0 and public.max_cost_per_seat()) then
        raise exception 'Contribution exceeds the configured cap or is invalid';
    end if;
    return new;
end;
$function$;

-- Server implementation of Member 4's estimateCo2() contract. Kotlin remains
-- the preview implementation; persisted awards never trust client-submitted CO2.
create or replace function public.estimate_trip_co2(
    p_distance numeric, p_passengers integer,
    p_vehicle text default 'car', p_fuel text default 'petrol'
) returns numeric language sql immutable set search_path = ''
as $function$
    select case when p_distance is null or p_distance <= 0 or p_distance >= 'Infinity'::numeric
                     or p_passengers is null or p_passengers <= 0 then 0
        else p_passengers * p_distance * case
            when lower(btrim(coalesce(p_vehicle,''))) in ('motorcycle','bike') then 0.08
            when lower(btrim(coalesce(p_fuel,''))) = 'diesel' then 0.22
            when lower(btrim(coalesce(p_fuel,''))) = 'hybrid' then 0.13
            when lower(btrim(coalesce(p_fuel,''))) in ('ev','electric') then 0.07
            else 0.20 end end;
$function$;
revoke all on function public.estimate_trip_co2(numeric,integer,text,text) from public, anon;
grant execute on function public.estimate_trip_co2(numeric,integer,text,text) to authenticated, service_role;

alter table public.trips
    add column if not exists vehicle_type_snapshot text,
    add column if not exists fuel_category_snapshot text,
    add column if not exists distance_source text;
alter table public.user_impact_stats
    add column if not exists current_tier_level integer not null default 0,
    add column if not exists current_tier_name text not null default 'No Badge';

-- Snapshot route/profile inputs at start, not from a subsequently edited profile.
-- Discover the installed PostGIS schema instead of assuming public/extensions.
create or replace function public.snapshot_trip_impact_inputs()
returns trigger language plpgsql security definer set search_path = ''
as $function$
declare v_schema text; v_estimate double precision; v_offer public.trip_offers%rowtype;
begin
    select * into strict v_offer from public.trip_offers where trip_offer_id = new.trip_offer_id;
    select vehicle_type, fuel_category into new.vehicle_type_snapshot, new.fuel_category_snapshot
        from public.users where "usersId" = new.driver_id;
    if new.distance_km is null then
        select n.nspname into strict v_schema from pg_catalog.pg_extension e
            join pg_catalog.pg_namespace n on n.oid=e.extnamespace where e.extname='postgis';
        execute format('select %I.st_distance($1,$2) / 1000.0 * 1.3',v_schema)
            into v_estimate using v_offer.origin_geo,v_offer.destination_geo;
        new.distance_km := round(v_estimate::numeric,2);
        new.distance_source := 'geodesic_road_estimate';
    else
        new.distance_source := 'offer_route';
    end if;
    return new;
end;
$function$;
revoke all on function public.snapshot_trip_impact_inputs() from public,anon,authenticated;
create trigger snapshot_trip_impact_inputs before insert on public.trips
    for each row execute function public.snapshot_trip_impact_inputs();

-- Preserve pre-existing personal totals. Historical amounts are not fabricated
-- into weekly cells: those require an audited event-by-event backfill.
create table public.impact_opening_balances as
    select user_id,coalesce(total_trips,0) as total_trips,
        coalesce(total_distance_km,0) as total_distance_km,
        coalesce(total_co2_saved_kg,0) as total_co2_saved_kg
    from public.user_impact_stats;
alter table public.impact_opening_balances add primary key(user_id);
alter table public.impact_opening_balances enable row level security;
revoke all on public.impact_opening_balances from public,anon,authenticated;

-- Immutable event/participant snapshots make duplicate completion harmless and
-- preserve faculty/college at completion instead of regrouping edited profiles.
create table public.trip_impact_events (
    trip_id uuid primary key references public.trips(trip_id),
    completed_at timestamptz not null,
    trip_day date not null,
    week_start date not null,
    distance_km numeric not null,
    passenger_count integer not null check (passenger_count > 0),
    total_co2_kg numeric not null,
    vehicle_type text,
    fuel_category text
);
create table public.trip_impact_credits (
    trip_id uuid not null references public.trip_impact_events(trip_id),
    user_id uuid not null references public.users("usersId"),
    trip_day date not null,
    week_start date not null,
    occupancy_weight integer not null check (occupancy_weight > 0),
    credited boolean not null,
    distance_km numeric not null,
    co2_kg numeric not null,
    faculty text,
    residential_area text,
    residential_college text,
    primary key (trip_id,user_id)
);
create index trip_impact_credit_daily on public.trip_impact_credits(user_id,trip_day) where credited;
create index trip_impact_credit_weekly on public.trip_impact_credits(week_start) where credited;
create table public.impact_reporting_cells (
    week_start date not null,
    group_type text not null check (group_type in ('faculty','college')),
    group_name text not null,
    active_user_count integer not null,
    trip_count integer not null,
    total_distance_km numeric not null,
    total_co2_saved_kg numeric not null,
    primary key (week_start,group_type,group_name)
);
alter table public.trip_impact_events enable row level security;
alter table public.trip_impact_credits enable row level security;
alter table public.impact_reporting_cells enable row level security;
revoke all on public.trip_impact_events,public.trip_impact_credits,public.impact_reporting_cells
    from public,anon,authenticated;
grant select on public.trip_impact_credits,public.impact_reporting_cells to authenticated;
create policy "Read own impact ledger" on public.trip_impact_credits for select to authenticated
    using (user_id=(select auth.uid()));
create or replace function public.is_staff_analyst()
returns boolean language sql stable security definer set search_path = ''
as $function$
    select auth.uid() is not null and exists (
        select 1 from public.users where "usersId"=auth.uid() and "usersRole"::text='staffAnalyst'
    );
$function$;
revoke all on function public.is_staff_analyst() from public,anon;
grant execute on function public.is_staff_analyst() to authenticated;
create policy "Staff read groups of five" on public.impact_reporting_cells for select to authenticated
    using (public.is_staff_analyst() and active_user_count >= 5);
create view public.staff_impact_dashboard with (security_invoker=true,security_barrier=true) as
    select week_start,group_type,group_name,active_user_count,trip_count,
           total_distance_km,total_co2_saved_kg
    from public.impact_reporting_cells where active_user_count >= 5;
revoke all on public.staff_impact_dashboard from public,anon,authenticated;
grant select on public.staff_impact_dashboard to authenticated;
-- The old impact_aggregates and anonymised_impact_dashboard stay private and
-- unmodified. Legacy counts have not been verified or backfilled.

create or replace function public.record_completed_trip_impact()
returns trigger language plpgsql security definer set search_path = ''
as $function$
declare
    v_time timestamptz; v_day date; v_week date; v_total numeric;
    v_count integer; v_existing integer; v_eligible boolean; v_credit numeric;
    v_tier integer; v_names text[]:=array['No Badge','Seedling','Sprout','Sapling','Tree','Forest Guardian'];
    r record; v_stats record;
begin
    if new.status::text <> 'completed' or old.status::text is not distinct from 'completed' then return new; end if;
    -- Serialise campus accounting to make per-user daily limits and aggregate
    -- refreshes safe even when different trips complete simultaneously.
    perform pg_catalog.pg_advisory_xact_lock(873214,3);
    if exists(select 1 from public.trip_impact_events where trip_id=new.trip_id) then return new; end if;
    select coalesce(sum(seats_booked),0) into v_count from public.bookings
        where trip_id=new.trip_id and status='completed';
    if v_count < 1 or new.confirmed_passenger_count is distinct from v_count then
        raise exception 'Confirmed passenger count does not match completed bookings';
    end if;
    if new.distance_km is null or not (new.distance_km > 0 and new.distance_km < 'Infinity'::numeric) then
        raise exception 'A finite positive trip distance is required';
    end if;
    v_time:=coalesce(new.end_time,now());
    v_day:=(v_time at time zone 'Asia/Kuala_Lumpur')::date;
    v_week:=date_trunc('week',v_time at time zone 'Asia/Kuala_Lumpur')::date;
    v_total:=case when new.distance_km>=1 then public.estimate_trip_co2(new.distance_km,v_count,new.vehicle_type_snapshot,new.fuel_category_snapshot) else 0 end;
    insert into public.trip_impact_events values
        (new.trip_id,v_time,v_day,v_week,new.distance_km,v_count,v_total,new.vehicle_type_snapshot,new.fuel_category_snapshot);
    for r in
        select u."usersId" as user_id,u.faculty,u.residential_area,u.residential_college,
               participants.weight
        from (
            select new.driver_id as id,1 as weight
            union all
            select passenger_id,sum(seats_booked)::integer from public.bookings
                where trip_id=new.trip_id and status='completed' group by passenger_id
        ) participants join public.users u on u."usersId"=participants.id
        order by u."usersId"
    loop
        select count(*) into v_existing from public.trip_impact_credits
            where user_id=r.user_id and trip_day=v_day and credited;
        v_eligible:=new.distance_km>=1 and v_existing<2;
        -- A multi-seat booking holder receives its companions' occupancy shares.
        -- Reports count registered user IDs, never those companion seats as users.
        v_credit:=case when v_eligible then v_total*r.weight/(v_count+1) else 0 end;
        insert into public.trip_impact_credits values
            (new.trip_id,r.user_id,v_day,v_week,r.weight,v_eligible,new.distance_km,v_credit,
             nullif(btrim(r.faculty),''),nullif(btrim(r.residential_area),''),nullif(btrim(r.residential_college),''));
        select count(*)::integer as trips,coalesce(sum(distance_km),0) as distance,
               coalesce(sum(co2_kg),0) as co2 into v_stats
            from public.trip_impact_credits where user_id=r.user_id and credited;
        select v_stats.trips+coalesce(b.total_trips,0) as trips,
               v_stats.distance+coalesce(b.total_distance_km,0) as distance,
               v_stats.co2+coalesce(b.total_co2_saved_kg,0) as co2 into v_stats
        from (select 1) seed left join public.impact_opening_balances b on b.user_id=r.user_id;
        v_tier:=case
            when v_stats.trips>=80 and v_stats.co2>=120 then 5
            when v_stats.trips>=40 and v_stats.co2>=60 then 4
            when v_stats.trips>=15 and v_stats.co2>=20 then 3
            when v_stats.trips>=5 and v_stats.co2>=5 then 2
            when v_stats.trips>=1 and v_stats.co2>=0.5 then 1 else 0 end;
        insert into public.user_impact_stats(user_id,total_trips,total_distance_km,total_co2_saved_kg,
            current_tier_level,current_tier_name,updated_at)
        values(r.user_id,v_stats.trips,round(v_stats.distance,2),round(v_stats.co2,2),v_tier,v_names[v_tier+1],now())
        on conflict(user_id) do update set total_trips=excluded.total_trips,
            total_distance_km=excluded.total_distance_km,total_co2_saved_kg=excluded.total_co2_saved_kg,
            current_tier_level=greatest(public.user_impact_stats.current_tier_level,excluded.current_tier_level),
            current_tier_name=v_names[greatest(public.user_impact_stats.current_tier_level,excluded.current_tier_level)+1],
            updated_at=now();
    end loop;
    update public.trips set co2_saved_kg=round(v_total,2) where trip_id=new.trip_id;
    delete from public.impact_reporting_cells where week_start=v_week;
    insert into public.impact_reporting_cells
    with tagged as (
        select c.*,'faculty'::text as group_type,faculty as group_name from public.trip_impact_credits c
            where c.week_start=v_week and credited and faculty is not null
        union all
        select c.*,'college'::text,residential_college from public.trip_impact_credits c
            where c.week_start=v_week and credited and residential_college is not null
    ), counts as (
        select group_type,group_name,count(distinct user_id) as users,
               count(distinct trip_id) as trips,sum(co2_kg) as co2
        from tagged group by group_type,group_name
    ), trip_distances as (
        select group_type,group_name,trip_id,max(distance_km) as distance
        from tagged group by group_type,group_name,trip_id
    ), distances as (
        select group_type,group_name,sum(distance) as distance
        from trip_distances group by group_type,group_name
    ) select v_week,c.group_type,c.group_name,c.users,c.trips,d.distance,c.co2
      from counts c join distances d using(group_type,group_name);
    return new;
end;
$function$;
revoke all on function public.record_completed_trip_impact() from public,anon,authenticated;
create trigger record_completed_trip_impact after update of status on public.trips
    for each row execute function public.record_completed_trip_impact();

-- Idempotent network retries return the same completed result, not a second award.
create or replace function public.complete_trip(p_trip_id uuid)
returns integer language plpgsql security definer set search_path = ''
as $function$
declare v_user uuid:=auth.uid(); v_trip public.trips%rowtype; v_passengers integer;
begin
    if v_user is null then raise exception 'Sign in first'; end if;
    select * into v_trip from public.trips where trip_id=p_trip_id for update;
    if not found then raise exception 'Trip not found'; end if;
    if v_trip.driver_id is distinct from v_user then raise exception 'Only the driver can complete this trip'; end if;
    if v_trip.status::text='completed' then return v_trip.confirmed_passenger_count; end if;
    if v_trip.status::text is distinct from 'in_progress' then raise exception 'This trip is not in progress'; end if;
    select coalesce(sum(seats_booked),0) into v_passengers from public.bookings
        where trip_id=p_trip_id and status='completed';
    if v_passengers<1 then raise exception 'At least one passenger must confirm the trip end first'; end if;
    update public.bookings set status='no_show' where trip_id=p_trip_id and status='confirmed';
    update public.trips set status='completed'::public.trip_status,end_time=now(),
        confirmed_passenger_count=v_passengers where trip_id=p_trip_id;
    update public.trip_offers set status='completed'::public.offer_status where trip_offer_id=v_trip.trip_offer_id;
    return v_passengers;
end;
$function$;
revoke all on function public.complete_trip(uuid) from public,anon;
grant execute on function public.complete_trip(uuid) to authenticated;
commit;
