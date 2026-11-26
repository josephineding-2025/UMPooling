begin;
-- All actors and changes in this file roll back. Staging/isolated DB only.
create function pg_temp.finish_trip(p_driver uuid,p_passengers uuid[],p_distance numeric,p_confirm integer default 99)
returns uuid language plpgsql as $$
declare o uuid;t uuid;p uuid;i integer:=0;
begin
    insert into public.trip_offers(driver_id,origin_geo,destination_geo,cost_per_seat,available_seats,departure_time,distance_km)
    values(p_driver,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,2,8,now()+interval '1 hour',p_distance)
    returning trip_offer_id into o;
    foreach p in array p_passengers loop
        perform set_config('request.jwt.claim.sub',p::text,true);
        perform public.book_seat_transaction(o,p,1,101.65,3.12);
    end loop;
    perform set_config('request.jwt.claim.sub',p_driver::text,true);
    t:=public.start_trip(o);
    foreach p in array p_passengers loop
        i:=i+1;
        if i<=p_confirm then
            perform set_config('request.jwt.claim.sub',p::text,true);
            perform public.confirm_trip_end(t);
        end if;
    end loop;
    perform set_config('request.jwt.claim.sub',p_driver::text,true);
    perform public.complete_trip(t);
    return t;
end;
$$;
do $test$
declare
    u uuid[]:=array[]::uuid[];staff uuid:=gen_random_uuid();i integer;t uuid;t2 uuid;
    n integer;v numeric;s numeric;r record;failed boolean:=false;
begin
    for i in 1..11 loop
        u:=array_append(u,gen_random_uuid());
        insert into auth.users(id,email)values(u[i],'sprint2-'||u[i]::text||'@siswa.um.edu.my');
    end loop;
    insert into auth.users(id,email)values(staff,'sprint2-'||staff::text||'@siswa.um.edu.my');
    update public.users set faculty='Five',residential_college='College Five',vehicle_type='car',fuel_category='petrol'
        where "usersId"=any(u[1:5]);
    update public.users set faculty='Four',residential_college='College Four'
        where "usersId"=any(u[6:9]);
    update public.users set "usersRole"='staffAnalyst' where "usersId"=staff;
    if public.estimate_trip_co2(10,2,'car','petrol')<>4 then raise exception 'Petrol contract mismatch';end if;
    if public.estimate_trip_co2(10,2,'car','diesel')<>4.4 then raise exception 'Diesel contract mismatch';end if;
    if public.estimate_trip_co2(10,2,'car','hybrid')<>2.6 then raise exception 'Hybrid contract mismatch';end if;
    if public.estimate_trip_co2(10,2,'car','electric')<>1.4 then raise exception 'EV contract mismatch';end if;
    if public.estimate_trip_co2(10,2,'motorcycle','diesel')<>1.6 then raise exception 'Vehicle precedence mismatch';end if;
    if public.estimate_trip_co2('NaN',2)<>0 then raise exception 'Non-finite estimate accepted';end if;
    t:=pg_temp.finish_trip(u[1],u[2:5],10);
    select co2_saved_kg,confirmed_passenger_count into v,n from public.trips where trip_id=t;
    if v<>8 or n<>4 then raise exception 'Completion data mismatch: %, %',v,n;end if;
    select sum(co2_kg)into s from public.trip_impact_credits where trip_id=t;
    if abs(s-8)>0.0000001 then raise exception 'Participant shares do not conserve trip total';end if;
    perform public.complete_trip(t);
    select count(*)into n from public.trip_impact_events where trip_id=t;
    if n<>1 then raise exception 'Completion retry duplicated event';end if;
    select total_trips,current_tier_level,total_co2_saved_kg into r from public.user_impact_stats where user_id=u[1];
    if r.total_trips<>1 or r.current_tier_level<>1 or r.total_co2_saved_kg<>1.6 then raise exception 'Personal totals/tier mismatch';end if;
    t2:=pg_temp.finish_trip(u[1],array[u[2]],10);
    perform pg_temp.finish_trip(u[1],array[u[2]],10);
    select count(*)into n from public.trip_impact_credits where user_id=u[1] and credited;
    if n<>2 then raise exception 'Daily cap failed';end if;
    select total_trips,total_co2_saved_kg into r from public.user_impact_stats where user_id=u[1];
    if r.total_trips<>2 or r.total_co2_saved_kg<>2.6 then raise exception 'Daily-capped totals mismatch';end if;
    select total_distance_km into v from public.impact_reporting_cells where group_type='faculty' and group_name='Five';
    if v<>20 then raise exception 'Group distance double-counted its passengers: %',v;end if;
    -- Four users should not pass the threshold; many seats are not many users.
    perform pg_temp.finish_trip(u[6],u[7:9],10);
    execute 'set local role authenticated';
    perform set_config('request.jwt.claim.sub',staff::text,true);
    select count(*)into n from public.staff_impact_dashboard where group_name='Five';
    if n<>1 then raise exception 'Staff cannot see five-user faculty';end if;
    select count(*)into n from public.impact_reporting_cells where group_name='Four';
    if n<>0 then raise exception 'Raw-table RLS exposed a four-user group';end if;
    perform set_config('request.jwt.claim.sub',u[1]::text,true);
    select count(*)into n from public.staff_impact_dashboard;
    if n<>0 then raise exception 'Commuter accessed staff dashboard';end if;
    select count(*)into n from public.trip_impact_credits where user_id<>u[1];
    if n<>0 then raise exception 'Personal ledger exposed other users';end if;
    begin
        update public.users set "usersRole"='staffAnalyst' where "usersId"=u[1];
    exception when insufficient_privilege then failed:=true;end;
    if not failed then raise exception 'Commuter self-promoted to staff';end if;
    reset role;
    -- A sub-kilometre completed trip is recorded but receives no award.
    t:=pg_temp.finish_trip(u[10],array[u[11]],0.5);
    select co2_saved_kg into v from public.trips where trip_id=t;
    if v<>0 then raise exception 'Short trip received CO2';end if;
    select count(*)into n from public.trip_impact_credits where trip_id=t and credited;
    if n<>0 then raise exception 'Short trip counted for tier';end if;
    -- No-show passenger has no credit; confirmed count comes from actual confirmations.
    t:=pg_temp.finish_trip(u[10],array[u[11],u[5]],10,1);
    select count(*)into n from public.trip_impact_credits where trip_id=t and user_id=u[5];
    if n<>0 then raise exception 'No-show received credit';end if;
    raise notice 'PASS: accounting, retry, daily cap, tier, no-shows and staff/privacy checks';
end;
$test$;
rollback;
