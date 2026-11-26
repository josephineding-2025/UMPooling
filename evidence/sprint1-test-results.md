
# Member 6 — Phase 1 to Sprint 1 Evidence and Test Register

**Status:** TEMPLATE — nothing below asserts a completed test.

## 1. Evidence register

| Task | Evidence path / link | Date | Status | Notes |
|---|---|---|---|---|
| Survey pilot (3–5 people) | PENDING | PENDING | Not verified | No respondent identifiers in Git |
| Survey distribution and reminder | PENDING | PENDING | Not verified | Record dates without personal contact details |
| Survey closure and anonymised export | PENDING | PENDING | Not verified | Check exported columns before publication |
| Assumptions review | `docs/assumptions.md` | PENDING | Draft only | Source/legal review needed |
| Phase 2 design review | PR #13 / meeting note | PENDING | Pending approval | Record reviewers and decisions |
| Sprint 1 SOS build | Local Gradle debug build (c95e24c) | 26.11.2026 | Pass — local build | `:app:assembleDebug` completed successfully; commit evidence pending |
| Sprint 1 SOS unit/UI tests | Local JVM unit tests; device UI tests pending | 26.11.2026 | Partial — unit tests passed | `:app:testDebugUnitTest` completed successfully; instrumented/UI tests not run |
| CI lint/build check | `.github/workflows/android-ci.yml` | 26.11.2026 | Workflow created; not verified | GitHub Actions execution pending; current workflow runs JVM tests and debug build, not lint |
| Local Supabase RLS tests | PENDING | PENDING | Not Run | Member 2 coordination required |
| Sprint 1 demo screenshot | PENDING | PENDING | Not captured | Synthetic data only |
| Report paragraph for SOS | `docs/sos-report-draft.md` | PENDING | Draft only | Update with implementation details and verified test evidence |
| AI use log entry | `AI_Usage_Log.md` | PENDING | Not verified | Record exact prompts and verification performed |

## 2. Test execution rows

Add one row per actual execution. Do **not** replace `Not Run` with `Pass` until observed.

| Test ID | Date | Git commit | Device / environment | Expected | Actual | Result | Evidence / defect |
|---|---|---|---|---|---|---|---|
| TC4.2-01 | PENDING | PENDING | PENDING | Hold 3 s activates | Not tested | Not Run | PENDING |
| TC4.2-02 | PENDING | PENDING | PENDING | Release early cancels | Not tested | Not Run | PENDING |
| TC4.2-03 | PENDING | PENDING | PENDING | SMS composer prefilled | Not tested | Not Run | PENDING |
| TC4.2-04 | PENDING | PENDING | PENDING | WhatsApp share prefilled | Not tested | Not Run | PENDING |
| TC4.2-05 | PENDING | PENDING | PENDING | Permission denial handled | Not tested | Not Run | PENDING |
| TC4.2-06 | PENDING | PENDING | PENDING | Location failure handled | Not tested | Not Run | PENDING |
| TC4.2-07 | PENDING | PENDING | PENDING | Missing app handled | Not tested | Not Run | PENDING |
| TC4.2-08 | PENDING | PENDING | PENDING | Backend outage does not block composer | Not tested | Not Run | PENDING |
| TC4.2-09 | PENDING | PENDING | PENDING | Nonparticipant insert rejected | Not tested | Blocked | RLS approval / local test project |
| TC4.2-10 | PENDING | PENDING | PENDING | Inactive trip insert rejected | Not tested | Blocked | RLS approval / local test project |
| TC4.2-11 | PENDING | PENDING | PENDING | No false delivery claim | Not tested | Not Run | PENDING |

## 3. Build / CI register

| Check | Command or workflow | Date | Actual output | Status |
|---|---|---|---|---|
| Compile Android debug build | `.\gradlew.bat :app:assembleDebug` | 26.11.2026 | BUILD SUCCESSFUL in 13s; 38 actionable tasks: 3 executed, 35 up-to-date | Pass (local build) |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest` | 26.11.2026 | BUILD SUCCESSFUL in 20s; 26 actionable tasks: 6 executed, 20 up-to-date | Pass (local unit tests) |
| Android lint | `.\gradlew.bat :app:lintDebug` | 26.11.2026 | FAILED — 105 errors and 87 warnings reported across the project; first reported error in `matching/SavedRoutesScreen.kt:177` (`SuspiciousIndentation`). Full report: `app/build/reports/lint-results-debug.html` (local, uncommitted). | Fail — project-wide findings require triage |
| Instrumented UI tests | `.\gradlew.bat :app:connectedDebugAndroidTest` on connected emulator/device | PENDING | Not run/verified | Not Run |
| PR CI checks | GitHub Actions run URL | PASSED | PR #19

**Verification note:** The local Android build and JVM unit-test tasks completed successfully on 26.11.2026. This does not establish that the instrumented SOS tests, device-level GPS retrieval, SMS/WhatsApp handoff or Supabase RLS tests have passed. The exact commit hash and CI workflow URL should be added after the changes are committed and the workflow has executed.

## 4. Limitations / blockers

- Approved emergency recipient(s): pending.
- Production-safe SOS database insert policy: pending Member 2 review.
- Android SOS UI integration: implemented and compiled locally on 26.11.2026. Device-level testing and reviewer approval remain pending. MVVM organisation has not been independently verified.
- Full team Sprint 1 evidence: check individual owner PRs separately; Member 6 must not claim their results.

**Android lint baseline comparison (26.11.2026)**

Android lint was executed against the clean `main` branch at commit `524eb3f` and the FR4.2 SOS review branch.

- Clean `main`: 105 errors and 85 warnings.
- SOS review branch (before the latest URI correction): 105 errors and 87 warnings.
- The total error count did not increase.
- Two additional warnings require final verification following the URI correction.

Accordingly, the project-wide lint failures are not, on the present comparison, attributable to an increase in errors introduced by FR4.2. Existing lint failures remain outstanding and should be addressed by the relevant code owners.

The SOS branch must still be checked for new warnings and regressions before merge.

**SOS device-testing limitation (26.11.2026)**

Android emulator testing could not be completed because the Android Studio environment experienced significant performance issues, and the application remained idle on the SOS screen.

Accordingly, the SOS device-level test cases remain unverified. The Android debug build and JVM unit tests have passed locally, but these results do not establish that the complete SOS workflow functions correctly on an Android device.

Device-level verification remains outstanding before the feature can be considered fully tested.