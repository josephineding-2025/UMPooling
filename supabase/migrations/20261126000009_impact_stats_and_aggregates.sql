-- Migration: 20261126000009_impact_stats_and_aggregates.sql
-- Module 5 (FR5.3): Cumulative personal carbon tracking, Green Commuter Tier advancement,
-- community aggregations, and staff anonymised query with k >= 5 privacy enforcement.
begin;

-- 1. Extend public.user_impact_stats with Green Commuter Tier columns
alter table public.user_impact_stats
    add column if not exists current_tier_level integer not null default 0 check (current_tier_level between 0 and 5),
    add column if not exists current_tier_name text not null default 'No Badge';

-- 2. Define or ensure public.impact_aggregates table structure
create table if not exists public.impact_aggregates (
    aggregate_id uuid primary key default gen_random_uuid(),
    week_start date not null,
    faculty text,
    residential_area text,
    residential_college text,
    active_user_count integer not null default 0 check (active_user_count >= 0),
    trip_count integer not null default 0 check (trip_count >= 0),
    total_distance_km numeric(10,2) not null default 0.0 check (total_distance_km >= 0),
    total_co2_saved_kg numeric(10,2) not null default 0.0 check (total_co2_saved_kg >= 0),
    created_at timestamptz not null default now()
);

-- Ensure unique constraint for weekly aggregation
do $$
begin
    if not exists (
        select 1 from pg_constraint where conname = 'uq_impact_aggregates_weekly_cohort'
    ) then
        alter table public.impact_aggregates
            add constraint uq_impact_aggregates_weekly_cohort
            unique nulls not distinct (week_start, faculty, residential_area, residential_college);
    end if;
end $$;

-- 3. Row Level Security for impact tables
alter table public.impact_aggregates enable row level security;
revoke all on public.impact_aggregates from public, anon, authenticated;
grant select on public.impact_aggregates to authenticated;

-- Authenticated users can view aggregate totals for leaderboards
drop policy if exists "Authenticated commuters view impact aggregates" on public.impact_aggregates;
create policy "Authenticated commuters view impact aggregates"
    on public.impact_aggregates for select to authenticated
    using (true);

-- 4. Helper function to calculate Green Commuter Tier from trips and CO2
create or replace function public.calculate_green_tier(p_trips integer, p_co2 numeric)
returns table(tier_level integer, tier_name text)
language plpgsql immutable as $$
begin
    if p_trips >= 80 and p_co2 >= 120.0 then
        return query select 5, 'Forest Guardian'::text;
    elsif p_trips >= 40 and p_co2 >= 60.0 then
        return query select 4, 'Tree'::text;
    elsif p_trips >= 15 and p_co2 >= 20.0 then
        return query select 3, 'Sapling'::text;
    elsif p_trips >= 5 and p_co2 >= 5.0 then
        return query select 2, 'Sprout'::text;
    elsif p_trips >= 1 and p_co2 >= 0.5 then
        return query select 1, 'Seedling'::text;
    else
        return query select 0, 'No Badge'::text;
    end if;
end;
$$;

-- 5. RPC Function: record_trip_completion_impact
-- Executed when a trip becomes completed; updates both personal stats and weekly aggregates.
create or replace function public.record_trip_completion_impact(p_trip_id uuid)
returns jsonb
language plpgsql security definer
set search_path = ''
as $$
declare
    v_trip record;
    v_driver_id uuid;
    v_passenger_count integer;
    v_distance_km numeric;
    v_total_trip_co2 numeric;
    v_personal_share_co2 numeric;
    v_week_start date;
    v_driver_profile record;
    v_passenger record;
    v_new_tier record;
    v_processed_count integer := 0;
