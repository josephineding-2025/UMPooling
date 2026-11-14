# Android device token registration

The app includes Firebase Messaging through the existing Firebase BoM. On Android
13+, MainActivity requests POST_NOTIFICATIONS using the Activity Result API.
Denying the permission does not prevent sign-in or token registration, but the
system will not display normal notifications without permission.

UMPoolingApplication starts an application-scoped authenticated-session observer.
It fetches the latest FCM token after sign-in, verified sign-up, restored sessions,
and auth refresh, then updates only `fcm_token` on the signed-in user's profile.
UMPoolingMessagingService handles onNewToken callbacks. If a callback arrives
before login, the next authenticated session fetches the latest token instead.
No Activity reference or unscoped per-callback coroutine is retained.

Sync is serialized, checks account identity after fetching the token, preserves
coroutine cancellation and logs only error class names, never tokens. An upload
failure does not change the login result. It retries on a later authenticated
session (including app restart/session refresh), rather than continuously in the
background. There is no offline retry queue.

## Supabase prerequisites — confirm before running

- `public.users.fcm_token` must exist as a nullable text column.
- `FcmTokenSync.USER_ID_COLUMN` currently uses **usersId**, matching the checked-in
  UserProfile model. The supplied example instead uses **user_id**. The live
  schema has not been inspected: confirm which column contains `auth.users.id`
  and change this one constant to `user_id` if that is the actual column.
- The user profile row must already exist, with that column equal to the signed-in
  Auth user UUID. This code updates an existing row; it does not create profiles.
- With RLS enabled, authenticated users must be able to SELECT their own profile
  and UPDATE their own fcm_token, using the UUID relationship above. Do not allow
  clients to update another user's token. The returned-row check detects an
  update that affects no visible profile. No database schema/policy changes have
  been applied by this code change.
- Retain local Supabase configuration and `app/google-services.json` as described
  in database-integration.md and firebase-setup.md.

## Verification on a device

1. Build with `bash gradlew :app:assembleDebug :app:testDebugUnitTest` and install
   on an Android device/emulator with Google Play services.
2. On Android 13+, test granting and denying notification permission. Also run
   on Android 12 or below to confirm there is no runtime notification prompt.
3. Sign in and check that only that user's fcm_token changes in public.users.
4. Restart while signed in and verify token registration still works. Sign out
   before a fresh launch and verify no unauthenticated profile update occurs.
5. Rotate the FCM token in a development test and confirm onNewToken updates
   the profile; test login after rotation while signed out as well.
6. Disable networking, then sign in/relaunch after reconnecting: a failed token
   upload must not invalidate authentication and a later authenticated session
   should retry. Check Logcat tag FcmTokenSync if the profile/RLS update fails.
7. Trigger the bookings, new-message and sos-alert webhooks using test data and
   verify the server reads fcm_token and sends an FCM notification payload.

This implements token registration only. Foreground/data-message rendering and
notification channels are separate from this step. With a single fcm_token
column, the latest device replaces previous devices. A stored token also remains
after logout; before supporting shared devices, implement token unlinking in the
logout flow and remove invalid tokens on the sending backend.

Static manifest/registration/dependency/package checks and git diff --check passed.
Supabase 3.0.1 session-status/update API signatures were checked against upstream
source. Build and unit test execution was attempted but Gradle's distribution
download failed with connection refused before compilation. Device permission,
real profile updates, token refresh and webhook delivery remain unverified.
