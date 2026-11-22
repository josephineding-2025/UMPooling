# UI handoff — cleanup status

This is a code-cleanup handoff, not certification that every Member 3 deliverable is complete.

## Ownership and entry points

- booking/MyTripsScreen.kt and MyTripsState.kt: trip-list presentation and display mapping.
- booking/ConfirmedTripTicketScreen.kt and TicketDetails.kt: shared demo/live ticket presentation contract.
- chat/ChatScreen.kt and ChatViewModel.kt: conversation presentation, sending and retry state.
- notifications/NotificationScreen.kt: reusable inbox presentation; demo fixtures stay explicitly separated.
- notifications/NotificationRoute.kt: thin Compose/ViewModel adapter.
- notifications/NotificationViewModel.kt and NotificationRepository.kt: inbox state, persistence and participant-checked destination effects.
- ui/AccountViewModel.kt: serialized settings/saved-label writes. Its ViewModelStore is cleared when the authenticated/demo scope leaves composition.
- ui/components/CommuterNavigation.kt: the only floating bottom-navigation implementation.
- safety/EmergencyContactScreen.kt and LiveSosPanel.kt: emergency-contact editing and the live SOS composer handoff.
- coordination: shared participant records, lifecycle repository/ViewModel and trip orchestration.

## Cleanup delivered

- Removed the duplicate Home and notification navigation implementations; Home, Profile, Dashboard, My Trips and Notifications use the shared component.
- Moved the My Trips files into booking, ChatViewModel into chat, and renamed/moved the live inbox adapter to notifications/NotificationRoute.kt.
- Removed notification storage/network operations from the composable. Notification identity is now owned by notifications rather than coordination.
- Settings and saved-route writes use AccountViewModel rather than composable-launched persistence.
- Signed-in booking tickets use the same styled ticket as demo, populated with actual route, departure, seats, price and booking reference. Missing profiles/vehicle details are explicit; live tickets do not invent verified badges.
- Active booking tickets retain access to trip lifecycle controls. Inactive/cancelled tickets cannot activate SOS.
- A successful My Trips SOS hold passes prepared=true to the active-trip SOS review. Direct SOS entry still requires a hold. Review/cancel and manual SMS/WhatsApp sending remain required. No delivery is claimed. Demo hold completes with the existing demo notice.
- Shared navigation labels use English/Malay resources and selected controls have a 48 dp minimum height.
- Notification tests cover successful/denied destinations, repeated intent handling, mark-all scope and identity stability.

## Remaining UI/UX work (not completed by this cleanup)

- Replace generic profile setup, immediate results, pending-group and demo lifecycle hubs with designed screens.
- Finish trip-end CO2 results, community leaderboard and authorised staff aggregate presentation/contracts.
- Complete localization of the newer screen/state copy; several labels still use English literals.
- Centralize remaining per-screen fonts/colors and audit contrast, touch targets, TalkBack, keyboard focus and 200% font scaling across the whole app.
- Implement and visually validate dark mode; the current theme is intentionally light-only.
- Replace chat polling with the agreed real-time integration and verify two-account messaging.
- Complete saved-route editing and coordinate-backed route integration with the matching owner.
- Prepare the usability script, run student sessions, record time to book, and save the report screenshot/device evidence set.
- Review and complete TASKS.md, author identification in AI_Usage_Log.md, and report sections using actual evidence.

## Integration dependencies

No Supabase migrations were deployed. Lifecycle migration 7 and participant offer-read migration 8 still require backend-owner review and staging checks. Live posting/reservation creation, automatic current location, server SOS audit records, ratings persistence, impact data and cross-device settings/routes are separate integrations.

## Reviewer checks

1. Run assembleDebug and testDebugUnitTest; inspect NotificationViewModelTest and existing auth/route/coordination tests.
2. In demo, visit every bottom destination, filter My Trips, open Ticket/Chat, and release an SOS hold early before completing a hold.
3. With staging accounts, verify booked/full/completed offers remain readable, tickets contain the correct account's data, notification denial does not navigate, and logout clears account UI state.
4. Compare Home, My Trips, Notifications and ticket phone/narrow previews with the target designs; test system Back and rotation.
5. Verify SMS/WhatsApp composers on a physical device without sending a message during QA unless explicitly authorised.
