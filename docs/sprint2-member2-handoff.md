# Member 2 Sprint 2 implementation and deployment handoff

Based on main `524eb3f` and Member 5's notification branch `afd3704`.
Changes are prepared for review; a Git push does not deploy SQL or Edge Functions.

## What is implemented

| Requirement | Implementation |
|---|---|
| FR3.1 | Configurable cap from `max_cost_per_seat()`/app_config, fail-closed validation, no hard-coded RM10 ceiling overriding configuration; live offer form loads that cap. No payment SDK, charge or checkout is added. |
| FR3.2 reservation | Offer-row lock, booking insert and seat decrement in one RPC transaction. Same-user/same-seat retries return the existing ticket instead of deducting seats again. The live flow supplies a quote version; changed route/price/departure details require refresh before a new reservation. |
| Ticket | Booking reference, price, departure and route-name snapshots taken at reservation. Live confirmation opens only the booking ID returned by the server. Existing tickets without a snapshot display unavailable price instead of invented demo values. |
| Cancellation | A passenger can cancel their own reservation before the trip starts; offer-first row locking restores seats once. Active whole-trip cancellation retains migration 7's terminal cancellation behaviour. |
| Completion | Existing start/passenger-confirm/driver-complete flow records distance/count, calls the server estimate contract, writes an event/participant ledger and updates personal totals once. Completed-call retries return the prior count. |
| CO2 integration | Server `estimate_trip_co2` matches Member 4's factors/formula and tier thresholds in `Co2CalculationService.kt`. Kotlin is the preview implementation; stored awards do not accept client CO2 totals. |
| Matching | Member 5's request trigger + worker integration, with installed PostGIS schema discovery, origin/destination proximity, requested-seat checks and MYT time matching including midnight. Saved routes do not invent available seats. |
| Push | Authenticated source-ID webhooks re-read source rows, participant-based routing, private message previews, per-recipient leases/deduplication, typed stale-token detection, 503 on failure and bounded retry mode. |
| Staff reports | `staff_impact_dashboard` uses an invoker view and underlying RLS. Only server-assigned staffAnalyst users can read cells with >=5 distinct registered contributors. Direct reads of the backing table have the same restriction. |

## Manual steps required from the project owner after review

### 1. Development/staging database first

Use a staging copy with the existing base tables, PostGIS, enum types and migrations
1–8. The repository cannot initialise an empty Supabase project from migrations
alone; the local test fixture is NOT a production baseline.

Run `supabase/checks/02_sprint2_preflight.sql` for read-only presence checks, then
check actual migration history and function definitions before applying anything.
Presence checks do not prove the correct function version is installed.
If Member 5 already applied a different migration 9, reconcile it with them and
create a follow-up version rather than overwriting applied migration history.
Do not replay migrations 1–8 or older SQL Editor snippets.

Apply these new migration files once, in order:

1. `20261125000009_ride_request_push_trigger.sql`
2. `20261126000010_completion_impact_reporting.sql`
3. `20261126000011_push_delivery_dedup.sql`
4. `20261126000012_booking_ticket_contract.sql`

Each is a transaction. On error stop, ROLLBACK if needed, and inspect the error.
Already committed earlier files will not be rolled back with a later file.

### 2. Deploy the notification function

Deploy the entire `supabase/functions/swift-worker/` directory, including
`notification_logic.ts`. Keep Verify JWT OFF and validate the x-webhook-secret.
The Vault entry `webhook_secret` must match the function secret `WEBHOOK_SECRET`.
Set Firebase server secrets (`FIREBASE_PROJECT_ID`, `FIREBASE_CLIENT_EMAIL`,
`FIREBASE_PRIVATE_KEY`) through the private dashboard/CLI secrets mechanism.
Never paste those values into GitHub, SQL snippets or chat.

Retain the migration-6 booking/message/SOS triggers. Migration 9 adds the request
trigger. Service-role credentials stay on the server. No email/Resend settings
are changed by this work.

For automatic failed-delivery retries, enable pg_cron in staging and apply
`supabase/ops/schedule_push_retry.sql`. Its named once-a-minute job reads the
webhook secret from Vault at runtime; the secret is not embedded in cron SQL.
A retry scans a bounded batch and stops automatically after five attempts. Monitor
`push_deliveries` for failures/exhausted attempts; transport delivery is not
exactly-once. A stable Android tag reduces duplicate notification cards if an
FCM success happens just before a response/acknowledgement is lost. Stale or
expired requests are discarded, not repeatedly sent to newly matching users.

### 3. Android configuration and device test

Build the branch with JDK21/SDK36.1. Keep your existing local.properties and
app/google-services.json private. Install the matching new app only after the
backend RPCs are installed: FCM registration now uses `register_fcm_token` and
logout uses `unregister_fcm_token`. Direct fcm_token column UPDATE is revoked.
The old permission check expecting direct token writes to be true is obsolete.

