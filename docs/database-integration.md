# Database branch integration

## Comparison

`main` and `uiux` have identical Android source and build files at the time of
this migration. Both declare `com.fopgang.umpooling` as the Gradle namespace
and application ID and use Jetpack Compose. The team's package convention is
also recorded in CONTRIBUTING.md.

`Database` was initialized separately: it has no common ancestor with `main`.
It uses `com.example.umpooling`, AppCompat/XML layouts, AGP 8.7.2 and SDK 35,
whereas `main` uses AGP 9.0.1 and compile SDK 36.1 / target SDK 36. It also
tracks local IDE files and Kotlin error logs.

This branch starts from `main` and ports the database layer from `Database`.
The original branch is preserved as a reference. Use this integration branch
for future database work; do not merge the unrelated Database history into main.

## Resulting source layout

```text
app/src/main/java/com/fopgang/umpooling/
  MainActivity.kt                  # existing team Compose activity
  SupabaseClientProvider.kt        # one shared client, created on first use
  data/
    Models.kt                     # original serialized table models
    repository/
      AuthRepository.kt           # original auth operations
      RideRepository.kt           # original ride operations
  ui/theme/                       # existing team Compose theme
```

Database models and repository behavior are retained. Their package/imports
now use the team's package and the shared client. The duplicate SupabaseProvider
previously declared inside the Database activity is replaced by
SupabaseClientProvider. The repositories remain ready for ViewModels to call;
this migration does not wire database operations into the starter Compose UI.

The main branch's SDK, wrapper, resources, tests, and Compose activity are
retained. Supabase dependencies, serialization, BuildConfig generation,
Internet permission, and Java API desugaring for API 24–25 are added. The
serialization compiler plugin matches AGP 9.0.1's built-in Kotlin 2.2.10;
the older standalone kotlin-android plugin is not carried over.

## Local setup

Keep Android Studio's `sdk.dir` in your ignored root `local.properties`. Add:

```properties
SUPABASE_URL=https://YOUR_PROJECT.supabase.co
SUPABASE_ANON_KEY=YOUR_PUBLISHABLE_OR_ANON_KEY
```

Use the client publishable/anon key, never a service-role key. Configure table
access through Supabase Row Level Security. These values are packaged into the
Android app and must be appropriate for a public client. No actual credentials
are included in this migration.

Gradle can sync with empty values, and the starter UI does not create the client.
Calling a repository without configuration returns a failure with a setup
message. After editing local.properties, rebuild the app.

## Verification

Static comparison confirms the model fields and repository operations match
Database after substituting packages/client names. MainActivity, Compose theme,
resources, and existing tests are unchanged from main. The version catalog parses,
manifest XML parses, and `git diff --check` passes.

Build and unit tests were attempted with:

```sh
bash gradlew :app:assembleDebug :app:testDebugUnitTest
```

They could not run in the migration environment: downloading Gradle 9.2.1 failed
with a connection-refused error. Compilation and live Supabase access remain
unverified. Before merging, a team member should:

1. Open this branch in Android Studio with JDK 21 and Android SDK 36.1 installed.
2. Add the local Supabase values and sync Gradle.
3. Run the command above, then launch the app and confirm the Compose screen opens.
4. Exercise sign-in, sign-up, and trip/request operations against a test Supabase
   project with the expected tables and policies. The starter UI does not yet
   expose these operations, so use the team's ViewModel integration or a test harness.

The original database logic is preserved, including its existing validation and
schema assumptions; this change does not verify backend tables or policies.
