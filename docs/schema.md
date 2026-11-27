# Database schema

Source: the live Supabase project as exported on 2026-11-15, plus migrations in `supabase/migrations/`. Column names are exact. `users` keeps quoted camelCase legacy names; every other column is snake_case.

## 1. Enums

| Type | Labels |
|---|---|
| `user_role` | passengers, drivers, both, staffAnalyst |
| `offer_status` | open, full, cancelled, completed |
| `request_status` | PENDING, SUCCESS, ERROR (uppercase) |
| `trip_status` | scheduled, in_progress, completed, cancelled |
| `sos_status` | active, resolved, false_alarm |

## 2. Tables

| Table | Key columns | Notes |
|---|---|---|
| `users` | `"usersId"` (pk, = auth user id), `"usersEmail"`, `"usersRole"`, `"vehiclePlate"`, `"vehicleModel"`, `"createdTime"`, `faculty`, `residential_area`, `residential_college`, `vehicle_type`, `fuel_category`, `trust_rating`, `emergency_contact` (jsonb), `fcm_token`, `display_name` | Email must end with a UM domain (table check plus trigger). `usersRole` is not writable by clients. Profile row is created by the auth trigger. |
| `saved_routes` | `route_id`, `user_id`, `origin_geo`, `destination_geo`, `distance_km`, `preferred_time`, `origin_zone`, `destination_zone` | Geo columns are NOT NULL (PostGIS geography). |
| `trip_offers` | `trip_offer_id`, `driver_id`, `origin_geo`, `destination_geo`, `cost_per_seat`, `available_seats`, `status`, `departure_time`, `created_at`, `origin_zone`, `destination_zone`, `distance_km` | From migration 10, `cost_per_seat` is nonnegative and limited by the configured cap trigger. Clients cannot change `available_seats` or, after migration 7, `status` directly. |
| `ride_requests` | `request_id`, `user_id`, `trip_offer_id` (nullable), `pickup_geo` (NOT NULL), `destination_geo`, `preferred_time`, `status`, `requested_at` | One request per user and offer (`unique_passenger_offer`). A request with no offer is allowed. |
| `bookings` | `booking_id`, `trip_offer_id`, `trip_id`, `passenger_id`, `seats_booked`, `pickup_geo`, `status`, `created_at` | Created only by `book_seat_transaction`. Unique per offer and passenger. `trip_id` is filled when the driver starts the trip. `status` values used: confirmed, completed (passenger confirmed the end), no_show, cancelled. |
| `trips` | `trip_id`, `trip_offer_id`, `driver_id`, `status`, `start_time`, `end_time`, `origin_zone`, `destination_zone`, `distance_km`, `confirmed_passenger_count`, `co2_saved_kg`, `vehicle_plate_snapshot` | Read-only for clients. Created and changed only by the trip lifecycle functions. |
| `messages` | `message_id`, `sender_id`, `content`, `sent_at`, `trip_id`, `booking_id` | A message must target a trip or a booking. The chat policies allow exactly one. |
| `ratings` | `rating_id`, `trip_id`, `reviewer_id`, `reviewee_id`, `rate` (1 to 5), `comment`, `createdTime` | One rating per trip, reviewer and reviewee (`unique_trip_rating`). |
| `sos_alerts` | `alert_id`, `trip_id` (NOT NULL), `user_id`, `location_geo`, `status`, `alert_time` | An SOS needs a trip. |
| `user_impact_stats` | `user_id` (pk), `total_trips`, `total_distance_km`, `total_co2_saved_kg`, `updated_at` | Owner can read. Not yet populated. A `tier` column is still to be added. |
| `impact_aggregates` | `aggregate_id`, `week_start`, `faculty`, `residential_area`, `residential_college`, `active_user_count`, `trip_count`, `total_distance_km`, `total_co2_saved_kg` | Closed to clients. Not yet populated. |
| `app_config` | `key`, `value` | Settings such as `max_cost_per_seat`. Clients can read. |

Views: `public_profiles` (user_id, display_name, trust_rating), `anonymised_impact_dashboard` (closed to clients).

### Booking and contribution contract

