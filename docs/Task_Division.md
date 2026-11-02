# UMPooling: Task Division for the Whole Project (6 members)

Group 9 (FOPGang), WIA2007 OCC 8. Replace the placeholders with real names. Dates assume calendar weeks; check the UM academic calendar for exam and break weeks and shift the sprints if needed. Requirement ids (FR1.1 to FR5.3, NFR1, NFR2) refer to UMPooling.md, Section 4 and 5.

## 1. Principles

- Every member owns one area end to end: **code, tests, report section, and video segment** for that area. This gives clear evidence for the report's Team Roles section and for the peer evaluation.
- Nobody is documentation-only. Each member writes the code for their own module and the report text about it.
- Each member contributes at least 20 hours. Keep a simple hour log (date, hours, task) in the shared sheet.
- Progress is tracked in Git only (no Trello). Every task has one owner and one FR/NFR id, and that id appears in the branch name and commit messages, so the git log shows who did what.
- Commit non-code work too (survey results, UM unit emails, wireframes, test results, report drafts) under evidence/ and docs/, so it also counts as visible contribution.

## 2. Ownership overview

| Member | Role | Job scope in one line | Owns (report) | Presents in video |
|---|---|---|---|---|
| 1. [Name] | Project manager and developer | M1 Authentication and Green Commuter Profile (FR1.1 to FR1.3); project management; UM unit liaison; pilot users | 1 Introduction, 6 Impact, 7 Team roles, final assembly | Intro, M1, closing |
| 2. [Name] | Lead developer / architect | Backend (Firestore schema, security rules and roles, Cloud Functions, push notifications); M3 booking and trip completion (FR3.1, FR3.2) | 3.2 Architecture, 3.3 Database, 4.1 Environment and conventions | Architecture, booking |
| 3. [Name] | UI/UX and developer | Design system, navigation, accessibility; M3 chat (FR3.3); trip ticket and trip-end CO2 screens; dashboard, leaderboard and badge UI | 3.1 UI design, figures and screenshots, formatting | UI/UX, chat, video editing |
| 4. [Name] | AI developer | M5 AI module: parser (FR5.1), grouping (FR5.2), CO2 service and dashboard data (FR5.3); AI evaluation; AI Usage Log | 2.1 User study analysis, 4.3 AI usage statement, 5.2 AI evaluation, Appendix D | AI features, results |
| 5. [Name] | Developer (matching and maps) | M2 ride matching (FR2.1 to FR2.3) including routes, residential zones, notifications and feed; maps and location data | 2.2 table rows for M2, 4.2 M2 code, 8 References | Matching demo |
| 6. [Name] | Developer and tester | M4 safety (FR4.1 to FR4.3: trip record, SOS, ratings); survey; tests, CI and NFR measurements | 2.3 NFRs, 5 Testing and validation, test evidence | Safety, NFR verification |

**Code review ring:** member n reviews member n+1's pull requests (1 reviews 2, ..., 6 reviews 1). Member 2 also approves changes to the backend and security rules.

## 3. Requirement ownership

| Requirement | Summary | Owner | Supports |
|---|---|---|---|
| FR1.1, FR1.2 | UM email only, email verification | Member 1 | |
| FR1.3 | Commuter profile, verification badge, Green Commuter Tier | Member 1 | Member 3 (badge UI), Member 4 (CO2 totals) |
| FR2.1 | Save frequent routes and residential zones, match notifications | Member 5 | Member 2 (push) |
| FR2.2 | Notify drivers with overlapping routes, with estimated CO2 | Member 5 | Member 2 (Cloud Function), Member 4 (CO2 estimate) |
| FR2.3 | Filterable feed with CO2 per offer | Member 5 | Member 3 (UI), Member 4 (CO2 estimate) |
| FR3.1 | Capped cost-share per passenger | Member 2 | Member 3 (UI) |
| FR3.2 | Reserve seat without double booking, ticket, trip-end passengers, distance and CO2 shown | Member 2 | Member 3 (UI), Member 4 (CO2), Member 6 (trip states) |
| FR3.3 | In-app chat | Member 3 | Member 2 (data) |
| FR4.1 | Trip record | Member 6 | Member 2 (trip states) |
| FR4.2 | SOS pre-filled message | Member 6 | Member 3 (UI) |
| FR4.3 | Ratings and trust rating | Member 6 | Member 1 (profile display) |
| FR5.1 | Natural-language request parser | Member 4 | Member 2 (Cloud Function), Member 3 (UI) |
| FR5.2 | Pickup grouping and departure windows | Member 4 | Member 5 (location data) |
| FR5.3 | Dashboard: personal CO2, community leaderboards, anonymised staff view | Member 4 (data) | Member 3 (UI), Member 2 (staff role and rules) |
| NFR1 | Performance (5 s notification, 3 s screens, 100 users) | Member 6 (tests) | Member 2 (backend) |
| NFR2 | Security and privacy | Member 2 (rules) | Member 6 (tests) |

