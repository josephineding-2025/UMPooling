# Contributing to UMPooling

Progress and contribution evidence for this project come from the git log, so follow these rules.

## 1. Identity

Use your own name and UM email in Git. 

```
git config user.name "Your Name"
git config user.email "your-email@siswa.um.edu.my"
```

## 2. Branches

- `main` is always buildable. Never commit directly to `main`.
- Branch from `main` as `feature/<requirement-id>-<short-name>`, for example `feature/FR2.3-destination-filter`.
- Other branch types: `fix/<short-name>`, `docs/<short-name>`.

## 3. Commits

- Commit small and often.
- Message format: `<requirement id>: <what changed>`, for example `FR2.3: add destination filter`.
- Use `docs:` for documentation, `fix:` for bug fixes, `chore:` for build or setup.
- Do not commit secrets (Supabase service-role key, API keys), `google-services.json`, `local.properties`, keystores or personal data.

## 4. Pull requests

1. Push your branch and open a pull request into `main`.
2. Fill in the pull request template.
3. One reviewer approves before merge. 
4. Merge with "Squash and merge" or "Rebase and merge" and delete the branch.

## 5. Definition of Done

A task is done when:
- the code is merged and the app still builds,
- there is at least one test or a recorded manual test,
- screenshots are saved in `evidence/`,
- the report paragraph is drafted in the shared document,
- any AI use is logged in `AI_Usage_Log.md`,
- the line in `TASKS.md` is ticked in the commit that finishes it.

## 6. Code conventions

- Language: Kotlin. UI: Jetpack Compose. Architecture: MVVM (UI, ViewModel, repository).
- Package: `com.fopgang.umpooling`, with sub-packages per feature (for example `auth`, `matching`, `booking`, `safety`, `ai`, `ui.theme`).
- Follow the official Kotlin style (`kotlin.code.style=official`). Format with Android Studio (Code > Reformat Code) before committing.
- Names: classes `PascalCase`, functions and variables `camelCase`, constants `UPPER_SNAKE_CASE`, composables `PascalCase`.
- Strings go in `res/values/strings.xml` (English) and `res/values-ms/strings.xml` (Bahasa Melayu), never hard-coded in composables.
- Keep functions short and comment the reason, not the obvious.

## 7. Docs and ideas

Ideas, notes and drafts go in `docs/` only. See [docs/README.md](docs/README.md).

## 8. AI usage

Log every meaningful AI use the same day in [AI_Usage_Log.md](AI_Usage_Log.md): tool, exact prompt, how you used the output, and how you tested it. The design, algorithms and analysis must be your own.
