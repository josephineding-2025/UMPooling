# UMPooling user flow and functional specification

This document specifies the target commuter experience. See [Screen navigation](../navigation.md) and the [UI handoff](ui-handoff.md) for implementation status. The [project proposal](../UMPooling.md) lists the functional requirements.

## 1. Authentication and onboarding

Users register or sign in with an **@siswa.um.edu.my** or **@um.edu.my** account and verify their email before using live ride features. During setup, they select Passenger, Driver, or Both and enter their faculty and residential area. Drivers provide vehicle details. Users can configure emergency contacts and notification preferences.

## 2. Home and main navigation

| Home action | Destination |
|---|---|
| Find a Carpool | `FeedScreen` (Available Rides) |
| Offer a Ride | `OfferRideScreen` |
| Schedule a ride filter chip | `ScheduleRideScreen` |
| Carbon Footprint | `SustainabilityDashboardScreen` |
| Saved | `SavedRoutesScreen` |
| Profile | `ProfileScreen` |
| Bottom-menu record button | `VoiceRequestScreen` |
| My Trips | Pending requests and confirmed, active, completed, or cancelled trips |
| Notifications | Notifications inbox |

Carry any search criteria entered on Home into the selected ride flow. Ride cards show the route, departure, available seats, contribution per seat, driver rating, and estimated CO₂ savings.

## 3. Post a ride offer

1. In `OfferRideScreen`, the driver selects valid pickup and drop-off locations, sets the departure date and time and available seats, and enters a suggested contribution per seat.
2. Load the contribution cap from backend configuration (`app_config.max_cost_per_seat`, exposed through the `max_cost_per_seat` RPC). Display it and validate the amount before submission. If the cap cannot be loaded, preserve the draft and offer a retry. The backend enforces the cap when saving.
3. After the backend accepts the post, publish the offer for eligible searches and match notifications. Preserve the form and offer a retry if posting fails.

Passengers and drivers settle the contribution outside the app.

## 4. Find or schedule a ride

### 4.1 Browse Available Rides

**Find a Carpool** opens `FeedScreen` directly. Passengers can browse upcoming open offers and refine the results by origin or pickup area, destination, departure, and requested seats. Exclude the passenger's own offers and offers with insufficient seats. Tapping an offer opens its details in `BookingConfirmationScreen`.

### 4.2 Find a ride by voice or manual entry

1. Tap the bottom-menu record button to open `VoiceRequestScreen`. Press **Hold to Talk** to record, requesting microphone permission when needed. Display the transcript so the passenger can check and correct it.
2. AI extracts origin, destination, departure date and time, and requested seats from the text. Prefill the **Review Booking Form** in `LiveBookingScreen`. Flag missing or ambiguous details for the passenger to resolve.
3. **Fill in manually** opens the same form directly. Keep any reliable details already entered. This path is available if permission is denied or transcription or extraction fails.
4. The passenger reviews the fields and taps **Confirm** to search. When an eligible offer is found, open `BookingConfirmationScreen` with the matched driver's offer. Offer access to other matches in Available Rides when applicable. If no offer matches, let the passenger edit the form or schedule a ride.

### 4.3 Schedule a ride

1. Tap the **Schedule a ride** filter chip and edit origin, destination, departure date and time, and **1–3 seats** in `ScheduleRideScreen`.
2. Tap **Confirm Schedule**. If eligible offers exist, open `FeedScreen` with the schedule filters applied. Show compatible pickup groups where available. A group with a reservable driver offer leads to offer details. A group awaiting a driver can be saved as a pending request.
3. If the search succeeds with no matches, show a message explaining that continuing saves a pending request and that the passenger will receive a push notification when a compatible offer becomes available, subject to notification settings and device permissions. **Cancel** keeps the form open and creates no request. **Continue** saves the request and shows its pending status in My Trips after the save succeeds.
4. If searching or saving fails, retain the form and provide a retry. A failed search and an empty result have separate messages.

Validate missing or identical endpoints, past departure times, and seat counts on both passenger forms. Use route, time, and capacity compatibility to find offers. The entry action determines which screen opens.

### 4.4 Open a match notification

Tapping a scheduled-request or saved-route match notification opens `RideMatchScreen`. Resolve the offer and, where applicable, the request from their IDs. Refresh availability before allowing the passenger to proceed to offer details. If the offer has filled, expired, or been cancelled, show its current status and let the passenger return to matching results. Pending requests, group participation, and match notifications leave seats available until a reservation succeeds.

## 5. Review and reserve

1. `BookingConfirmationScreen` shows the selected offer's driver card, rating, vehicle, price per seat, requested seats and total contribution, and pickup-to-destination timeline. Confirm the pickup point before submitting.
2. **Confirm Booking** submits the selected offer ID, seat count, and pickup to the backend. Block repeated taps while the request is in progress. The backend validates eligibility, departure, current terms, and capacity, then writes the booking and reduces availability in one transaction.
3. After the backend returns a booking ID, load `ConfirmedTripTicketScreen` from that ID. Use the same ID when reopening the ticket from My Trips. If ticket retrieval fails after booking, retry loading the ticket by ID.
4. If booking fails, stay on the review and show the reason with a refresh or retry action. If the result is uncertain after a connection failure, check My Trips before resubmitting. If price or availability changes, refresh the offer and require the passenger to review it again.

The ticket shows the pickup, destination, departure, driver and vehicle details, reserved seats, per-seat and total contribution, status, and booking reference. Example ticket values are **RM 4 × 2 seats = RM 8** and **UMP-2841**. Live tickets use the recorded booking values and reference.

## 6. Coordinate and complete the trip

### 6.1 Chat

**Message driver** opens `ChatScreen` for the booking or trip. Provide quick replies, typed messages, timestamps, message history, and retry for failed sends. A labelled demo can show an auto-reply. Live replies come from the other participant.

### 6.2 Active-trip SOS

When the driver starts the trip, the ticket's **SOS** action opens the active-trip safety screen in `SafetyScreens.kt`. Hold **SOS** continuously for three seconds to prepare the emergency action. Releasing early or leaving the screen cancels the hold. After the hold, the passenger can cancel or choose **Call emergency services**, which opens the system dialer for the authorised responder. The passenger places the call in the dialer.

Where available, the passenger can also review a prefilled SMS or WhatsApp message with trip details and available location, addressed to the configured emergency contact or UM Security. The passenger sends it in that app. The safety screen reports the handoff separately from any call or message outcome.

### 6.3 Trip completion and review

Passengers confirm trip completion and the driver finalises the trip. Use recorded distance and confirmed occupancy to estimate CO₂ savings and update impact totals. Then invite users to **Rate & Review** with 1–5 stars, feedback tags, and an optional written review. **Submit** saves the review and returns Home. **Skip** returns Home immediately. Preserve feedback for retry if saving fails. Ratings contribute to the community trust rating.

## 7. Saved routes and sustainability

In `SavedRoutesScreen`, passengers can add, edit, or delete favourite recurring routes with a pickup area, destination, and preferred departure window. A newly posted compatible driver offer can trigger a prompt push notification for a saved route or pending request. A new passenger request can also notify drivers with overlapping saved routes. Match notifications identify the trip and show departure, destination, seat information, and estimated CO₂ savings when available. See [Matching rules](../matching-rules.md).

**Carbon Footprint** shows estimated per-trip and cumulative CO₂ savings, weekly and monthly trends, commuting tips, and Green Commuter Tier progress. Community leaderboards compare faculties or residential colleges. Authorised staff can view anonymised aggregates. Recurring-route matching helps commuters coordinate rides for early lectures while reducing single-occupant trips and demand for campus parking.