## 4. Shared interfaces (agree these in Phase 2)

Several features depend on each other, so these contracts must be written down in docs/ by 11 Nov.

1. **Trip lifecycle** (defined by Member 6, implemented by Member 2): offered or requested, booked, active, completed or cancelled. At completion the confirmed passenger count and distance are saved.
2. **CO2 service** (Member 4): one function `estimateCo2(distanceKm, passengers, vehicleType, fuelCategory)` returning kg. It is used for offers and notifications (Member 5), the trip-end screen (Members 2 and 3) and the dashboard. A stub with fixed values must exist by 15 Nov so others are not blocked.
3. **impactStats data** (written by Member 4, read by Members 1 and 3): per-user cumulative CO2 and trips (for the Green Commuter Tier), and anonymised aggregates by faculty, college and week (for leaderboards and the staff view).
4. **Roles** (Member 2): passenger, driver, both, and a staff analyst role that can see aggregates only.
5. **Green Commuter Tier**: tier names and thresholds decided by Members 1 and 4 in Phase 2.

## 5. Phase plan and milestones

| Phase | Dates | Goal | Milestone |
|---|---|---|---|
| 1. Validation | 27 Oct to 8 Nov | Survey, Sustainability Centre email, target decided | Findings summary, problem statement |
| 2. Requirements and design | 2 to 11 Nov | FR/NFR final, wireframes, schema, architecture, interfaces | Design review on Wed 11 Nov |
| 3. Sprint 1: core | 9 to 22 Nov | Auth, schema, navigation shell, routes, post offers and requests, feed, CO2 stub | Demo on Sun 22 Nov |
| 4. Sprint 2: features and AI | 23 Nov to 6 Dec | Booking, chat, notifications, trip completion, SOS, ratings, AI, dashboard | Demo on Sun 6 Dec |
| 5. Integration and user testing | 7 to 13 Dec | End-to-end flows, 8 to 10 user tests, AI evaluation, NFR tests, prototype review by the Sustainability Centre | Test report on Sun 13 Dec |
| 6. Hardening | 14 to 20 Dec | Bug fixes, polish, release build | **Feature freeze Sun 20 Dec** |
| 7. Video | 21 Dec to 3 Jan | Script, record, edit | **Submit by 12pm Sun 3 Jan** |
| 8. Report | 4 to 10 Jan | Assemble, edit to limit, AI log, evidence | **Submit by 12pm Sun 10 Jan** |

Critical path: authentication (Member 1), the Firestore schema and rules (Member 2), and the CO2 stub (Member 4) must be usable by about 15 Nov, because every other module depends on them.

## 6. Job scope per member and phase

Each row states the job scope for that phase and what must be committed when it is done.

