
# UMPooling — Emergency SOS Flow Design

Status: DRAFT — Pending Team Confirmation
Owner: Member 6
Requirement: FR4.2 — Emergency SOS

## 1. Objective

Allow a driver or passenger participating in an active
trip to quickly prepare an emergency message containing
relevant trip information and available location data.

## 2. SOS Availability

The SOS button is intended to be available only when:

- The user is authenticated.
- The user is a participant in the trip.
- The trip status is `in_progress`.

Trip participation and status must be verified by the
backend before recording an SOS alert.

## 3. Proposed User Flow

1. The user opens an active trip.
2. The user presses and holds the SOS button for
   three seconds.
3. Releasing the button before three seconds cancels
   the activation.
4. After activation, the app prepares an emergency
   message containing available trip details.
5. The app attempts to obtain the user's current
   location, subject to permission and availability.
6. The app opens an SMS or WhatsApp composer with
   the prepared message.
7. The user reviews and sends the message through
   the external messaging application.

The app must not claim that the message was sent
merely because the composer opened.

## 4. Emergency Message Information

Proposed message fields:

- Trip ID
- Current location or available location link
- Origin and destination zones
- Vehicle plate snapshot, where available
- Emergency assistance request

Do not include unnecessary passenger personal data.

## 5. Error Handling

### Location Permission Denied

The user may still open the emergency message composer
without live location.

### Location Unavailable

The message should indicate that current location
could not be obtained.

### Messaging Application Unavailable

The app should provide a clear error and, where
supported, an alternative messaging option.

### Database Unavailable

Failure to record an SOS alert should not prevent
the user from opening an emergency message composer.

### No Active Trip

The normal trip SOS flow is unavailable.
The app should not create an SOS record for an
inactive or unrelated trip.

## 6. Backend Integration

Relevant table: `sos_alerts`

Relevant columns:
- alert_id
- trip_id
- user_id
- location_geo
- status
- alert_time

Valid SOS statuses:
- active
- resolved
- false_alarm

SOS insertion authorisation requires review before
implementation.

## 7. Open Decisions

- Final emergency recipient(s)
- Whether SMS, WhatsApp or both will be supported
- Whether the user selects the messaging application
- Whether SOS alerts must be logged in Supabase
- How alerts are resolved
- Whether an additional emergency-call option is needed
- Final error messages and accessibility behaviour

## 8. Proposed Android Implementation Design

Status: DRAFT — Pending Team Confirmation

### 8.1 Messaging Channels

The application is proposed to support:

- SMS
- WhatsApp

After SOS activation, the user selects the preferred
messaging channel.

The application prepares a prefilled emergency message
and attempts to open the selected external application.

The user is responsible for confirming and sending
the message.

The application must not claim successful delivery
based solely on opening the external composer.

### 8.2 Emergency Recipient

Status: NOT YET DECIDED

Possible recipients include:

- User's saved emergency contact
- UM Security
- Both, subject to channel capabilities

The final recipient selection and routing rules
require team confirmation.

Do not hardcode an unapproved emergency number.

### 8.3 Emergency Message Template

Proposed message:

"EMERGENCY SOS — UMPooling

I may need urgent assistance during a carpool trip.

Trip ID: [trip_id]
Origin: [origin_zone]
Destination: [destination_zone]
Vehicle: [vehicle_plate_snapshot]
Current location: [location_link_or_unavailable]

Please contact me or arrange assistance."

Only available, necessary information should be included.

### 8.4 Location Handling

- Request location permission when necessary.
- Use available current location if permission is granted.
- If permission is denied, continue without location.
- If location retrieval fails or times out, continue
  without location.
- Do not expose live location outside the intended
  emergency workflow.

### 8.5 SMS and WhatsApp Handoff

SMS:
- Open an SMS composer with a prefilled message.
- Include the recipient only when approved and available.
- Do not automatically send SMS.

WhatsApp:
- Attempt to open WhatsApp with a prefilled message.
- Recipient routing depends on the approved design
  and supported WhatsApp integration.
- Do not automatically send messages.

### 8.6 Supabase Integration

Relevant table: sos_alerts

A database alert may be created only when:
- The user is authenticated.
- The user is an eligible participant.
- The trip is in_progress.

SOS logging rules and database permissions remain
pending Member 2 review.

Failure to log an SOS must not block the external
emergency messaging handoff.

### 8.7 Pending Decisions

- Final emergency recipient(s)
- Exact SMS and WhatsApp recipient-routing behaviour
- Whether SOS activation automatically creates a
  Supabase alert
- Whether the user can resolve a logged SOS alert
- Fallback when neither messaging channel is available
- Final UI design and accessibility requirements
