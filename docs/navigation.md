# Screen navigation

Navigation Compose 2.8.5 owns the back stack. AuthViewModel gates verified accounts and isolates explicit demo access. Account changes discard the previous navigation scope.

- Home -> Find a Ride -> immediate results (within 30 minutes) or future pickup groups.
- Offer details -> booking confirmation -> confirmed ticket carries offer/booking IDs. Live posting, matching and reservation creation remain disconnected; demo confirmation is explicitly separate.
- Signed-in My Trips loads participant bookings/trips and driver offers through Supabase RLS. Driver offer, booking ticket and trip destinations use distinct IDs.
- Driver start, passenger end confirmation, driver completion and active-trip cancellation use existing lifecycle RPCs. Busy states block duplicate operations; failures remain visible; successful operations reload server state. Lists refresh every 10 seconds while displayed.
- The backend currently supports driver cancellation of an active trip, not pre-start/passenger cancellation. These unsupported operations are not represented as successful cancellations.
- Chat loads and inserts messages against exactly one booking or trip. Five-second polling refreshes messages. Failed sends retain the draft and message UUID for idempotent retry. Call/location controls are disabled until those integrations exist.
- Active-trip SOS uses the actual route, trip reference and vehicle snapshot. Holding for three seconds opens a confirmation; SMS/WhatsApp opens a composer for review and manual sending. Current location is entered manually; unavailable location is explicit. This does not create a server SOS alert or claim delivery.
- Emergency contacts, notification preferences, saved route labels and the received-notification inbox persist privately per account on this device. Demo storage is separate. They do not sync across devices. Saved labels do not create coordinate-backed server routes.
- Notification intents preserve resource IDs. Destinations are resolved through participant-visible server records before navigation, with chat taking precedence for message events. Foreground/data notification delivery checks local push/chat preferences and Android notification permission. Backend-generated background notification payloads must enforce server preferences separately.
- My Trips SOS passes `prepared=true` only after its hold completes, opening the active-trip review without another hold. Direct SOS entry retains the hold; both paths require explicit message handoff. Live tickets use TicketDetails rather than demo identity/vehicle values.
- All bottom destinations share CommuterNavigation. Notification operations use a ViewModel/repository; account settings and saved labels use AccountViewModel. See [handoff checklist](ui-handoff.md).
- Bottom destinations use single-top/popUpTo/saveState/restoreState. Forms and chat drafts use rememberSaveable; server state reloads on entry.

## Backend prerequisites and verification

Lifecycle migration 7 and participant offer-read migration 8 must be reviewed and applied to the target Supabase environment. Migration 8 is additive RLS access for participants after offers become full/completed; no remote SQL was applied during implementation.

Build and JVM tests cover compilation, route/form/auth contracts, lifecycle action permissions, conversation ID validation, retry IDs and duplicate-send suppression. Live two-account RLS/RPC checks, FCM delivery, physical-device composers and cross-process persistence require staging/device verification. Existing visual components are reused; live coordination hubs use the shared peach page/card/button styles.
