-- ISOLATED TEST DATABASE ONLY. This is an approximation of the exported baseline,
-- not a production migration or an exact Supabase Auth/network implementation.
create extension if not exists postgis;
create extension if not exists "uuid-ossp";
create role anon nologin;
create role authenticated nologin;
create role service_role nologin bypassrls;
create schema auth;
create table auth.users(id uuid primary key,email text);
create function auth.uid() returns uuid language sql stable as $$
    select nullif(current_setting('request.jwt.claim.sub',true),'')::uuid;
$$;
create function auth.role() returns text language sql stable as $$
    select coalesce(nullif(current_setting('request.jwt.claim.role',true),''),current_user::text);
$$;
create schema vault;
create table vault.decrypted_secrets(name text,decrypted_secret text);
create schema net;
create function net.http_post(url text,headers jsonb,body jsonb,timeout_milliseconds integer)
returns bigint language sql as $$select 1::bigint$$;
grant usage on schema public,auth to anon,authenticated,service_role;
create type public.user_role as enum('passengers','drivers','both','staffAnalyst');
create type public.offer_status as enum('open','full','cancelled','completed');
create type public.trip_status as enum('scheduled','in_progress','completed','cancelled');
create type public.request_status as enum('PENDING','SUCCESS','ERROR');
create type public.sos_status as enum('active','resolved','false_alarm');
create table public.users(
    "usersId" uuid primary key references auth.users(id),"usersEmail" text not null unique,
    "usersRole" public.user_role not null default 'passengers',
    "vehiclePlate" text,"vehicleModel" text,"createdTime" timestamptz default now()
);
create table public.saved_routes(
    route_id uuid primary key default gen_random_uuid(),user_id uuid not null references public.users("usersId"),
    origin_geo geography(Point,4326) not null,destination_geo geography(Point,4326) not null,
    distance_km numeric default 0,created_at timestamptz default now()
);
create table public.trip_offers(
    trip_offer_id uuid primary key default gen_random_uuid(),driver_id uuid not null references public.users("usersId"),
    origin_geo geography(Point,4326) not null,destination_geo geography(Point,4326) not null,
    cost_per_seat numeric(5,2) not null constraint "trip_offers_costPerSeat_check" check(cost_per_seat<=10),
    available_seats integer not null check(available_seats>=0),status public.offer_status default 'open',
    departure_time timestamptz not null,created_at timestamptz default now()
);
create table public.ride_requests(
    request_id uuid primary key default gen_random_uuid(),user_id uuid not null references public.users("usersId"),
    trip_offer_id uuid references public.trip_offers(trip_offer_id),pickup_geo geography(Point,4326) not null,
    status public.request_status default 'PENDING',requested_at timestamptz default now()
);
create table public.trips(
    trip_id uuid primary key default gen_random_uuid(),trip_offer_id uuid not null references public.trip_offers(trip_offer_id),
    driver_id uuid not null references public.users("usersId"),status public.trip_status default 'scheduled',
    start_time timestamptz,end_time timestamptz,"createdTime" timestamptz default now()
);
create table public.bookings(
    booking_id uuid primary key default gen_random_uuid(),trip_offer_id uuid references public.trip_offers(trip_offer_id),
    trip_id uuid references public.trips(trip_id),passenger_id uuid references public.users("usersId"),
    seats_booked integer default 1 check(seats_booked>0),pickup_geo geography(Point,4326),
    status text default 'confirmed',created_at timestamptz default now(),unique(trip_offer_id,passenger_id)
);
create table public.messages(
    message_id uuid primary key default gen_random_uuid(),sender_id uuid not null references public.users("usersId"),
    content text not null,sent_at timestamptz default now(),trip_id uuid references public.trips(trip_id),
    booking_id uuid references public.bookings(booking_id),check(trip_id is not null or booking_id is not null)
);
create table public.ratings(
    rating_id uuid primary key default gen_random_uuid(),trip_id uuid not null references public.trips(trip_id),
    rate integer not null check(rate between 1 and 5),comment text,
    reviewer_id uuid not null references public.users("usersId"),reviewee_id uuid not null references public.users("usersId"),
    "createdTime" timestamptz default now(),unique(trip_id,reviewer_id,reviewee_id)
);
create table public.sos_alerts(
    alert_id uuid primary key default gen_random_uuid(),trip_id uuid not null references public.trips(trip_id),
    user_id uuid not null references public.users("usersId"),location_geo geography(Point,4326) not null,
    status public.sos_status default 'active',alert_time timestamptz default now()
);
create table public.impact_aggregates(
    aggregate_id uuid primary key default gen_random_uuid(),week_start date not null,faculty text,residential_area text,
    active_user_count integer not null default 0,trip_count integer not null default 0,
    total_distance_km numeric default 0,total_co2_saved_kg numeric default 0,
    unique(week_start,faculty,residential_area)
);
create view public.anonymised_impact_dashboard as select * from public.impact_aggregates where active_user_count>=5;
do $$declare t text;begin
    foreach t in array array['users','trip_offers','trips','bookings','messages','ride_requests','saved_routes','ratings','sos_alerts','impact_aggregates']
    loop execute format('alter table public.%I enable row level security',t);end loop;
end$$;
grant all on all tables in schema public to anon,authenticated,service_role;
alter default privileges in schema public grant all on tables to anon,authenticated,service_role;
create policy "View open trip offers" on public.trip_offers for select using(auth.role()='authenticated');
create policy "Drivers manage own offers" on public.trip_offers for all using(auth.uid()=driver_id);
create policy "Passengers manage own requests" on public.ride_requests for all using(auth.uid()=user_id);
create policy "Drivers view requests for their offers" on public.ride_requests for select using(exists(
    select 1 from public.trip_offers o where o.trip_offer_id=ride_requests.trip_offer_id and o.driver_id=auth.uid()));
create policy "Users manage own routes" on public.saved_routes for all using(auth.uid()=user_id);
