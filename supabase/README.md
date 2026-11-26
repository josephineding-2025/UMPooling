# Supabase: database, functions and migrations

## Layout

| Path | What it is |
|---|---|
| `migrations/` | SQL changes in order, named `<timestamp>_<name>.sql`. Add new changes here. |
| `functions/swift-worker/` | The push notification Edge Function (dashboard name: send-push-notification). |
| `checks/` | Read-only inspection queries and explicitly labelled rollback-based staging tests. |

## Sprint 2 additions (not deployed)

The new migrations 9–12, matching/notification source, booking integration and
staff reporting are described in [Member 2 handoff](../docs/sprint2-member2-handoff.md).
Use that handoff for staging order and tests; do not replay migrations 1–8.
The previous status below describes the earlier baseline, not proof of current
live migration state.

## Status

- Migrations 1 to 6 are already applied to the live project. Migration 7 (trip lifecycle) is written but not applied; test it with `checks/01_trip_lifecycle_test.sql` (1 to 4 were applied from Member 2's
  earlier copies, so the live version of migration 4 may differ from the file here: it still
  grants `usersRole` updates; migration 5 removes that grant).
- These files are written for the **existing** project. The base tables, enums and PostGIS were
  created in the dashboard, so the migrations alone **cannot build an empty project**. Before
  anyone relies on `supabase db reset`, create a baseline: `supabase db dump --schema public`
  into a first migration, then mark migrations 1 to 6 as already applied with
  `supabase migration repair --status applied <version>`.
- Never save secrets in these files. The webhook secret lives in Supabase Vault and the
  `WEBHOOK_SECRET` function secret.

## PR #5 lifecycle fix and staging verification

Migration 7 revokes direct client UPDATE of offer status (including old column
and table grants), while preserving ordinary driver edits. Lifecycle functions
run with their trusted owner's privileges and remain the only client route to
change offer status. This closes the reopen-after-start booking bypass.

Apply migration 7 only to a development/staging copy with the existing schema
and migrations 1–6 already present. Then run `checks/01_trip_lifecycle_test.sql`.
That test intentionally finishes with the exception
`TRIP LIFECYCLE TESTS PASSED (rolled back)`: this exact message means the test
passed and its database changes rolled back. Any other error is a failure.

The expanded test covers completion, direct reopen attempts on active/completed/
cancelled offers, bookings after start, safe driver field edits, outsider/passenger
permission failures, cancellation of both confirmed and end-confirmed bookings,
repeat cancellation, and confirmation after cancellation.

Cancellation here means the driver cancels an **in-progress entire trip**. The
offer remains terminally cancelled; its seats are not reopened for sale. Passenger
booking cancellation with exactly-once seat restoration and pre-start offer
cancellation remain separate work. Do not describe this test as covering those
unimplemented flows.

Use another teammate's review plus actual staging results before approving the
PR. No SQL in this repository is applied to the shared Supabase by a Git push.

## Why the original snippets conflict

1. The first snippet renames user columns to snake_case; the fifth renames them
   back. Function bodies and Kotlin/webhook payload consumers are not all updated
   by a database column rename. Choose one version and migrate clients together.
2. The signup trigger runs BEFORE the Auth row insert even though usersId has a
   foreign key to auth.users.id. An AFTER trigger is appropriate for creating the
   referencing profile. It must handle NULL emails and pin its search path.
3. The all-tables snippet drops impact_stats, potentially losing data. Dropping
   a message column with CASCADE can remove policies/dependencies and its data
   before message relationships have been backfilled.
4. The original booking RPC permits anonymous execution, trusts a caller-supplied
   passenger UUID, and has no pinned search path. Its row lock/transaction is
   useful; keep those. Failed inserts roll back seat changes, so the existing
   seats CHECK already rejects nonpositive booked seats, but validate inputs
   explicitly and early.
5. The message ALL policy permits a caller to insert as themselves without
   membership in the referenced trip/booking. Split read and insert policies.
6. CREATE TABLE/ADD COLUMN IF NOT EXISTS does not repair an existing table's
   types, defaults, constraints or grants. It is not a full schema synchroniser.
7. Our earlier export checked impact_stats, but the supplied script creates
   user_impact_stats. Its presence and shape must be checked before concluding
   personal totals are absent. The reviewed scripts retain user_impact_stats.

## Deliberate naming choice

Keep public.users' existing quoted usersId/usersEmail/usersRole/vehiclePlate/
vehicleModel columns for now, matching the current Auth and FCM clients. Other
existing table columns remain snake_case. Do not rerun either rename snippet.
Kotlin models for offers, requests and messages still require snake_case mapping
and the geography fields; these SQL changes do not perform that Android migration.
A later coordinated migration can standardise the remaining legacy names,
including createdTime, across the database, Kotlin and deployed Edge Functions.

## Execution order

Use a development/staging copy containing the exported tables, enum types and
PostGIS objects in public. These files cannot initialise an empty project.
Preserve a database backup. Do not paste all five old snippets again.

1. Run `checks/00_read_only_preflight.sql`. Inspect user_impact_stats columns, existing
   triggers and column grants. Stop for any schema drift, multiple profile
   triggers, or an existing trigger that also changes seat availability. Supabase
   SQL Editor may show only the last result set; run each SELECT separately and
   export each result when reviewing it.
2. Run `20261115000001_existing_schema_additions.sql`. It preserves existing rows and uses
   user_impact_stats with a foreign key to users("usersId"). It adds a college
   field and a trip vehicle snapshot, but does not populate historical values.
