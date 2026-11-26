begin;
do $$declare d uuid:=gen_random_uuid();p uuid:=gen_random_uuid();o uuid;t uuid;b uuid;v numeric;distance numeric;credit numeric;companion_credit numeric;n integer;
begin
    insert into auth.users(id,email)values(d,'snapshot-'||d::text||'@siswa.um.edu.my'),(p,'snapshot-'||p::text||'@siswa.um.edu.my');
    update public.users set vehicle_type='car',fuel_category='petrol',faculty='Companions',residential_college='Companions' where "usersId" in(d,p);
    insert into public.trip_offers(driver_id,origin_geo,destination_geo,cost_per_seat,available_seats,departure_time)
        values(d,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,2,3,now()+interval '1 hour') returning trip_offer_id into o;
    perform set_config('request.jwt.claim.sub',p::text,true);b:=public.book_seat_transaction(o,p,3,101.65,3.12);
    perform set_config('request.jwt.claim.sub',d::text,true);t:=public.start_trip(o);
    update public.users set fuel_category='electric' where "usersId"=d;
    update public.trip_offers set distance_km=99,cost_per_seat=3 where trip_offer_id=o;
    perform set_config('request.jwt.claim.sub',p::text,true);perform public.confirm_trip_end(t);
    perform set_config('request.jwt.claim.sub',d::text,true);perform public.complete_trip(t);
    select distance_km,co2_saved_kg into distance,v from public.trips where trip_id=t;
    if distance not between 1 and 5 or v<>round(distance*3*0.20,2) then
        raise exception 'Route/vehicle snapshot was changed: %, %',distance,v;end if;
    select co2_kg into credit from public.trip_impact_credits where trip_id=t and user_id=d;
    select co2_kg into companion_credit from public.trip_impact_credits where trip_id=t and user_id=p;
    if abs(companion_credit-credit*3)>0.0000001 or abs(companion_credit+credit-distance*3*0.20)>0.0000001 then
        raise exception 'Multi-seat occupancy shares inflated total';end if;
    select active_user_count into n from public.impact_reporting_cells where group_name='Companions' and group_type='faculty';
    if n<>2 then raise exception 'Seats were counted as distinct registered users';end if;
    if not exists(select 1 from public.bookings where booking_id=b and cost_per_seat_snapshot=2) then
        raise exception 'Original ticket price lost';end if;
end$$;
rollback;