Migration 10 exposes the configured per-seat contribution cap through `max_cost_per_seat()` and uses the same value in the offer trigger. Migration 12 adds offer quote versions, the `book_seat_with_quote` RPC and booking snapshots for the reference, contribution, route and departure. Offer review uses the offer ID. The successful reservation returns the booking ID used to load the ticket. See the [user flow](design/User_Flow.md).

## 3. Functions and triggers

| Name | Kind | Purpose |
|---|---|---|
| `handle_new_um_user` / `on_auth_user_created_um` | trigger on `auth.users` | Rejects non-UM emails and creates the profile row. |
| `book_seat_transaction(p_trip_offer_id, p_passenger_id, p_seats, p_pickup_lng, p_pickup_lat)` | RPC | Locks the offer row, checks status, departure and seats, creates the booking and reduces seats in one transaction. Signed-in callers only, and only for themselves. |
| `set_my_role(p_role)` | RPC | Switches the caller between passengers, drivers and both. Never changes a staff account. |
| `enforce_cost_cap` / `trip_offers_cost_cap` | trigger | Rejects a cost per seat above the configured cap. |
| `start_trip(offer)`, `confirm_trip_end(trip)`, `complete_trip(trip)`, `cancel_trip(trip)` | RPC | Trip lifecycle (migration 7, **written but not yet applied**). See section 5. |
| `is_booking_participant`, `is_trip_participant` | helpers | Used by the read policies. |
| `notify_push` / `notify_booking`, `notify_message`, `notify_sos` | triggers | Call the `swift-worker` Edge Function. |
| `handle_new_user` | unused | Kept for audit only. |

## 4. Row Level Security (summary)

| Table | Who can do what |
|---|---|
| `users` | Read and update own row only. Updatable columns: display_name, vehiclePlate, vehicleModel, faculty, residential_area, residential_college, vehicle_type, fuel_category, emergency_contact, fcm_token. |
| `trip_offers` | Anyone signed in can view open offers. Drivers manage their own, but cannot change seats. |
| `ride_requests` | Passengers manage their own. Drivers can view requests for their offers. |
| `bookings`, `trips` | Participants can read. No client writes. |
| `messages` | Participants of the booking or trip can read and send, and only as themselves. No edits or deletes. |
| `ratings` | Anyone signed in can read. Insert policy exists (definition to be reviewed). |
| `sos_alerts`, `saved_routes` | Users manage their own. |
| `user_impact_stats` | Owner reads. |
| `impact_aggregates` | No client access. |

## 5. Trip lifecycle

Database states: **scheduled, in_progress, completed, cancelled** (`trip_status`). A `trips` row is created when the driver starts the trip, so `scheduled` is not used yet (a booked ride is a `bookings` row with status `confirmed` until then).

| Step | Who | Function | Result |
|---|---|---|---|
| Book | Passenger | `book_seat_transaction` | Booking `confirmed`, seats reduced, no trip yet |
| Start | Driver | `start_trip(offer)` | Trip `in_progress`, bookings linked, offer set to `full` so nobody else can book. Allowed from 2 hours before departure. Needs at least one confirmed booking. |
| Confirm end | Each passenger | `confirm_trip_end(trip)` | Their booking becomes `completed` |
| Complete | Driver | `complete_trip(trip)` | Needs at least one confirmed passenger. Others become `no_show`. Trip `completed`, `end_time` set, `confirmed_passenger_count` = seats on confirmed bookings, offer `completed`. |
| Cancel | Driver | `cancel_trip(trip)` | Only while in progress. Trip and offer `cancelled`, bookings `cancelled`. |

`distance_km` is copied from the offer when the trip starts. `co2_saved_kg` and the tier are filled by Member 4's trigger when a trip becomes `completed`. Counting rules (minimum distance, two counted trips a day) belong to that trigger.

Test: `supabase/checks/01_trip_lifecycle_test.sql` runs a full trip in a transaction that always rolls back.

## 6. Known open points

- The enum `request_status` uses uppercase labels; send them exactly.
- The `ratings` insert policy should require that the reviewer was on the trip (to be checked).
- A passenger cannot rebook an offer after cancelling, because bookings are unique per offer and passenger.
- `user_impact_stats` has no `tier`.
