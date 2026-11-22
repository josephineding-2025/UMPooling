# UMPooling — Refined User Flow and Functional Specification

## Document basis

This specification combines the proposed screen flow with the requirements in [Copy of WIA2007 MAD.pdf](https://chatgpt.com/library?file_id=libfile_ea0a9a5b16fc8191bd9b48de702f514f&account_id=personal).

The 30-minute branching rule, 1–3 requested passenger seats, and three-second SOS hold are additional user-supplied interaction rules. This describes intended behaviour, not current implementation status.

## 1. Authentication and onboarding

Users register or sign in using **@siswa.um.edu.my** or **@um.edu.my** accounts. Email verification must be completed before accessing live ride features.

During profile setup, users select **Passenger, Driver, or Both**, then provide their faculty and residential area. Drivers also provide the required vehicle details. Users can configure emergency contacts and notification preferences.

## 2. Home and main navigation

Home displays a welcome message, ride-search form, available rides, and recommended matches. Ride cards show the route, departure time, available seats, contribution per seat, driver rating, and estimated CO₂ savings.

| Action | Destination |
|---|---|
| Find a Carpool | `FindRideScreen` |
| Offer a Ride | `OfferRideScreen` |
| Carbon Footprint | `SustainabilityDashboardScreen` |
| Saved | `SavedRoutesScreen` |
| Profile | `ProfileScreen` |
| My Trips | Upcoming, pending, active, and past trip records |
| Microphone button | `VoiceRequestScreen` |
| Notifications | Notifications inbox, separate from notification settings |

Search choices entered on Home must carry forward into the next screen.

## 3. Posting a ride offer

In `OfferRideScreen`, the driver enters or selects:

- Origin and destination, with valid pickup/drop-off locations.
- Departure date and time.
- Available passenger seats.
- Suggested contribution per seat.

The app **reads the contribution cap from backend configuration**, displays it, and validates the entered amount. The backend enforces the cap again when saving.

After a successful post, the offer appears in the filtered ride feed and can trigger notifications for passengers with compatible saved routes. Posting failures preserve the form and provide a retry action.

**Contributions are settled outside the app; there is no in-app payment.**

## 4. Finding a ride

### 4.1 Manual entry

The passenger taps **Find a Carpool** to open `FindRideScreen`, then reviews or edits:

- Origin and destination.
- Departure date and time.
- Requested seats: **1–3**, subject to the selected vehicle’s remaining capacity.

The user taps **Confirm & Find Rides**. Empty locations, identical origin/destination, invalid seats, and past departure times receive clear validation messages.

### 4.2 Voice-assisted entry

The passenger taps the microphone button to open `VoiceRequestScreen`.

1. Press and hold **Hold to Talk**, granting microphone permission when needed.
2. Speech recognition converts the recording into visible, editable text.
3. A cloud language model extracts **origin, destination, departure date/time, and seats**.
4. The app opens `FindRideScreen` with those fields prefilled.
5. The passenger reviews, corrects, and confirms the form.

Speech transcription and AI field extraction are separate steps. Missing or ambiguous information must be highlighted rather than silently guessed. **Fill in manually** opens the same form directly, preserving any information already entered.

## 5. Matching and departure-time branching

After confirmation, the system filters offers using compatible origin/pickup area, destination, departure time, and sufficient seats. The **30-minute rule determines the presentation**, while route/time compatibility determines which rides actually match.

| Requested departure | Result |
|---|---|
| Now through the next 30 minutes, inclusive | Immediate ride results and driver details |
| More than 30 minutes away | Advance ride results and pickup-group recommendations |
| Already in the past | Ask the passenger to update the departure time |

### 5.1 Immediate rides

The passenger selects a result to view the driver card, trust rating, vehicle details, price per seat, remaining seats, estimated CO₂ savings, and pickup-to-destination timeline.

Tapping **Request to join** opens a reservation summary showing the requested seats and total contribution. The system then attempts the transactional booking.

### 5.2 Advance rides and pickup groups

`RequestFutureRideScreen` recommends passengers from nearby residential areas with compatible destinations and departure windows.

Each group shows its pickup area, proposed departure window, passenger count, and driver/vehicle status.

- **Driver and reservable offer available:** joining attempts a transactional booking.
- **No driver assigned:** joining saves a **Pending group request** in My Trips. It does not reserve a seat or produce a confirmed ticket.
- When an offer becomes available, the passenger reviews the final details and confirms the reservation.

When no match exists, users can adjust filters, save the route for notifications, or submit an unmatched ride request that notifies compatible drivers.

## 6. Transactional booking and confirmed ticket

The connected sequence is:

1. Driver posts an offer.
2. Passenger filters offers.
3. Passenger selects offer details.
4. Passenger confirms the reservation.
5. Backend books seats atomically.
6. Ticket loads by booking ID.

The backend checks offer status, departure time, contribution rules, and remaining capacity. It creates the booking and reduces availability in one transaction.

**Show confirmation only after a successful booking.** Disable repeated submissions while processing. If the offer becomes full, expires, or fails, show the reason and refresh availability.

The confirmed ticket displays:

- Pickup, destination, and departure time.
- Driver and vehicle details.
- Number of reserved seats.
- Contribution per seat and total.
- Booking status and a unique booking reference.
- **Message driver** and **SOS** actions, with SOS activation available during an active trip.

For example, **RM4 × 2 seats = RM8**. RM4 and **UMP-2841** are sample values; production tickets use the actual contribution and a reference associated with the booking.

## 7. My Trips and chat

My Trips distinguishes **Pending group requests, Confirmed bookings, Active trips, Completed trips, and Cancelled records**.

Opening a confirmed booking loads its ticket by booking ID. Selecting **Message driver** opens the conversation associated with that booking or trip.

Chat supports quick replies, typed messages, timestamps, message history, and sending/failure/retry states. Quick replies let the user review or send common coordination messages.

An **auto-reply may be used in an explicitly labelled demo**. In live use, replies come from the other participant unless a separate automated assistant is deliberately introduced.

## 8. Active trip, SOS, and completion

The driver starts the trip, making its active status visible to participants.

### 8.1 SOS interaction

During an active trip, holding **SOS for three seconds** prepares the emergency action. Releasing early cancels the hold. Before handoff, the user can cancel the action.

To match the document’s requirement, SOS opens **SMS or WhatsApp with a prefilled message** containing trip details and the available current location, addressed to the configured emergency contact or UM Security. The user completes sending in that application.

The app must not claim delivery merely because the composer opened. Cancellation cannot retract an already-sent message.

### 8.2 Trip completion

At trip end, passengers confirm completion and the driver finalizes the trip. The system records confirmed passenger count and distance, then displays estimated CO₂ savings and updates personal impact totals.

## 9. Rate & Review

After completion, users can select **1–5 stars**, choose feedback tags, and add an optional written review.

- **Submit:** save the review successfully, then return Home.
- **Skip:** return Home without creating a review.
- **Submission failure:** preserve the review and offer retry.

Ratings contribute to the other participant’s community trust rating.

## 10. Saved routes, notifications, and sustainability

### 10.1 Saved recurring routes

Users can add, edit, and delete recurring routes, including their residential area and preferred departure window.

### 10.2 Matching notifications

Matching works in both directions:

- **New driver offer → notify passengers with compatible saved routes.**
- **New passenger request → notify drivers with overlapping saved routes.**

Notifications show relevant departure, destination, seat information, estimated CO₂ savings, and request/booking status. Opening a notification leads to the associated offer or request.

### 10.3 Sustainability dashboard

The sustainability dashboard presents per-trip and cumulative estimated CO₂ savings, weekly/monthly trends, commuting tips, and Green Commuter Tier progress.

Community leaderboards compare **faculties or residential colleges**. Authorised staff receive anonymised aggregate views.

CO₂ values shown before booking are **estimates**; completed-trip totals use recorded distance and confirmed occupancy. This connects the carpooling journey to the project’s climate-awareness objective.