### Member 1: Project manager and developer (M1, project management, liaison)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation (to 8 Nov) | Confirm group registration. Email the lecturer to confirm SDG 13.3 and ask the cost-sharing question. Write the one-page concept summary and send the Sustainability Centre email by 4 Nov; follow up on 9 Nov; keep the tracker. Set up the AI Usage Log and commit conventions (each member sets git user.name and user.email). Paper sketch of the sign-up and verification flow | Sent-email screenshot and tracker in evidence/; AI_Usage_Log.md in the repo |
| 2 Requirements and design (to 11 Nov) | Write the problem statement and report Section 1 from the survey results. Finalise the FR/NFR list with Member 4. Define the Green Commuter Tier names and thresholds with Member 4. Write TASKS.md (one line per FR/NFR with owner and date). Run the design review on 11 Nov | TASKS.md; docs/requirements.md; Section 1 draft |
| 3 Sprint 1 (9 to 22 Nov) | Implement FR1.1 (UM email domain restriction) and FR1.2 (verification link, access blocked until verified), plus login, password reset and sign-out. Run sprint planning and standups. Start recruiting pilot users (target 20+) | Merged auth pull request; pilot user list (first names only) |
| 4 Sprint 2 (23 Nov to 6 Dec) | Implement FR1.3: profile fields (faculty or department, residential area, role, vehicle type, registration number, fuel-efficiency category), verification badge, and Green Commuter Tier computed from Member 4's impactStats. Show the trust rating from Member 6. Handle emergency contact settings. Track progress and remove blockers. Reply to the Sustainability Centre and send the thank-you | Merged profile and tier pull request; sprint review notes |
| 5 Integration and testing (7 to 13 Dec) | Organise 8 to 10 user test sessions with Member 3, including the question "did seeing CO2 saved change your willingness to carpool?". Collect the SUS form. Share the prototype with the Sustainability Centre and record the change made because of their feedback. Log bugs in BUGS.md | evidence/user-tests/; BUGS.md; stakeholder feedback note |
| 6 Hardening (14 to 20 Dec) | Triage bugs by priority. Check every FR/NFR is covered and has a result. Confirm the feature freeze on 20 Dec | Coverage checklist in docs/ |
| 7 Video (21 Dec to 3 Jan) | Write the storyboard and script covering every rubric part (overview, demo, design, technical, NFR, stakeholders, team, closing). Schedule recording. Present the intro, M1 and closing | docs/video-script.md |
| 8 Report (4 to 10 Jan) | Write Sections 1, 6 and 7. Assemble the final document and check word and page limits, formatting, references and the checklist. Attach the AI log and evidence. Submit both deliverables. Remind everyone to complete the peer evaluation | Final report submitted |

### Member 2: Lead developer / architect (backend, M3 booking and trip completion)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation | Create the GitHub repo (branch rules, evidence/ and docs/ folders). Decide the stack with the lecturer. Create the Firebase project. Review the survey form wording once | Repo and Firebase project ready |
| 2 Requirements and design | Draw the architecture diagram. Define Firestore collections and fields (users, savedRoutes, tripOffers, rideRequests, trips, messages, ratings, impactStats). Draft security rules and the roles (passenger, driver, both, staff analyst). Write coding conventions. Define the Cloud Function interfaces with Members 4 and 5 and the trip lifecycle with Member 6 | docs/architecture.md; docs/schema.md; docs/conventions.md |
| 3 Sprint 1 | Wire Firebase into the app. Build the repository layer and dependency injection. Publish security rules v1. Add the Cloud Function that triggers notifications. Support Members 1, 4 and 5 with data access | Merged backend pull requests; rules v1 |
| 4 Sprint 2 | Implement FR3.1 (cost-share per passenger with a configurable cap, no in-app payment). Implement FR3.2: seat reservation using a Firestore transaction so a seat cannot be double booked, trip ticket generation, and trip completion that saves the confirmed passenger count and distance and calls Member 4's CO2 function. Push notifications. Matching trigger with Member 5. Staff-analyst rules for the aggregate view | Merged booking and completion pull requests |
| 5 Integration and testing | Fix integration bugs. Add indexes and optimise queries. Finalise security rules. Handle error cases (offline, expired offers, cancelled trips) | Final rules; passing emulator tests with Member 6 |
| 6 Hardening | Load test the backend with Member 6 (100 concurrent users). Check quotas and costs. Produce a signed release build. Write the README setup guide | README; release build |
| 7 Video | Explain the architecture and data flow. Demo booking, seat locking and trip completion | Segment recorded |
| 8 Report | Write 3.2 (architecture, AI data flow), 3.3 (database design) and 4.1 (environment, conventions). Include the seat-locking transaction as the code segment | Sections 3.2, 3.3, 4.1 in the shared doc |

