-- Integrates Member 5's FR2.2 branch (afd3704). Adapted for the installed
-- PostGIS schema, saved-route time matching, and truthful unknown seat counts.
begin;
alter table public.ride_requests add column if not exists requested_seats integer not null default 1
    check (requested_seats between 1 and 8);
create or replace function public.find_overlapping_drivers_for_request(p_request_id uuid)
returns table(driver_id uuid,available_seats integer,departure_time timestamptz,
    distance_km numeric,vehicle_type text,fuel_category text)
language plpgsql stable security definer set search_path = ''
as $function$
declare v_request public.ride_requests%rowtype; v_schema text;
begin
    select * into v_request from public.ride_requests where request_id=p_request_id;
    if not found or upper(v_request.status::text) is distinct from 'PENDING'
       or v_request.destination_geo is null or v_request.preferred_time is null
       or v_request.preferred_time < now() then return; end if;
    select n.nspname into strict v_schema from pg_catalog.pg_extension e
        join pg_catalog.pg_namespace n on n.oid=e.extnamespace where e.extname='postgis';
    return query execute format($query$
        with candidates as (
            select o.driver_id,o.available_seats,o.departure_time,
                coalesce(o.distance_km,(%1$I.st_distance($1,$2)/1000.0*1.3)::numeric) as distance_km,
                u.vehicle_type,u.fuel_category,0 as preference
            from public.trip_offers o join public.users u on u."usersId"=o.driver_id
            where o.status::text='open' and o.available_seats >= $4
              and u."usersRole"::text in ('drivers','both') and o.driver_id<>$5
              and o.departure_time>=now()
              and %1$I.st_dwithin(o.origin_geo,$1,500)
              and %1$I.st_dwithin(o.destination_geo,$2,300)
              and abs(extract(epoch from (o.departure_time-$3)))<=900
            union all
            select sr.user_id,null::integer,$3,
                (%1$I.st_distance($1,$2)/1000.0*1.3)::numeric,u.vehicle_type,u.fuel_category,1
            from public.saved_routes sr join public.users u on u."usersId"=sr.user_id
            where u."usersRole"::text in ('drivers','both') and sr.user_id<>$5
              and sr.preferred_time is not null
              and %1$I.st_dwithin(sr.origin_geo,$1,500)
              and %1$I.st_dwithin(sr.destination_geo,$2,300)
              and least(
                abs(extract(epoch from (sr.preferred_time-($3 at time zone 'Asia/Kuala_Lumpur')::time))),
                86400-abs(extract(epoch from (sr.preferred_time-($3 at time zone 'Asia/Kuala_Lumpur')::time)))
              )<=900
        ) select distinct on(c.driver_id) c.driver_id,c.available_seats,c.departure_time,
            c.distance_km,c.vehicle_type,c.fuel_category
          from candidates c order by c.driver_id,c.preference,c.departure_time
    $query$,v_schema)
    using v_request.pickup_geo,v_request.destination_geo,v_request.preferred_time,
          v_request.requested_seats,v_request.user_id;
end;
$function$;
revoke all on function public.find_overlapping_drivers_for_request(uuid) from public,anon,authenticated;
grant execute on function public.find_overlapping_drivers_for_request(uuid) to service_role;
drop trigger if exists notify_ride_request on public.ride_requests;
create trigger notify_ride_request after insert on public.ride_requests
    for each row execute function public.notify_push();
-- Matched drivers may inspect only the requests they were eligible to receive.
create or replace function public.is_matched_request_driver(p_request_id uuid)
returns boolean language sql stable security definer set search_path=''
as $function$
    select auth.uid() is not null and exists (
        select 1 from public.find_overlapping_drivers_for_request(p_request_id) m
        where m.driver_id=auth.uid()
    );
$function$;
revoke all on function public.is_matched_request_driver(uuid) from public,anon;
grant execute on function public.is_matched_request_driver(uuid) to authenticated;
create policy "Matched drivers read requests" on public.ride_requests for select to authenticated
    using(public.is_matched_request_driver(request_id));
commit;
