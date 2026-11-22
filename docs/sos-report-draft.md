
# FR4.2 — Emergency SOS: Report Paragraph Draft

**Owner:** Member 6  
**Status:** DRAFT — do not submit as an implementation result until code and test evidence are added

## Design paragraph (may be used after team review)

UMPooling's Emergency SOS function is designed as a safety-support feature for authenticated participants in an active carpool trip. The proposed interaction requires a deliberate press-and-hold action before preparing a message containing available trip information and current location, where permission is granted. The design proposes SMS and WhatsApp as external messaging channels; the user reviews and sends the message manually. This approach does not constitute an automated emergency dispatch service, and opening a messaging composer alone does not demonstrate successful message delivery. To protect trip-related information, any optional SOS database record should be subject to server-side checks of the user's identity, trip participation and active-trip status. The intended recipient, record-logging rules and fallback procedures remain subject to team approval.

## Implementation paragraph — COMPLETE ONLY AFTER REAL EXECUTION

Implementation details: **NOT YET VERIFIED**. Add the final Kotlin file paths, view-model and repository responsibilities, navigation entry point, SMS/WhatsApp intent handling, location permission behaviour, and the precise trip eligibility checks actually used.

## Testing paragraph — COMPLETE ONLY AFTER REAL EXECUTION

Tests and results: **NOT YET VERIFIED**. Insert the actual unit/UI test count, connected device/emulator, Android version, Git commit, observed pass/fail results, RLS test outcome and evidence paths. Do not state that the app or CI passed until the build/workflow output is available.

## Figure and evidence placeholders

- SOS screen figure: `evidence/[actual-screenshot-name].png` — not yet captured.
- Test evidence: `evidence/member6-sprint1-test-results.md` — not yet executed.
- Reviewed source branch/PR: `feature/fr4-2-sos` / link pending.
- Backend review: Member 2 decision pending.
