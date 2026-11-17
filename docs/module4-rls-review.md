
# UMPooling — Module 4 RLS Review

Status: DRAFT — Pending Member 2 Review
Date: 18 November 2026
Owner: Member 6

## 1. Scope

This review covers the authorisation requirements for:

- FR4.2 — Emergency SOS
- FR4.3 — Ratings and Reviews

## 2. Existing Helper Functions

### is_trip_participant(p_trip_id uuid)

Returns true when the authenticated user is:

- The trip driver; or
- A passenger with a booking linked to the trip.

Does not check trip or booking status.

### is_booking_participant(p_booking_id uuid)

Returns true when the authenticated user is:

- The passenger associated with the booking; or
- The driver associated with its trip offer or trip.

Does not check booking or trip status.

## 3. FR4.2 — SOS Findings

Current policy:
- Users manage own sos alerts (ALL)
- Checks auth.uid() = user_id

Potential gaps:
- No explicit trip-participation requirement.
- No active-trip requirement.
- Users may be permitted to update/delete own alerts.

Proposed authorisation:
- Only eligible participants of an active trip may
  create SOS alerts.
- Read/update/delete permissions require separate
  decisions.

## 4. FR4.3 — Ratings Findings

Current INSERT policy:
- Checks auth.uid() = reviewer_id

Potential gaps:
- No explicit trip-participation requirement.
- No completed-trip requirement.
- No reviewee eligibility check.
- No explicit self-rating restriction.

Proposed authorisation:
- Only eligible participants of completed trips may
  submit ratings.
- Reviewer and reviewee must be different users.
- Rating eligibility for no_show/cancelled bookings
  requires confirmation.

## 5. Proposed Tests

SOS:
- Active trip participant can create an alert.
- Nonparticipant cannot create an alert.
- Participant cannot create an alert for a completed trip.
- User cannot impersonate another alert owner.
- Update/delete permissions match approved rules.

Ratings:
- Eligible participant can rate another participant
  after trip completion.
- Nonparticipant cannot rate trip participants.
- Ratings cannot be submitted before completion.
- Self-rating is rejected.
- Duplicate ratings are rejected.
- No-show/cancelled booking eligibility follows
  the approved team rules.

## 6. Pending Decisions

- Confirm SOS insert/read/update/delete permissions.
- Confirm whether SOS alerts require database logging.
- Confirm rating eligibility for no-show bookings.
- Confirm whether drivers and passengers can rate
  each other under all completed-trip scenarios.
- Review existing helper function execution permissions.
- Obtain Member 2 approval before deployment.
