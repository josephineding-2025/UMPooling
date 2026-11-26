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

Requirements: Android Studio, JDK 21, the Android SDK, and a device or emulator running Android 7.0 (API 24) or higher.

1. Clone the repo and set your own Git identity:
   ```
   git clone https://github.com/josephineding-2025/UMPooling.git
   git config user.name "Your Name"
   git config user.email "your-email@siswa.um.edu.my"
   ```
2. Open the folder in Android Studio and let Gradle sync. `local.properties` is created for you and is never committed.
3. Add the project keys to `local.properties` (ask the team's private chat for the values):
   ```
   SUPABASE_URL=https://<project>.supabase.co
   SUPABASE_ANON_KEY=<the publishable key, starts with sb_publishable_>
   ```
   Use only the publishable key. The secret / service-role key must never go in the app or the repository.
4. Run the `app` configuration on an emulator or device. The app builds without `google-services.json`; that file is only needed for push notifications, is kept private, and goes in `app/`.
5. Before opening a pull request, run `bash gradlew :app:assembleDebug :app:testDebugUnitTest`. It must pass.

## Backend (Supabase)

The backend is a Supabase project (Auth, Postgres with Row Level Security, an Edge Function for push). Read these before you touch data or backend code:

| Read this | When |
|---|---|
| [docs/architecture.md](docs/architecture.md) | How the app, Supabase, the Edge Function and Firebase push fit together |
| [docs/schema.md](docs/schema.md) | Table and column names, enum labels (case matters), functions, who can read or write what, the trip lifecycle |
| [docs/conventions.md](docs/conventions.md) | Naming, secrets, tests and how to write database changes |
| [supabase/README.md](supabase/README.md) | The migrations, the Edge Function, how to apply and test them |

Rules in short: change the database only with a new file in `supabase/migrations/` reviewed by Member 2, send enum labels exactly as written in `docs/schema.md`, and never commit secrets. Trip start, confirm, complete and cancel are database functions; the test for them is `supabase/checks/01_trip_lifecycle_test.sql`.

## Module 5: AI Commuting Assistant & Climate Analytics (Sprint 1 & 2 Updates) (Just for info purpose rmb delete aft tat)

This branch (`JasmineJY_Sprint2`) contains the completed Sprint 1 and Sprint 2 deliverables for **Module 5**, developed by **Jasmine Chin Jia Yee (Member 4 - AI Developer)**.

### What Has Been Implemented & Updated

#### 1. FR5.1: Natural-Language Ride Request Parser
- **Supabase Edge Function (`supabase/functions/parse-ride-request`):**
  - Powered by OpenRouter calling `google/gemini-flash-1.5` with temperature `0.0` and strict JSON schema enforcement (`origin`, `destination`, `departure_date`, `departure_time`, `seats`, `missing_fields`, `confidence`).
- **Pre-filled Form Confirmation Flow:**
  - Integrated into `VoiceRequestScreen.kt` and `UMPoolingApp.kt`.
  - Commuters can speak or type casual requests in English, Malay, or Manglish (e.g., *"Need a ride from Tiara to FSKTM tomorrow at 8:30am"*).
  - The parsed fields automatically pre-fill the `FindRideScreen` search form.
  - **Human-in-the-Loop Guarantee:** The system *never* creates or books rides automatically; the commuter explicitly reviews, edits missing details, and confirms before searching.
- **Offline Fallback Heuristics (`RideParserRepository.kt`):**
  - If the device is offline or the Edge Function is unreachable, a local regex-based heuristic extractor recognizes UM landmarks (KK12, FSKTM, Central Library, etc.) as an instant fallback.

#### 2. FR5.2: Commuter Grouping & Departure Window Optimization
- **Spatial Clustering Engine (`CommuterGroupingService.kt`):**
  - Implements DBSCAN clustering on pickup coordinates using the Haversine metric ($\epsilon = 300\text{ m}$, `min_samples = 2`).
- **Multi-Factor Compatibility Pruning:**
  - Prunes clusters based on destination proximity ($\le 300\text{ m}$), departure time overlap ($\pm 15\text{ min}$ window), and vehicle capacity limits ($\le 4$ passengers per vehicle, auto-splitting excess passengers).
- **Optimal Departure Window Recommendation:**
  - Recommends a unified 15-minute departure window (`windowStart` to `windowEnd`) and recognized campus pickup stops for grouped commuters.
  - Dynamically wired into `RequestFutureRideScreen.kt` to surface nearby co-riders heading the same way and suggest optimal pickup stops.

#### 3. FR5.3: Climate Impact Analytics, Green Commuter Tiers & Privacy Guarantees
- **Calculation Service (`Co2CalculationService.kt`):**
  - **Emission Factor ($E$):** `0.20 kg CO2e/km` cited from the Malaysian MITI Sustainability Starter Kit (referencing US EPA).
  - **Road Circuity Factor:** Uses Haversine distance multiplied by `1.3×` road circuity (`estimateRoadDistanceKm`), eliminating external Google Routes API dependencies.
  - **Trip Formula:** Total Trip $\text{CO}_2\text{e} = N \times D \times E$; Personal Share $= (N \times D \times E) / (N + 1)$.
  - **Anti-Gaming Rules:** Only completed trips with dual confirmation, $\ge 1$ passenger, $\ge 1.0\text{ km}$, and at most 2 counted trips per day.
- **Green Commuter Tiers:**
  - Levels 0–5 (Seedling, Sprout, Sapling, Tree, Forest Guardian). Thresholds are cumulative and non-downgrading.
  - Integrated dynamically into `GreenCommuterTierScreen.kt`.
- **Impact Repository & Non-Individual Leaderboards (`ImpactRepository.kt`):**
  - Generates personal impact metrics, daily/weekly charts, and personalized insights text for `SustainabilityDashboardScreen.kt`.
  - **Privacy Guarantee:** Leaderboards rank strictly by **Faculty** and **Residential College**; individual users are *never* ranked to preserve peer privacy.
- **Staff Aggregate Queries & $k \ge 5$ Anonymisation:**
  - Migration `supabase/migrations/20261126000009_impact_stats_and_aggregates.sql` provides stored procedure `get_staff_anonymised_aggregates(k_min INT DEFAULT 5)`.
  - Any cohort with fewer than 5 active commuters has metrics masked as `NULL` to guarantee differential privacy.

#### 4. Seed Data Generator for Demo & Evaluation
- `SeedDataGenerator.kt` and `supabase/seed_impact_data.sql` provide 50 synthetic, realistic UM commuting requests and cohort impact records across faculties and colleges for immediate demonstration and automated evaluation.

#### 5. Verification & Test Suite
- Full test suite passes with **117 tests (0 failures)**:
  - `Co2CalculationServiceTest` (15 tests)
  - `RideParserRepositoryTest` (4 tests)
  - `CommuterGroupingServiceTest` (6 tests)
  - `ImpactRepositoryTest` (4 tests)
  - `SeedDataGeneratorTest` (3 tests)

---

### OpenRouter & API Key Setup (for FR5.1 NLP Ride Parser)

To run the AI features while keeping student costs zero and API keys completely secure:

#### 1. What Key Is Needed
- **`OPENROUTER_API_KEY`**: The **only** external API key required. It is used by the `parse-ride-request` Supabase Edge Function to access OpenRouter's free/low-cost model tiers (`google/gemini-flash-1.5`).
- **No Google Routes API Key Needed:** Distances are computed locally via Haversine with the approved $1.3\times$ road circuity factor.
- **No Keys in Android Code:** The Android app never stores or accesses the OpenRouter key directly. It calls the Supabase Edge Function using standard user authentication headers.

#### 2. Where to Set the OpenRouter Key
Set the key in your Supabase backend environment:

- **Via Supabase Web Dashboard (Recommended):**
  1. Go to your Supabase Project $\rightarrow$ **Project Settings** (gear icon).
  2. Select **Edge Functions** $\rightarrow$ **Secrets**.
  3. Click **Add new secret**:
     - **Name:** `OPENROUTER_API_KEY`
     - **Value:** `sk-or-v1-xxxxxxxxxxxxxxxxxxxx`
  4. Save.

- **Via Supabase CLI:**
  ```bash
  supabase secrets set OPENROUTER_API_KEY="sk-or-v1-xxxxxxxxxxxxxxxxxxxx"
  supabase functions deploy parse-ride-request
  ```

If the key is not configured or the user is offline, the app automatically falls back to local heuristic extraction (`RideParserRepository`) so that testing and commuting are never blocked.

## Repository structure

```
UMPooling/
  app/                  Android app module (Kotlin, Jetpack Compose)
  supabase/
    migrations/         Database changes in order
    functions/          Edge Functions (push notifications)
    checks/             Read-only queries and tests for the live project
  gradle/               Gradle wrapper and version catalog (libs.versions.toml)
  docs/                 Team documents (architecture, schema, conventions, design, requirements)
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
