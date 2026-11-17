# UMPooling — Trip Lifecycle Design

## 1. Purpose

This document defines the proposed trip lifecycle and its
relationship to booking, safety, SOS, trip records and ratings.

Status: DRAFT — Pending confirmation with Member 2.


## 2. Database Trip and Booking States

The live Supabase database defines the following trip_status
values:

- scheduled
- in_progress
- completed
- cancelled

The current start_trip function creates a trip directly
with status `in_progress`. The `scheduled` value is not
used by the inspected lifecycle functions.

Booking statuses used by the lifecycle functions include:

- confirmed
- completed
- no_show
- cancelled

Trip status and booking status are separate concepts.

## 3. Verified Trip Lifecycle Logic

The following behaviour was verified by inspecting the
deployed Supabase function definitions on 18 November 2026.

### 3.1 Seat Reservation

Function: `book_seat_transaction`

A successful booking produces a confirmed reservation.

The booking function's detailed implementation has not
yet been inspected as part of this verification.

### 3.2 Starting a Trip

Function: `start_trip(p_trip_offer_id uuid)`

The function:

1. Requires an authenticated user.
2. Verifies that the caller is the offer's driver.
3. Accepts an offer with status `open` or `full`.
4. Prevents starting an offer with an existing
   non-cancelled trip.
5. Allows starting no earlier than two hours
   before departure.
6. Requires at least one confirmed booking.
7. Creates a trip with status `in_progress`.
8. Links confirmed bookings to the new trip.
9. Changes the offer status to `full`.

### 3.3 Passenger Confirmation

Function: `confirm_trip_end(p_trip_id uuid)`

The function:

1. Requires an authenticated user.
2. Requires the trip to be `in_progress`.
3. Updates the caller's confirmed booking
   to `completed`.
4. Rejects users without an eligible confirmed booking.

### 3.4 Completing a Trip

Function: `complete_trip(p_trip_id uuid)`

The function:

1. Requires the caller to be the trip's driver.
2. Requires the trip to be `in_progress`.
3. Requires at least one completed passenger booking.
4. Counts confirmed passengers using the sum of
   seats_booked for completed bookings.
5. Marks remaining confirmed bookings as `no_show`.
6. Changes the trip status to `completed`.
7. Records end_time and confirmed_passenger_count.
8. Changes the offer status to `completed`.

### 3.5 Cancelling a Trip

Function: `cancel_trip(p_trip_id uuid)`

The function:

1. Requires the caller to be the trip's driver.
2. Allows cancellation only while `in_progress`.
3. Changes confirmed and completed bookings
   to `cancelled`.
4. Changes the trip status to `cancelled`.
5. Records end_time.
6. Changes the offer status to `cancelled`.

## 3.6 Verification Limitations

The database function definitions have been inspected,
but their runtime behaviour has not yet been tested.

The following remain to be confirmed:

- Whether the functions have been tested successfully
- Whether function execution permissions are appropriate
- Whether the lifecycle matches the team's approved UX
- Whether cancellation and no-show rules are final
- Whether the functions are deployed through Migration 7
  or another deployment process


## 4. Module 4 Dependencies

### FR4.1 — Trip Records

Trip records should contain the fields specified in FR4.1
and be accessible only to authorised participants and admins.

The retention period is not yet confirmed.

### FR4.2 — Emergency SOS

SOS is available during an active trip.

The proposed flow opens an SMS or WhatsApp composer with
trip information and available location data.

The final recipient rules, permission handling and
error behaviour require confirmation.

### FR4.3 — Ratings and Reviews

Ratings are available after trip completion.

Only eligible trip participants should be permitted
to review another participant.


## 5. Open Decisions and Security Findings

### 5.1 Confirmed Database Behaviour

The following were verified from the live database:

- Trip statuses: scheduled, in_progress, completed, cancelled.
- Booking statuses used by lifecycle functions:
  confirmed, completed, no_show, cancelled.
- Only the driver can start, complete or cancel a trip.
- A passenger can confirm completion of their own booking.
- The driver can complete a trip after at least one
  passenger booking has been confirmed as completed.
- Remaining confirmed bookings are marked no_show.

### 5.2 Security Findings — Pending Member 2 Review

**FR4.2 — SOS authorisation**

The existing sos_alerts RLS policy checks whether
auth.uid() equals user_id.

It does not explicitly verify trip participation or
that the trip is in_progress.

The team must confirm the intended SOS insertion,
update, deletion and access rules.

**FR4.3 — Rating eligibility**

The existing ratings INSERT policy checks whether
auth.uid() equals reviewer_id.

It does not explicitly verify that the reviewer and
reviewee participated in a completed trip.

Rating eligibility and self-rating restrictions require
Member 2's review.

### 5.3 Remaining Design Decisions

- Confirm final trip lifecycle UX with Member 2.
- Confirm whether the current no_show behaviour is intended.
- Define SOS recipient and location-permission handling.
- Define SOS database logging and error behaviour.
- Confirm trip record retention period.
- Confirm rating eligibility and permitted review actions.
- Verify lifecycle functions through runtime tests.
- Reconcile Migration 7 deployment documentation.


## 6. Confirmation

Member 6: Draft prepared.
Member 2: Pending review.
Final decisions: Not yet approved.



---
## Database Verification — 18 November 2026

The following tables were confirmed to exist in the live
Supabase database:

- bookings
- trips
- sos_alerts
- ratings

The following public database functions were also found:

- start_trip
- confirm_trip_end
- complete_trip
- cancel_trip

Verification status:
- Table structures: Confirmed
- Function existence: Confirmed
- Function implementation: Inspected
- Runtime behaviour: Not yet tested
- RLS policy definitions: Inspected
- RLS authorisation: Issues identified; runtime testing pending

Note: docs/schema.md previously indicated that Migration 7
had not been applied. The current database contains the
lifecycle functions, so deployment status requires
reconciliation with Member 2.