Use two verified UM test accounts on separate devices. Confirm:

- Sign in, post a real offer and check its configurable cap and persisted values.
- Find the offer, pick a verified pickup point and reserve; check the ticket ID,
  price/departure/route snapshots and passenger/driver access.
- Retry a reservation after a simulated response loss: same booking, no extra seat.
- Cancel before start twice: restore seats once. Rebooking the same cancelled
  passenger/offer pair remains unsupported and returns an explicit error.
- Driver starts, passenger confirms end, driver completes. Inspect stored distance,
  CO2 and totals. Repeat completion: no duplicate credit.
- Trigger booking/message/request notifications, open their participant-checked
  destinations, and test foreground/background/process-closed delivery. Force
  stop is a different Android case and does not guarantee delivery.
- Switch accounts on one device: registration moves the token to the current
  account; opening an old notification must still fail RLS where inappropriate.
- Verify staff reports with four vs five distinct test users. Commuters and
  anonymous clients must not see any staff rows or another user's personal ledger.
- Measure real notification latency and concurrent-user performance. Local tests
  do not establish the 5-second notification or 100-user NFRs.

Only promote to the shared project after staging evidence and teammate review.

## Member 4 and Member 5 integration contracts

- Member 4: `estimate_trip_co2(distance,passengers,vehicle,fuel)` implements the
  existing factor precedence/formula. The completion trigger credits trips >=1km
  with at least one confirmed passenger, with max two counted trips per registered
  user per MYT calendar day. It snapshots profile group membership at completion
  and vehicle/fuel at start. A trip's reported model total and eligible personal
  awards are different: daily-capped participants receive no additional award.
- For multi-seat bookings the registered holder receives the occupancy shares
  for their companions. This conserves the model total without inventing companion
  user IDs; confirm this extension with Member 4 before merging. The privacy
  threshold always counts DISTINCT registered IDs, never seats.
- Stored distance is the posted offer's distance, or a PostGIS geodesic distance
  times 1.3 fallback when absent. `distance_source` records that provenance.
  This is not a GPS-measured actual-distance pipeline; do not label it measured.
- `ImpactRepository` provides typed personal totals and staff cells for Member 4's
  dashboard. Existing demo dashboard charts/tier screens are not replaced by this
  backend PR and must be wired to these persisted records by the dashboard owner.
- Member 5: matching now calls `find_overlapping_drivers_for_request(request_id)`.
  It is service-only and reads the actual request, instead of accepting forged
  request geometry. Requests without destination/time, expired or non-PENDING
  requests do not notify. Windows are 500m origin, 300m destination and +/-15min.
- Saved-route-only matches return NULL seat availability. A request notification
  describes REQUESTED seats and a vehicle-specific estimated CO2 value. It does
  not promise that a saved-route driver has posted an offer or reserved seats.
- `request_id` payloads are accepted by the Android inbox. Opening checks that
  the driver is still eligible through RLS, then opens the offer form. Detailed
  request-to-offer negotiation remains Member 5's integration workflow.

## Data preservation and reporting

Existing personal totals are snapshotted into private `impact_opening_balances`.
They are added to new ledger-derived totals, not overwritten or invented as
historical weekly events. Existing tier levels never decrease. Old aggregate
rows remain private and unchanged; the new staff view uses only new trusted
completion credits. An audited historical backfill is separate work.

Faculty and college are separate dimensions. Group trip counts/distance count a
physical trip once within a group, while CO2 sums that group's participant shares.
Do not add faculty and college rows together as a campus total: they overlap.
Weekly buckets use MYT Monday. K>=5 suppresses small cells but is not a blanket
proof of anonymity under all combinations of outside information.

The event/credit ledger retains foreign-key links for audit integrity. Administrative
account/trip deletion and retention/erasure procedures need a coordinated policy;
do not delete referenced completed records ad hoc. No raw ledger access is granted
to staff. Personal totals remain owner-readable and client read-only.

## Tests

The isolated test fixture approximates the supplied baseline and uses stubbed
Auth/Vault/pg_net; it never sends emails, pushes or HTTP calls. It is not proof
of live Supabase deployment. `supabase/tests/run_local.py` drives all migrations,
SQL business/RLS tests and independent-session concurrency checks against a
network-disabled PostGIS16 container named `umpooling-sprint2-test`.

The existing rollback lifecycle test and new accounting, booking/matching,
legacy-balance, snapshot, final-seat, duplicate/cancellation and concurrent daily
cap tests passed. Edge helper tests, Deno type/lint checks and the actual Android
build/unit-test result are recorded in evidence/sprint2-member2-validation.md.
