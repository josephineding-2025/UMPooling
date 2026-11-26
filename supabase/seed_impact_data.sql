-- ============================================================================
-- SYNTHETIC DEMO / EVALUATION DATA ONLY - DO NOT RUN ON LIVE / PRODUCTION DB
-- ============================================================================
-- This script contains synthetic cohort data for local test fixtures, offline
-- benchmarking, and UI evaluation.
-- DO NOT execute this script against the shared production Supabase instance.
-- Real production impact data is derived dynamically on trip completion
-- via public.record_completed_trip_impact() (Migration 10).
-- ============================================================================
begin;

insert into public.impact_reporting_cells (
    week_start, group_type, group_name,
    active_user_count, trip_count, total_distance_km, total_co2_saved_kg
) values
    ('2026-11-23', 'faculty', 'Computer Science (FSKTM)', 24, 96, 960.00, 192.00),
    ('2026-11-23', 'faculty', 'Faculty of Engineering', 18, 72, 648.00, 129.60),
    ('2026-11-23', 'faculty', 'Faculty of Science', 14, 52, 468.00, 93.60),
    ('2026-11-23', 'faculty', 'Faculty of Business & Economics', 10, 38, 304.00, 60.80),
    ('2026-11-23', 'college', 'Kolej Kediaman 12 (KK12)', 15, 60, 360.00, 72.00),
    ('2026-11-23', 'college', 'Kolej Kediaman 10 (KK10)', 11, 42, 252.00, 50.40),
    ('2026-11-23', 'faculty', 'Faculty of Pharmacy', 3, 8, 88.00, 17.60),
    ('2026-11-23', 'faculty', 'Faculty of Law', 2, 5, 55.00, 11.00)
on conflict do nothing;

commit;
