-- Review in staging after migration 7. Booked passengers must retain access
-- to their offer after it becomes full, completed or cancelled.
begin;
drop policy if exists "Read participant offers" on public.trip_offers;
create policy "Read participant offers" on public.trip_offers
for select to authenticated using (
 driver_id = (select auth.uid()) or exists (
  select 1 from public.bookings b
  where b.trip_offer_id = trip_offers.trip_offer_id
    and public.is_booking_participant(b.booking_id)
 )
);
commit;
