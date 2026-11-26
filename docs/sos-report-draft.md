
# FR4.2 — Emergency SOS: Report Paragraph Draft

**Owner:** Member 6
**Status:** DRAFT — do not submit as an implementation result until code and test evidence are added

## Design paragraph (may be used after team review)

UMPooling's Emergency SOS function is designed as a safety-support feature for authenticated participants in an active carpool trip. The proposed interaction requires a deliberate press-and-hold action before preparing a message containing available trip information and current location, where permission is granted. The design proposes SMS and WhatsApp as external messaging channels; the user reviews and sends the message manually. This approach does not constitute an automated emergency dispatch service, and opening a messaging composer alone does not demonstrate successful message delivery. To protect trip-related information, any optional SOS database record should be subject to server-side checks of the user's identity, trip participation and active-trip status. The intended recipient, record-logging rules and fallback procedures remain subject to team approval.

### Implementation paragraph

The FR4.2 Emergency SOS feature has been integrated into UMPooling's existing safety interface and active-trip navigation. The implementation includes a three-second press-and-hold activation mechanism, foreground location-permission handling, retrieval of the user's saved emergency contact, and preparation of an emergency message containing available trip information and a location link where obtainable. Users may open SMS or WhatsApp to review and manually send the prepared message. The feature does not automatically dispatch emergency assistance or confirm message delivery.

### Testing paragraph

The Android debug build and JVM unit tests passed locally on 26 November 2026. GitHub Actions CI for Draft PR #19 also passed. Device-level testing remains incomplete due to emulator performance issues. Consequently, the three-second activation, location handling, messaging handoff and complete SOS workflow have not yet been verified on a physical device or emulator. Supabase RLS verification also remains outstanding.

## Figure and evidence placeholders

- SOS screen figure: `evidence/[actual-screenshot-name].png` — not yet captured.
- Test evidence: `evidence/sprint1-test-results.md` — not yet executed.
- Reviewed source branch/PR: `feature/fr4-2-sos-review` / (https://github.com/josephineding-2025/UMPooling/pull/19).
- Backend review: Member 2 decision pending.