begin
    -- 1. Fetch completed trip details
    select * into v_trip
    from public.trips
    where trip_id = p_trip_id and status = 'completed';

    if not found then
        return jsonb_build_object('success', false, 'reason', 'Trip not found or not in completed status');
    end if;

    v_driver_id := v_trip.driver_id;
    v_passenger_count := coalesce(v_trip.confirmed_passenger_count, 0);
    v_distance_km := coalesce(v_trip.distance_km, 0);

    -- Anti-gaming rules: distance >= 1.0km, confirmed passengers >= 1
    if v_passenger_count < 1 or v_distance_km < 1.0 then
        return jsonb_build_object('success', false, 'reason', 'Trip does not meet minimum CO2 eligibility thresholds (distance >= 1km, passengers >= 1)');
    end if;

    -- Standard MITI baseline formula: E = 0.20 kg CO2e / km
    v_total_trip_co2 := round(v_passenger_count * v_distance_km * 0.20, 2);
    v_personal_share_co2 := round(v_total_trip_co2 / (v_passenger_count + 1), 2);
    v_week_start := date_trunc('week', coalesce(v_trip.end_time, now()))::date;

    -- Update trip row with total avoided CO2
    update public.trips
    set co2_saved_kg = v_total_trip_co2
    where trip_id = p_trip_id;

    -- Update driver personal stats
    insert into public.user_impact_stats (user_id, total_trips, total_distance_km, total_co2_saved_kg, updated_at)
    values (v_driver_id, 1, v_distance_km, v_personal_share_co2, now())
    on conflict (user_id) do update set
        total_trips = public.user_impact_stats.total_trips + 1,
        total_distance_km = public.user_impact_stats.total_distance_km + v_distance_km,
        total_co2_saved_kg = public.user_impact_stats.total_co2_saved_kg + v_personal_share_co2,
        updated_at = now();

    -- Update driver tier
    select tier_level, tier_name into v_new_tier
    from public.calculate_green_tier(
        (select total_trips from public.user_impact_stats where user_id = v_driver_id),
        (select total_co2_saved_kg from public.user_impact_stats where user_id = v_driver_id)
    );

    update public.user_impact_stats
    set current_tier_level = v_new_tier.tier_level,
        current_tier_name = v_new_tier.tier_name
    where user_id = v_driver_id;

    v_processed_count := v_processed_count + 1;

    -- Update confirmed passengers
    for v_passenger in
        select b.passenger_id
        from public.bookings b
        where b.trip_id = p_trip_id and b.status = 'completed'
    loop
        insert into public.user_impact_stats (user_id, total_trips, total_distance_km, total_co2_saved_kg, updated_at)
        values (v_passenger.passenger_id, 1, v_distance_km, v_personal_share_co2, now())
        on conflict (user_id) do update set
            total_trips = public.user_impact_stats.total_trips + 1,
            total_distance_km = public.user_impact_stats.total_distance_km + v_distance_km,
            total_co2_saved_kg = public.user_impact_stats.total_co2_saved_kg + v_personal_share_co2,
            updated_at = now();

        select tier_level, tier_name into v_new_tier
        from public.calculate_green_tier(
            (select total_trips from public.user_impact_stats where user_id = v_passenger.passenger_id),
            (select total_co2_saved_kg from public.user_impact_stats where user_id = v_passenger.passenger_id)
        );

        update public.user_impact_stats
        set current_tier_level = v_new_tier.tier_level,
            current_tier_name = v_new_tier.tier_name
        where user_id = v_passenger.passenger_id;

        v_processed_count := v_processed_count + 1;
    end loop;

    -- Update impact_aggregates by driver's cohort
    select faculty, residential_area, residential_college into v_driver_profile
    from public.users
    where "usersId" = v_driver_id;

    insert into public.impact_aggregates (
        week_start, faculty, residential_area, residential_college,
        active_user_count, trip_count, total_distance_km, total_co2_saved_kg
    ) values (
        v_week_start, v_driver_profile.faculty, v_driver_profile.residential_area, v_driver_profile.residential_college,
        v_processed_count, 1, v_distance_km, v_total_trip_co2
    )
    on conflict on constraint uq_impact_aggregates_weekly_cohort do update set
        trip_count = public.impact_aggregates.trip_count + 1,
        total_distance_km = public.impact_aggregates.total_distance_km + v_distance_km,
        total_co2_saved_kg = public.impact_aggregates.total_co2_saved_kg + v_total_trip_co2;

    return jsonb_build_object(
        'success', true,
        'trip_id', p_trip_id,
        'total_trip_co2_kg', v_total_trip_co2,
        'personal_share_co2_kg', v_personal_share_co2,
        'occupants_credited', v_processed_count
    );
end;
$$;

-- 6. RPC Function: get_staff_anonymised_aggregates
-- Masks totals as NULL for small commuter groups with active_user_count < 5 (k-anonymity mandate).
create or replace function public.get_staff_anonymised_aggregates()
returns table(
    aggregate_id uuid,
    week_start date,
    faculty text,
    residential_area text,
    residential_college text,
    active_user_count integer,
    trip_count integer,
    total_distance_km numeric,
    total_co2_saved_kg numeric,
    is_masked boolean
)
language sql security definer
set search_path = ''
as $$
    select
        a.aggregate_id,
        a.week_start,
        a.faculty,
        a.residential_area,
        a.residential_college,
        a.active_user_count,
        a.trip_count,
        case when a.active_user_count >= 5 then a.total_distance_km else null end as total_distance_km,
        case when a.active_user_count >= 5 then a.total_co2_saved_kg else null end as total_co2_saved_kg,
        case when a.active_user_count < 5 then true else false end as is_masked
    from public.impact_aggregates a
    order by a.week_start desc, a.total_co2_saved_kg desc nulls last;
$$;

commit;
