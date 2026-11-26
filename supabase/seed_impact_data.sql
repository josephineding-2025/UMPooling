-- Seed Data Script for Module 5 (FR5.3) Evaluation and Demo
-- Populates public.impact_aggregates with realistic UM weekly cohorts
-- containing both k >= 5 (unmasked) and k < 5 (masked) groups.
begin;

insert into public.impact_aggregates (
    week_start, faculty, residential_area, residential_college,
    active_user_count, trip_count, total_distance_km, total_co2_saved_kg
) values
    ('2026-11-23', 'Computer Science (FSKTM)', 'Tiara Damansara', null, 24, 96, 960.00, 192.00),
    ('2026-11-23', 'Faculty of Engineering', 'Vista Angkasa', null, 18, 72, 648.00, 129.60),
    ('2026-11-23', 'Faculty of Science', 'Pacific Star', null, 14, 52, 468.00, 93.60),
    ('2026-11-23', 'Faculty of Business & Economics', 'Pantai Hillpark', null, 10, 38, 304.00, 60.80),
    ('2026-11-23', null, null, 'Kolej Kediaman 12 (KK12)', 15, 60, 360.00, 72.00),
    ('2026-11-23', null, null, 'Kolej Kediaman 10 (KK10)', 11, 42, 252.00, 50.40),
    ('2026-11-23', 'Faculty of Pharmacy', 'Seksyen 17', null, 3, 8, 88.00, 17.60),
    ('2026-11-23', 'Faculty of Law', 'Pacific Star', null, 2, 5, 55.00, 11.00)
on conflict do nothing;

commit;