### Member 3: UI/UX and developer (design system, chat, trip and dashboard screens)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation | Draft the stakeholder map and personas. Help Member 1 design the one-page concept summary and dashboard sketch | docs/stakeholders.md; concept summary |
| 2 Requirements and design | Define the design system (colour scheme, typography, components, accessibility rules). Wireframes for all screens, including trip-end CO2 result, dashboard (personal, community and staff views), leaderboard, and Green Commuter Tier badge. Figma clickable prototype. Quick feedback from 3 to 5 students. Navigation graph | Figma link; docs/design-system.md |
| 3 Sprint 1 | Build the theme and reusable Compose components. Navigation shell, onboarding and home screens. English and Bahasa Melayu string resources. Loading, empty and error states | Merged UI foundation pull request |
| 4 Sprint 2 | Implement FR3.3 in-app chat (real-time). Trip ticket screen and trip-end CO2 result screen. Dashboard UI with Member 4: personal CO2 and insights, community leaderboards by faculty or college, staff aggregate view. Badge and tier UI with Member 1. Show CO2 on offer cards with Member 5. Accessibility pass (content descriptions, contrast, font scaling), dark mode | Merged chat, ticket and dashboard pull requests |
| 5 Integration and testing | Write the usability test script and tasks with Member 1. Measure time to book. Fix UI issues from testing | docs/usability-script.md; fixes merged |
| 6 Hardening | Pixel-level polish across screens. Capture the screenshot set for the report and video. App icon and splash screen | evidence/screenshots/ |
| 7 Video | Lead recording and editing (subtitles, audio check). Present UI/UX principles, chat and dashboard design | Final video file |
| 8 Report | Write 3.1 (UI principles, colour scheme, Figure 1). Prepare all figures. Format the Google Doc (Times New Roman 12 pt, double spacing, captions). Include the chat code segment | Section 3.1 and formatted document |

### Member 4: AI developer (M5, CO2 service, analysis, AI log)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation | Analyse the survey export (percentages, splits by resident or commuter and local or international, Likert means, top quotes). Read the SDG 13.3 text and note the argument. Prototype the parser prompt and collect 30 test sentences. Choose and cite a CO2 emission factor | evidence/survey-analysis.md; docs/emission-factor.md; parser prompt v0 |
| 2 Requirements and design | Design the AI features: JSON schema and prompt for the parser with a fallback to manual entry; grouping method (origin zone plus time window, clustering approach); written CO2 formula and assumptions; the `estimateCo2` interface; impactStats structure; Green Commuter Tier thresholds with Member 1; evaluation protocol and dataset (100+ sentences in English, Malay and mixed) | docs/ai-design.md; docs/co2-formula.md |
| 3 Sprint 1 | Deliver the CO2 service with unit tests, and the stub by 15 Nov. Build the parser Cloud Function with input validation (with Member 2). Get zone data from Member 5 | Merged CO2 module and parser function |
| 4 Sprint 2 | Implement FR5.1 in the request screen (user confirms the pre-filled form). FR5.2 grouping and departure-window recommendation. FR5.3 data: per-user totals and insights text, anonymised aggregates by faculty, college and week, leaderboard data, staff aggregate query. Seed-data generator for demo and evaluation | Merged AI and impactStats pull requests |
| 5 Integration and testing | Run the AI evaluation (field accuracy, failure cases, latency, cost). Tune the prompt. Test wrong and malicious input | evidence/ai-evaluation.md with tables |
| 6 Hardening | Guardrails (rate limits, output validation, prompt-injection checks). Finalise evaluation tables | Guardrails merged; final tables |
| 7 Video | Live demo of the assistant, grouping and CO2 dashboard. Present accuracy results and limitations | Segment recorded |
| 8 Report | Write 2.1 (user study findings), 4.3 (AI usage statement) and 5.2 (AI evaluation). Include the AI code segment. Merge all members' log entries into Appendix D using the official template | Sections 2.1, 4.3, 5.2 and Appendix D |

### Member 5: Developer, matching and maps (M2)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation | Review existing sapu groups and similar apps (no personal data). List key UM locations (colleges, faculties, gates) and the residential areas from the survey (Tiara Damansara, Seksyen 17, Pacific Star, Pantai Hillpark) with coordinates. Create the Android project skeleton with a map screen | docs/competitors.md; docs/locations.json; skeleton merged |
| 2 Requirements and design | Define the location and zone dataset. Write the matching rules (overlap means same origin zone, same destination, departure within +/- 15 minutes). Map UX decisions. Start the references list | docs/matching-rules.md; references.bib or list |
| 3 Sprint 1 | FR2.1: save and manage frequent routes and residential zones (add, edit, delete, map picker) and subscribe to match notifications. Create the flows for posting ride offers and ride requests. Basic feed listing | Merged routes and offers pull requests |
| 4 Sprint 2 | FR2.3: filterable feed (route proximity, departure time, destination) with pagination and estimated CO2 per offer, using Member 4's function. FR2.2: notify drivers with overlapping routes, with departure time, destination, available seats and estimated CO2, using Member 2's Cloud Function. Handle cancellation and expiry | Merged feed and notification pull requests |
| 5 Integration and testing | Query performance. Clean-up of expired offers. Edge cases (no-shows, duplicates). Help seed demo data | Fixes merged |
| 6 Hardening | Bug fixes. Restrict map API keys. Offline behaviour | Merged fixes |
| 7 Video | Demo the matching flow: save a route, post a request, driver notification with CO2 | Segment recorded |
| 8 Report | Write the module M2 rows in Section 2.2 and the M2 code segment. Compile and format all references in one academic style (APA 7) | Section 2.2 rows; Section 8 |