3. Run `20261115000002_auth_profile_trigger.sql`. It preserves the trigger function name,
   creates an AFTER INSERT trigger and validates UM email domains. Test a new UM
   signup, rejected non-UM signup and expected phone-only rejection in staging.
   Email confirmation remains the responsibility of Supabase Auth settings.
   The later trigger export confirms on_auth_user_created also called
   handle_new_user() to create the same profile. Script 02 removes that duplicate
   trigger and preserves its unused function; only the UM-validated AFTER trigger
   remains. This does not alter the bookings notification webhook.
   Existing accounts missing profiles still need a separate audited backfill.
4. Run `20261115000003_book_seat_transaction.sql`. It retains the five-argument RPC signature,
   checks auth.uid(), validates seats/coordinates and prevents self-booking,
   expired offers and insufficient capacity. Failed duplicate bookings roll back;
   retries return a duplicate error rather than returning the original ticket.
5. Review the behaviour below, then run `20261115000004_access_controls.sql` in staging. It
   aborts if unexpected policies are present on the key protected tables.
6. Run `20261115000005_role_profile_cost_cap.sql`: usersRole becomes read-only for clients (role changes
   go through `set_my_role`), adds `public_profiles` and a configurable cost-share cap
   (default RM 5.00 in `app_config`). Not run yet.
7. Run all acceptance checks below before promoting the reviewed changes into
   dated migrations and applying them to the shared project.

Each edit file has its own BEGIN/COMMIT. If a statement fails, run ROLLBACK in
that editor session before continuing. Earlier committed files are not rolled
back when a later file fails. No live database execution has been performed here.

## Access-control behaviour to review

- Raw users rows are readable only by their owner. The old app-wide user SELECT
  policy is removed. Implement an explicit minimal public-profile API for driver
  names/vehicle display later; do not restore public access to tokens/contacts.
- Clients cannot update user identity, email or trust_rating through table writes.
  The default five-star rating is removed for new profiles; existing values are
  preserved and still need recalculation from valid reviews. usersRole is a
  commuter-role field: confirm its enum cannot contain staff/admin privileges
  before allowing the provided client role update grant.
- Bookings and trips can be read by participants. Direct client writes are not
  granted; booking goes through the RPC. Cancellation and trip lifecycle RPCs
  must be implemented before those operations are available.
- Direct client UPDATE of available_seats and DELETE of offers are removed.
  Capacity changes/cancellation require validated backend operations.
- Messages can be read/sent only in one referenced trip OR booking, and sender_id
  must match the authenticated user. Clients cannot update/delete messages.
  Existing messages with both target IDs remain stored but are hidden by these
  policies until their intended conversation is repaired. Membership helpers
  intentionally allow historical participants; cancellation/no-show chat rules
  still need agreement. The helper owner must be a trusted migration role with
  access to the underlying tables and must not be changed to a client role.
- Raw aggregates AND dashboard view are unavailable to ordinary clients until
  staff-authorised queries and group-count correctness are implemented. The
  existing >=5 view filter is retained, but it does not prove the stored count
  represents distinct users. A separate approved community view may expose only
  permitted faculty/college totals; do not grant raw-table access.
- Unexpected legacy column grants must be removed after the preflight review.
  Table-level REVOKE alone does not remove separate column-level grants. These
  files remove the known sensitive UPDATE grants; pre-existing unrelated grants,
  roles, other RPCs, or views can provide additional access and require review.

## Acceptance checks (not yet executed against PostgreSQL)

- New UM Auth user gets one profile with matching UUID; invalid domains fail.
- Anonymous RPC call fails; passenger A cannot supply passenger B's UUID.
- Zero/negative/NULL seats, invalid coordinates and self-booking fail.
- Two different users racing for the last seat: exactly one confirmed booking.
- Duplicate booking attempt leaves seat count unchanged.
- RPC creates one booking and deducts seats once, including with existing webhooks.
- Users see only their private profile; FCM token update still works; trust_rating
  edits fail. Drivers/passengers can read their own booking/trip and nobody else's.
- Sender spoofing and messages into another conversation fail; legitimate
  booking/trip participant messaging succeeds. Test revoked UPDATE/DELETE too.
- Personal totals can be read only by their owner, with no client write access.
- Raw aggregate/dashboard access is denied until an authorised reporting endpoint
  exists. Test four-vs-five DISTINCT contributors when that endpoint is added.

## Remaining requirements — these files do not complete the whole backend

- Populate personal/weekly impact totals from an idempotent trip-completion
  operation. Define the CO2 formula, attribution, college/week grouping and
  distinct contributor counting. Creating columns does not calculate totals.
- Create/link trips and bookings, snapshot vehicle registration, support exactly-
  once cancellation seat restoration, and record actual trip completion counts.
- Validate ratings against completed-trip participation and derive trust scores.
  The current schema already has a unique trip/reviewer/reviewee rating constraint.
- Staff roles must be trusted backend assignments, independent of commuter roles.
- Align Kotlin serial names, required geography payloads and RPC calls with the
  live schema, and migrate any affected deployed notification functions.
- Review the actual booking/message/SOS Edge Functions, webhook authentication,
  secrets, FCM notification payloads, device logout/unlink and background delivery.
  None of those server functions was provided with the five SQL snippets.

Validation performed here: PostgreSQL SQL syntax parsing, PL/pgSQL syntax checks
where the parser supports the body, comparison with supplied schema exports, and
`git diff --check`. No live SQL, signup, RLS, concurrency, Android, or push-delivery
checks have been completed. These are staged review candidates, not verified
production migrations.
