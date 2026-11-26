-- This actor is seeded only by the isolated Python test runner before migration 10.
begin;
do $$declare d uuid:='00000000-0000-4000-8000-000000000001';p uuid:=gen_random_uuid();o uuid;t uuid;r record;
begin
    insert into auth.users(id,email) values(p,'legacy-test-'||p::text||'@siswa.um.edu.my');
    insert into public.trip_offers(driver_id,origin_geo,destination_geo,cost_per_seat,available_seats,departure_time,distance_km)
    values(d,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,2,1,now()+interval '1 hour',10) returning trip_offer_id into o;
    perform set_config('request.jwt.claim.sub',p::text,true);
    perform public.book_seat_transaction(o,p,1,101.65,3.12);
    perform set_config('request.jwt.claim.sub',d::text,true);t:=public.start_trip(o);
    perform set_config('request.jwt.claim.sub',p::text,true);perform public.confirm_trip_end(t);
    perform set_config('request.jwt.claim.sub',d::text,true);perform public.complete_trip(t);
    select * into r from public.user_impact_stats where user_id=d;
    if r.total_trips<>4 or r.total_distance_km<>20 or r.total_co2_saved_kg<>3 then
        raise exception 'Existing personal totals were lost: %',row_to_json(r);end if;
end$$;
rollback;
