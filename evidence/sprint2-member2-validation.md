# Sprint 2 Member 2 validation — 2026-11-26

Source main: 524eb3f3f62fca799d5abd8a9d3ab44a777aab0a.
Member 5 matching branch reviewed: afd37046472ab2ff66512cb01a1b4ee7a7aeb3fd.

## Executed database checks

PostgreSQL16/PostGIS3.5 in an isolated Docker container with --network none.
The synthetic baseline uses the known columns/constraints and stubbed Supabase
Auth, Vault and pg_net. It is not a snapshot of the live project.

Applied migrations 1–12. Passed:

- Existing lifecycle regression suite (including role/status/cancellation checks).
- Member 4 formula/factor parity, personal split conservation, tier thresholds,
  daily cap, repeated completion, sub-kilometre exclusion and no-show exclusion.
- Staff reads vs commuter reads; four-user group suppressed even at raw-cell
  table level; five-user group visible; personal ledger isolated by user.
- Configured RM12 cap permits RM11 after removing the old fixed RM10 check,
  and rejects RM13. Retry returns one ticket and does not decrement seats twice.
- Ticket price and route snapshots survive later offer edits. Stale quote versions are rejected for new reservations, while a retry can retrieve its existing unchanged ticket.
- Passenger cancellation restores a seat exactly once.
- Saved-route midnight match succeeds; out-of-window match fails; no fabricated
  seat capacity. FCM token moves to the currently authenticated account and
  logout unlinks it. Delivery leases reject duplicate claims and permit retries.
- Existing personal balances survive first newly credited completion.
- Route/vehicle snapshots survive changes after start; companion occupancy
  shares conserve the total while privacy counts two registered users, not seats.
- Two independent PostgreSQL sessions racing for the last seat: one success.
- Concurrent same-user booking retries: one booking; concurrent cancellations:
  seats restored once.
- Three independent completion sessions for overlapping participants: max two
  counted trips per registered user for that MYT day.

No live database rows, account settings or deployments were changed.

## Edge Function checks

- Node native TypeScript helper tests: 4 passed (event validation, secret equality,
  typed stale-token classification and non-fabricated matching text).
- Deno type check, formatting and lint passed for swift-worker.
- Firebase OAuth/send/cron execution needs configured staging secrets and real
  devices; no claim of real push delivery or measured five-second performance.

## Android

Executed `:app:assembleDebug :app:testDebugUnitTest` using JDK21, Gradle9.2.1
and SDK36.1. Final build: BUILD SUCCESSFUL. JUnit XML reports 104 tests,
0 failures, 0 errors, 0 skipped. No local.properties, Firebase JSON or real
Supabase/Firebase credentials were supplied. SDK/proxy/cache paths were isolated
under the task workspace and system CA trust used for Gradle downloads.

No device/emulator, screenshot or visual flow test has been performed. Those
remain actual staging/phone checks, not inferred from successful compilation.

## Manual handoff

See docs/sprint2-member2-handoff.md. Do not replay old SQL Editor snippets or
merge/deploy without reviewing migration state, staging evidence and the team
integration contracts, including the companion-credit convention.
