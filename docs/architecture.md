# Architecture

Group 9 (FOPGang). Describes what exists today and what is still planned. Items marked **Planned** are not built yet.

## 1. Overview

```
Android app (Kotlin, Jetpack Compose, MVVM)
   |  supabase-kt: Auth, Postgrest, Realtime, Functions
   v
Supabase project
   |-- Auth (email + password, UM email domain trigger)
   |-- Postgres + PostGIS (tables, enums, RLS policies, RPC functions, triggers)
   |-- Realtime (chat)                                  [Planned]
   |-- Edge Function swift-worker (push notifications)
   |       |-- reads recipients with the service role
   |       '-- sends to Firebase Cloud Messaging (FCM HTTP v1)
   '-- Vault (webhook secret)

Firebase: used only for FCM push delivery (no Analytics, no Firebase auth or database).
```

## 2. Components

Client ownership and remaining UI work are documented in [UI handoff](ui-handoff.md). Bottom navigation is shared through CommuterNavigation. NotificationRoute delegates to NotificationViewModel/NotificationRepository; AccountViewModel owns settings and saved-label writes. My Trips and shared live/demo ticket presentation belong to booking; chat state belongs to chat.

| Component | Technology | Notes |
|---|---|---|
| Client | Kotlin, Jetpack Compose, MVVM, package `com.fopgang.umpooling` | Reads `SUPABASE_URL` and `SUPABASE_ANON_KEY` (the publishable key) from `local.properties` through `BuildConfig`. |
| Auth | Supabase Auth | Sign-up is restricted to `@siswa.um.edu.my` and `@um.edu.my` twice: in the app (`UmEmail`) and by the database trigger `on_auth_user_created_um`. |
| Database | Supabase Postgres with PostGIS | Row Level Security on every table. See `docs/schema.md`. |
| Business logic | Postgres functions (RPC) | `book_seat_transaction` locks the offer row so a seat cannot be double booked. `set_my_role` changes a commuter's role. `start_trip`, `confirm_trip_end`, `complete_trip` and `cancel_trip` are written (migration 7) but not yet applied. |
| Notifications | Edge Function `swift-worker` plus FCM | Database triggers call the function. See section 4. |
| AI (parser) | Edge Function calling a cloud LLM | **Planned** (Member 4). Same pattern as swift-worker. |
| CO2 and tier | Postgres trigger or function on trip completion | **Planned** (Member 4). |

## 3. Security model

- The app holds only the **publishable** key. The service-role (secret) key is never in the app or in the repository.
- RLS is on for every table. Clients can read only their own profile, their own bookings and trips, and chat messages of conversations they belong to.
- Clients cannot write `usersRole`, `trust_rating`, bookings, trips or seat counts directly. Role changes go through `set_my_role`, which never allows `staffAnalyst`. Staff accounts are assigned by the project owner in the dashboard.
- `public_profiles` exposes only name and trust rating to other signed-in users.
- The cost-share cap is enforced by a database trigger (default RM 5, configurable in `app_config`) in addition to the original RM 10 table check.
- Staff view: the raw `impact_aggregates` table and `anonymised_impact_dashboard` view are closed to ordinary clients. A staff-only read path is **Planned**. It must hide groups with fewer than 5 users.

## 4. Push notification data flow

1. A row is inserted into `bookings`, `messages` or `sos_alerts`.
2. The trigger `notify_push()` reads the webhook secret from Vault and calls `swift-worker` with the table name and the new row (asynchronous, so the insert never waits).
3. `swift-worker` rejects any request without the correct `x-webhook-secret` header (401).
4. It works out the recipients (booking: the driver; message: the other participants; SOS: staff analysts), reads their `fcm_token`, and sends the push through FCM.
5. The app stores its FCM token in `users.fcm_token` after sign-in (`FcmTokenSync`).

What leaves the system: only the notification title, a message preview of at most 120 characters, and ids. No location data is sent.

## 5. AI data flow (Planned, Member 4)

The app sends only the typed sentence to an Edge Function. The function holds the LLM key as a secret, adds the current date and time zone, validates the returned JSON against a schema, and returns it to the app to pre-fill the form. The user confirms or edits it. If the model fails or returns incomplete fields, the app falls back to manual entry. No personal data is sent to the model.

## 6. Trip lifecycle

See `docs/schema.md` section 5. The functions are written (migration 7) but not yet applied; until they are, a booking cannot become a completed trip.

## 7. Known gaps

- The base tables were created in the Supabase dashboard, so the migrations alone cannot rebuild an empty project (see `supabase/README.md`).
- Applying and testing the trip lifecycle functions, ride-request notifications (FR2.2), the staff aggregate read path and the CO2 and tier trigger are not built.
- Auth UI and session restoration are implemented. Physical-device push delivery and two-account staging integration still require recorded verification; the existence of a sign-in screen is no longer the blocker.
