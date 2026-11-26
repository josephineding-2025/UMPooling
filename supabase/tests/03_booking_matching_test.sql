begin;
do $test$
declare d uuid:=gen_random_uuid();p uuid:=gen_random_uuid();x uuid:=gen_random_uuid();
    o uuid;b uuid;b2 uuid;r uuid;n integer;v numeric;failed boolean:=false;
begin
    insert into auth.users(id,email)values
        (d,'sprint2-'||d::text||'@siswa.um.edu.my'),
        (p,'sprint2-'||p::text||'@siswa.um.edu.my'),
        (x,'sprint2-'||x::text||'@siswa.um.edu.my');
    update public.users set "usersRole"='drivers' where "usersId"=d;
    update public.app_config set value=12 where key='max_cost_per_seat';
    insert into public.trip_offers(driver_id,origin_geo,destination_geo,cost_per_seat,available_seats,departure_time,distance_km,origin_zone)
        values(d,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,11,2,now()+interval '1 hour',10,'original')
        returning trip_offer_id into o;
    -- The old fixed RM10 check must not override a configured RM12 cap.
    begin update public.trip_offers set cost_per_seat=13 where trip_offer_id=o;
    exception when others then failed:=true;end;
    if not failed then raise exception 'Contribution cap bypassed';end if;
    perform set_config('request.jwt.claim.sub',p::text,true);
    b:=public.book_seat_transaction(o,p,1,101.65,3.12);
    b2:=public.book_seat_transaction(o,p,1,101.65,3.12);
    if b<>b2 then raise exception 'Retry did not return original ticket';end if;
    select available_seats into n from public.trip_offers where trip_offer_id=o;
    if n<>1 then raise exception 'Retry deducted seats twice';end if;
    update public.trip_offers set cost_per_seat=12,origin_zone='edited' where trip_offer_id=o;
    select cost_per_seat_snapshot into v from public.bookings where booking_id=b;
    if v<>11 then raise exception 'Ticket price changed with offer';end if;
    if not exists(select 1 from public.bookings where booking_id=b and booking_reference like 'UMP-%' and origin_zone_snapshot='original') then
        raise exception 'Ticket snapshot missing';end if;
    if public.cancel_booking(b)<>1 or public.cancel_booking(b)<>0 then raise exception 'Cancellation not idempotent';end if;
    select available_seats into n from public.trip_offers where trip_offer_id=o;
    if n<>2 then raise exception 'Cancelled seat was not restored exactly once';end if;
    failed:=false;
    perform set_config('request.jwt.claim.sub',x::text,true);
    begin perform public.book_seat_with_quote(o,1,101.65,3.12,0);
    exception when others then
        if sqlerrm <> 'Offer details changed; refresh before booking' then raise;end if;
        failed:=true;
    end;
    if not failed then raise exception 'Stale quote was accepted';end if;
    select quote_version into n from public.trip_offers where trip_offer_id=o;
    b:=public.book_seat_with_quote(o,1,101.65,3.12,n);
    update public.trip_offers set cost_per_seat=11 where trip_offer_id=o;
    if public.book_seat_with_quote(o,1,101.65,3.12,n)<>b then raise exception 'Quoted retry changed the existing ticket';end if;
    -- Nullable legacy states must fail closed, not pass SQL three-valued logic.
    update public.bookings set status=null where booking_id=b;
    failed:=false;
    begin perform public.cancel_booking(b);
    exception when others then
        if sqlerrm <> 'Only a booking before trip start can be cancelled' then raise;end if;
        failed:=true;
    end;
    if not failed then raise exception 'Unknown legacy booking state restored inventory';end if;
    update public.bookings set status='confirmed' where booking_id=b;
    -- Saved-route matching across midnight uses MYT clock time and no fabricated seats.
    insert into public.saved_routes(user_id,origin_geo,destination_geo,preferred_time)
        values(d,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,'00:05');
    insert into public.ride_requests(user_id,pickup_geo,destination_geo,preferred_time)
        values(x,st_setsrid(st_makepoint(101.65,3.12),4326)::geography,
        st_setsrid(st_makepoint(101.66,3.13),4326)::geography,
        (((now() at time zone 'Asia/Kuala_Lumpur')::date+1)+time '23:55') at time zone 'Asia/Kuala_Lumpur')
        returning request_id into r;
    select count(*)into n from public.find_overlapping_drivers_for_request(r) where driver_id=d and available_seats is null;
    if n<>1 then raise exception 'Midnight saved route should match without invented seats';end if;
    update public.saved_routes set preferred_time='12:00' where user_id=d;
    select count(*)into n from public.find_overlapping_drivers_for_request(r);
    if n<>0 then raise exception 'Out-of-window saved route matched';end if;
    -- A shared-device token is moved to the currently authenticated account.
    perform set_config('request.jwt.claim.sub',p::text,true);
    perform public.register_fcm_token('synthetic-token');
    perform set_config('request.jwt.claim.sub',x::text,true);
    perform public.register_fcm_token('synthetic-token');
    select count(*)into n from public.users where fcm_token='synthetic-token';
    if n<>1 or not exists(select 1 from public.users where "usersId"=x and fcm_token='synthetic-token') then
        raise exception 'Token remained linked to previous account';end if;
    perform public.unregister_fcm_token();
    if exists(select 1 from public.users where "usersId"=x and fcm_token is not null) then
        raise exception 'Logout did not unlink token';end if;
    -- Service-only claims prevent duplicate sends and allow a failed event retry.
    if not public.claim_push_delivery('test-event',p) then raise exception 'Initial delivery not claimed';end if;
    if public.claim_push_delivery('test-event',p) then raise exception 'Concurrent delivery claim accepted';end if;
    update public.push_deliveries set status='failed' where event_key='test-event';
    if not public.claim_push_delivery('test-event',p) then raise exception 'Failed delivery could not retry';end if;
    update public.push_deliveries set status='sent' where event_key='test-event';
    if public.claim_push_delivery('test-event',p) then raise exception 'Sent delivery was resent';end if;
    execute 'set local role authenticated';
    begin perform public.claim_push_delivery('forged',p);raise exception 'Client could claim delivery';
    exception when insufficient_privilege then null;end;
    reset role;
    raise notice 'PASS: configurable cap, ticket, reservation/cancellation retries, matching and delivery claims';
end;
$test$;
rollback;
