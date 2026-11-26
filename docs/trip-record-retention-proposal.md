
# UMPooling — Trip Record Fields and Retention Proposal

**Owner:** Member 6
**Status:** PROPOSAL — retention duration, authority and implementation not approved

## 1. Purpose

FR4.1 requires a safety/accountability record available to eligible trip participants and authorised administrators. This document maps the required fields to the known database and identifies decisions needed before defining a retention rule.

## 2. Required information and known data sources

| FR4.1 field | Intended source | Note |
|---|---|---|
| Trip ID | `trips.trip_id` | Unique trip identifier |
| Vehicle registration | `trips.vehicle_plate_snapshot` | Preserve the value associated with the trip, not a later profile edit |
| Origin / destination zones | `trips.origin_zone`, `trips.destination_zone` | Coarse zone labels; avoid implying these are live GPS coordinates |
| Driver ID | `trips.driver_id` | Only display as approved to eligible viewers |
| Passenger IDs | `bookings.passenger_id` linked by `trip_id` | Not stored as one array on the `trips` table |
| Start / end | `trips.start_time`, `trips.end_time` | `end_time` may be absent while the trip is active |

The existing `trips` table also contains status, distance, passenger count, CO2 estimate and other fields. They are not automatically necessary for every safety-related view.

## 3. Access policy proposed for review

- The trip driver may view their authorised trip records.
- A passenger may view records for trips in which their booking is eligible under the approved access policy.
- An unrelated authenticated user must not be able to obtain those records or associated passenger IDs.
- Any administrative access must be separately justified and implemented in backend permissions. The `staffAnalyst` role intended for anonymised sustainability reporting should not automatically gain unrestricted identifiable trip access.

## 4. Retention rule — open decision

**No retention duration is approved yet. Do not write "X months" as a fact in the app or report.**

The team must agree and document:

1. Trigger for the retention clock: trip completion, cancellation, account closure or another justified point.
2. Duration for identifiable trip, booking and SOS information, and whether records are needed for handling a reported safety incident.
3. Whether the final action is deletion, irreversible anonymisation or a documented restricted archive.
4. Separate treatment of aggregate impact data used in the dashboard.
5. Whether a documented safety complaint requires a narrowly limited hold, who may authorise it and how it ends.
6. Technical enforcement: scheduled job, approved database function or controlled administrative process; tests and audit evidence.
7. User-facing privacy wording, consistent with the approved policy.

## 5. Proposed tests after approval

- Eligible participant may retrieve an in-retention trip record.
- Unrelated user is denied access to the same record and linked booking identifiers.
- Identifiable record is deleted or anonymised at the approved deadline in the test environment.
- A safety-related hold, if approved, is applied and released only by authorised personnel.
- Aggregates do not reidentify individual commuters.

**Review required:** Members 1 and 2, and the project's authorised decision-maker/lecturer as appropriate. Implementation and deletion tests have not been performed.
