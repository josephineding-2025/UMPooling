-- Read-only presence/grant checks, not proof of migration history or runtime correctness.
-- Do not execute setup scripts just because a presence check passes/fails.
with checks(name,ready) as (values
    ('Baseline start_trip exists',to_regprocedure('public.start_trip(uuid)') is not null),
    ('Baseline confirm_trip_end exists',to_regprocedure('public.confirm_trip_end(uuid)') is not null),
    ('Baseline complete_trip exists',to_regprocedure('public.complete_trip(uuid)') is not null),
    ('Baseline webhook function exists',to_regprocedure('public.notify_push()') is not null),
    ('Matching installed',to_regprocedure('public.find_overlapping_drivers_for_request(uuid)') is not null),
    ('Completion ledger installed',to_regclass('public.trip_impact_events') is not null),
    ('Staff view installed',to_regclass('public.staff_impact_dashboard') is not null),
    ('Token registration installed',to_regprocedure('public.register_fcm_token(text)') is not null),
    ('Quoted booking installed',to_regprocedure('public.book_seat_with_quote(uuid,integer,double precision,double precision,bigint)') is not null),
    ('Cancellation installed',to_regprocedure('public.cancel_booking(uuid)') is not null),
    ('PostGIS installed',exists(select 1 from pg_catalog.pg_extension where extname='postgis'))
)
select name,case when ready then 'PRESENT' else 'MISSING' end as state from checks order by name;
