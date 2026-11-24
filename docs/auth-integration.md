# Authentication integration

The Android app uses Supabase Auth. Firebase remains push-only.

- MainActivity obtains AuthViewModel from its saved-state-aware ViewModelProvider.
- The SDK restores and refreshes its persisted session. No token or password is placed in navigation or Compose saved state.
- Protected destinations require a verified UM email. Pending verification keeps only its normalized email in SavedStateHandle.
- Sign-up, password sign-in, email OTP verification and resend call AuthRepository. Descriptive registration metadata does not assign a privileged role.
- Auth access changes replace the previous account's navigation/form scope. Logout clears SDK session storage and the previous back stack.
- Token cleanup and logout are serialized with FCM uploads. Server token cleanup/revocation are best effort while offline; local session clearing still runs.
- Demo access is a separate AuthAccess.Demo state with a visible banner and exit action. It never signs in anonymously or marks a user verified. Entering demo clears an existing unverified session first; exiting a restored demo also clears any cached real session.

## Supabase project setup

Enable email/password authentication and require email confirmation. Configure the signup confirmation email template to include the six-digit `{{ .Token }}` for the existing code-entry screen. The app uses the SIGNUP OTP type. This change does not modify any remote settings or email templates.

If a user follows the default email confirmation link in a browser, they can choose “Already verified using the email link? Sign in” and then sign in with their password. The app does not consume email-link redirects or implement password recovery yet.

Configure SUPABASE_URL and SUPABASE_ANON_KEY in ignored local.properties. With missing configuration, auth displays a setup error and demo mode remains available. No real credentials belong in tests.

Backend RLS must continue to authorize requests; the UI gate does not replace it. Other feature data remains the existing UI fixtures until its repositories are connected.

## Verification

AuthViewModelTest uses an injected gateway and coroutine scope to cover restoration, UM-domain/verification gates, signup payloads, OTP/resend, pending-email restoration, demo separation, logout, expiration, and duplicate-request suppression. A real project/device test is still needed for email delivery, process restoration and offline logout.
