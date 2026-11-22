
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
| Sprint 1 SOS build | PENDING | PENDING | Not Run | Use exact commit and build log |
| Sprint 1 SOS unit/UI tests | PENDING | PENDING | Not Run | Use TC4.2 IDs |
| CI lint/build check | PENDING | PENDING | Not verified | Attach workflow run or blocker |
| Local Supabase RLS tests | PENDING | PENDING | Not Run | Member 2 coordination required |
| Sprint 1 demo screenshot | PENDING | PENDING | Not captured | Synthetic data only |
| Report paragraph for SOS | `docs/2026-11-23_member6_sos-report-draft.md` | PENDING | Draft only | Add actual implementation and test evidence |
| AI use log entry | `AI_Usage_Log.md` | PENDING | Not verified | Exact prompts and verification |

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
| Compile Android debug build | `.\gradlew.bat :app:assembleDebug` | PENDING | Not run/verified | Not Run |
| JVM unit tests | `.\gradlew.bat :app:testDebugUnitTest` | PENDING | Not run/verified | Not Run |
| Android lint | `.\gradlew.bat :app:lintDebug` | PENDING | Not run/verified | Not Run |
| Instrumented UI tests | `.\gradlew.bat :app:connectedDebugAndroidTest` on connected emulator/device | PENDING | Not run/verified | Not Run |
| PR CI checks | GitHub Actions run URL | PENDING | Not verified | Not Run |

## 4. Limitations / blockers

- Approved emergency recipient(s): pending.
- Production-safe SOS database insert policy: pending Member 2 review.
- Actual Android UI integration and MVVM organisation: pending review of current feature code.
- Full team Sprint 1 evidence: check individual owner PRs separately; Member 6 must not claim their results.
