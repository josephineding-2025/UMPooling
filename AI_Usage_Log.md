# AI Usage Log - UMPooling (WIA2007 Group Project)

This log is a mandatory appendix of the final report (Appendix D). Keep it in the repository root so it is also visible as a README-style file. If the official Appendix 2(b) template has different columns, copy these entries into that template at the end.

## Rules for this team

1. **Log at the time, not at the end.** Add an entry the same day you use AI for anything that ends up in the project.
2. **One entry per meaningful use.** Brainstorming, code generation, debugging, writing help, data analysis, translations. Trivial spell-checks are not needed.
3. **Paste the real prompt**, not a summary. Keep the AI's key answer or a link to the chat export.
4. **Say what you did with the output** (used as is, modified, rejected) and **how you verified it** (ran it, wrote a test, checked a source, compared with documentation). If nobody has verified it yet, write "NOT YET VERIFIED".
5. **Never leave the last two columns generic.** "Checked and works" is not enough. Say what you ran, what failed and what you changed.
6. **Each member logs their own use.** Put your name in the Member column.
7. **Design and analysis stay human.** The matching logic, data model, evaluation design and conclusions must be your own thinking. Say so in the notes when relevant.
8. **Do not paste secrets or personal data** (API keys, student data) into AI tools.

## Log

| No. | Date | Member | AI tool | Prompt / query (verbatim) | Output received | How the output was used (used / modified / rejected) | How it was tested or verified, and what was changed |
|---|---|---|---|---|---|---|---|
| 1 | | | | | | | |
| 2 | 2026-11-14 | Account: GeokLiu (member to confirm) | OpenAI Codex | I previously worked on Database branch but my members in main adn uiui branch said I do differnt project striucture as main branch can you help me check and change. And aslo what is my Android package name based on uiux and main branch | Branch comparison and database integration into main structure; package com.fopgang.umpooling. | Ported database layer on a new branch from main; retained Compose and configured one Supabase client. | Static model/repository comparison, package and unchanged UI checks, TOML/XML parsing and git diff --check; build/unit tests blocked by Gradle download connection refusal. Android Studio and live database checks NOT YET VERIFIED; see docs/database-integration.md. |

## Notes on authorship (fill in honestly)

- **Origin of the idea:** [Describe who in the team proposed UMPooling and why, e.g. from personal experience with "sapu" rides. This shows the idea and problem choice are the team's own.]
- **Design decisions made by the team:** [e.g. cost-share cap, no in-app payment, no student ID upload, which features are mandatory vs stretch.]
- **What AI suggested that we rejected, and why:** [e.g. rain-damage reporting idea; fine-tuned on-device model.]

## Verification checklist before submission

- [ ] Every AI-assisted artefact in the repository (code, text, diagrams) has a matching log entry.
- [ ] Prompts are verbatim and the verification column is specific.
- [ ] Each member has reviewed their own entries.
- [ ] The log has been copied into Appendix D using the official Appendix 2(b) template.
- [ ] The AI usage statement in Section 4.3 of the report matches this log.