### Member 6: Developer and tester (M4 safety, survey, QA)

| Phase | Job scope | Done when (commit or evidence) |
|---|---|---|
| 1 Validation | Own the whole survey: build the Google Form from Survey_Interview_Kit.md, pilot it with 3 to 5 friends, send the link to friends, one reminder after 3 days, close on 6 Nov, export responses. Research PDPA and safety points and list assumptions to verify. Draft the test plan skeleton | evidence/survey-raw.csv (no personal data); docs/assumptions.md |
| 2 Requirements and design | Test strategy and acceptance criteria per FR (Given/When/Then). NFR measurement method (script design for 5 s notification, 3 s load, 100 users). Define the trip lifecycle states with Member 2. SOS flow, trip record fields and retention rule. Privacy notice text | docs/test-plan.md; docs/trip-lifecycle.md |
| 3 Sprint 1 | FR4.2: SOS (pre-filled SMS or WhatsApp message with trip details and location; emergency contacts). Set up unit tests, UI tests and CI (lint and build on pull requests). Test the security rules with the Firebase emulator | Merged SOS pull request; CI passing |
| 4 Sprint 2 | FR4.1: trip record with start and end states, aligned with Member 2's trip completion. FR4.3: ratings and reviews feeding the trust rating (shown by Member 1). UI tests for the booking flow. Maintain the bug list | Merged trip record and ratings pull requests |
| 5 Integration and testing | Full regression. NFR1 load test (100 concurrent users, notification latency, screen load times). NFR2 security tests (unverified access, cross-user data access). Results table | evidence/test-results.md |
| 6 Hardening | Retest fixes. Device and OS matrix (3 devices). Accessibility scanner check | Updated test results |
| 7 Video | Demo SOS and the trip record. Present the NFR measurements with real numbers | Segment recorded |
| 8 Report | Write 2.3 (NFRs and justification) and Section 5 (strategy, Table 4, user testing summary with Member 1). Include the SOS code segment. Collect test evidence and the Git log and contribution graph screenshots for the appendix | Sections 2.3, 5 and Appendix C |

## 7. Working rules

- **Meetings:** two 20-minute standups per week (Mon and Thu, adjust to timetables) and a 45-minute sprint review on the Sunday that ends each sprint. Commit short meeting notes to docs/meetings/.
- **Git:** `main` is always buildable. Work on `feature/<fr-id>-<short-name>` branches; open a pull request; one reviewer from the ring approves before merge. Commit messages: `FR2.3: add destination filter`.
- **Definition of Done for a task in TASKS.md:** code merged, at least one test or a recorded manual test, screenshots saved to evidence/, the report paragraph drafted in the shared doc, and any AI use logged.
- **Report drafting:** each member drafts their report section at the end of the sprint in which the feature ships, so Phase 8 is editing, not writing. Aim for the word budgets in Report_Layout.md.
- **Evidence for peer evaluation:** commits, pull requests, TASKS.md history and the hour log. Commit small and often under your own identity. One huge commit at the end, or commits made on someone else's account, will not show your real contribution. If someone is blocked or behind, say so at standup and rebalance early.
- **AI use:** log it the same day in AI_Usage_Log.md. Keep the design decisions, algorithms and analysis your own.
- **Scope rule:** M1 to M4 and the core of M5 (parser, per-trip CO2, personal totals) are mandatory. If time is short, simplify in this order: leaderboards, the staff aggregate view, then advanced clustering. Keep every FR present in a simpler form so the three-FRs-per-module count still holds.
