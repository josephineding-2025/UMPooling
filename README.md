# UMPooling

A UM-only campus carpooling app for Universiti Malaya students and staff. Users with verified UM email accounts share rides, see the CO2 they save, and help UM raise climate awareness and plan lower-carbon commuting.

- **Course:** WIA2007 Mobile Application Development, Group Project 2026/2027 Semester 1 (OCC 8)
- **Group:** Group 9 (FOPGang)
- **SDG:** SDG 13 Climate Action, Target 13.3 (education, awareness-raising and capacity on climate action)
- **Platform:** Android (Kotlin, Jetpack Compose)

## Team

| Member | Role | Main area |
|---|---|---|
| Josephine Ding Jie Yu | Project manager and developer | M1 Authentication and Green Commuter Profile |
| Ng Geok Liu | Lead developer / architect | Backend, M3 booking and trip completion |
| Ng Yue Qhi | UI/UX and developer | Design system, chat, dashboard screens |
| Jasmine Chin Jia Yee | AI developer | M5 AI assistant and CO2 analytics |
| Ng Shao Ern | Developer | M2 ride matching and maps |
| Jasmine Chin Ying Hui | Developer and tester | M4 safety, survey, tests |

Full task split by phase: [docs/Task_Division.md](docs/Task_Division.md).

## Modules

| Module | Summary |
|---|---|
| M1 Authentication and Green Commuter Profile | UM email only, email verification, commuter profile, Green Commuter Tier |
| M2 Ride Matching and Location-Based Dispatch | Saved routes and zones, driver notifications, filterable feed with CO2 per offer |
| M3 Booking, Cost-Sharing and Coordination | Capped cost-share, seat reservation, trip-end CO2, in-app chat |
| M4 Commuter Safety and Emergency | Trip record, SOS message, ratings |
| M5 AI Commuting Assistant and Climate Impact Analytics | Natural-language requests, pickup grouping, CO2 dashboard |

Requirements and the SDG 13.3 mapping: [docs/UMPooling.md](docs/UMPooling.md).

## Getting started

Requirements: Android Studio, the Android SDK, and a device or emulator running Android 7.0 (API 24) or higher.

1. Clone the repo:
   ```
   git clone https://github.com/josephineding-2025/UMPooling.git
   ```
2. Set your own Git identity :
   ```
   git config user.name "Your Name"
   git config user.email "your-email@siswa.um.edu.my"
   ```
3. Open the folder in Android Studio and let Gradle sync. `local.properties` is created for you and is not committed.
4. Run the `app` configuration on an emulator or device.
5. Firebase and API keys : `google-services.json` and any API keys are NOT committed. Get them from the team's private chat and place `google-services.json` in `app/`.

## Repository structure

```
UMPooling/
  app/                  Android app module (Kotlin, Jetpack Compose)
  gradle/               Gradle wrapper and version catalog (libs.versions.toml)
  docs/                 Team documents and ideas 
  evidence/             Survey exports, UM unit emails, screenshots, test results
  .github/              Pull request template
  AI_Usage_Log.md       AI usage log
  CONTRIBUTING.md       Git workflow and code conventions
  TASKS.md              Requirement checklist with owners
  BUGS.md               Bug list
```

## Where to put things

- Ideas, notes, drafts, meeting notes: **docs/** only. Do not edit code for an idea until it is agreed.
- Code: only on your own feature branch, through a pull request. See [CONTRIBUTING.md](CONTRIBUTING.md).
- Survey data, screenshots, emails and test results: **evidence/** (remove personal data first).
- Any AI help: log it in [AI_Usage_Log.md](AI_Usage_Log.md) the same day.
