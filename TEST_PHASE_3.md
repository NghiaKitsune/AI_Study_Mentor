# Phase 3 — Test Results: Dashboard Milo Insight (AI-generated)

**Date:** 2026-07-16
**Branch:** `feature/api-expansion`
**Environment:** Claude Code remote sandbox (Linux container, no Android SDK, no emulator)

---

## Summary

| Metric | Result |
|--------|--------|
| Code implementation | ✅ Complete per PLANNING.md Phase 3 spec |
| `assembleDebug` | ⚠️ NOT RUN — see "Build limitation" below |
| Emulator smoke test | ⚠️ NOT RUN — no Android SDK / emulator in this sandbox |
| Static review | ✅ Brace-balance + XML well-formedness checked on all edited files |

---

## Build limitation (read before assuming a regression)

This session ran in a Linux remote sandbox that has **no Android SDK and no
network access to `dl.google.com` / `services.gradle.org`** (outbound proxy
returns HTTP 403 for both). `./gradlew assembleDebug` cannot even download
the Gradle 9.0 distribution here, let alone resolve the Android Gradle
Plugin or compile against `android.jar`. This is an environment constraint,
not a code issue — all prior sessions' `assembleDebug` runs happened on the
user's Windows machine per `CLAUDE.md`'s Build & Run section.

**Action needed from the user:** run the usual local build to confirm:
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```
then install on the emulator and open `ProfileActivity → Dashboard` to see
the insight card.

In lieu of a real build, this session verified the changes by:
- Reading every touched file end-to-end after editing.
- Counting `{`/`}` per file to catch unbalanced braces (all matched).
- Parsing `activity_dashboard.xml` and `strings.xml` with `xml.dom.minidom` (both well-formed).
- Cross-checking method/field names against call sites (`GroqAiService.InsightCallback`, `Session.hasFreshInsight/cachedInsight/saveInsight`, `R.id.text_milo_insight`, `R.string.dashboard_insight_loading/fallback`).

---

## Files Changed

| File | Thay đổi |
|------|---------|
| `api/GroqAiService.java` | THÊM — `quickInsight(String statsSummary, InsightCallback cb)` + nested `InsightCallback` interface; reuses the existing `http`/`gson`/`main` fields. Plain-text completion (no `response_format: json_object`) since the model only needs to return 1-2 sentences, not structured JSON. |
| `util/Session.java` | THÊM — `KEY_CACHED_INSIGHT`, `KEY_INSIGHT_DATE`; methods `cachedInsight()`, `hasFreshInsight()` (true once/day), `saveInsight()`. |
| `ui/DashboardActivity.java` | THÊM — `bindMiloInsight()` called from `onCreate()` after `bindSubjects()`; shows cached insight if generated today, else calls `GroqAiService.quickInsight()` with a stats summary built from `questionDao().count()`, `Session.streak/xp/levelTitle/bestQuizPct`, and per-subject counts. Falls back to `R.string.dashboard_insight_fallback` on error; guards against `isFinishing()/isDestroyed()` before touching views in the async callback. |
| `res/layout/activity_dashboard.xml` | SỬA — gave the Milo-insight `TextView` `android:id="@+id/text_milo_insight"`, replaced hardcoded "algebraic equations…" text with `@string/dashboard_insight_loading`. |
| `res/values/strings.xml` | THÊM — `dashboard_insight_loading`, `dashboard_insight_fallback`. |

---

## GroqAiService.quickInsight() design

```
POST https://api.groq.com/openai/v1/chat/completions
Model: llama-3.3-70b-versatile
temperature: 0.7
(no response_format — plain text reply)

system: "You are Milo... write exactly 1-2 short, warm, encouraging
         sentences (max 220 characters total) that reference a real
         pattern in the stats and suggest one concrete next step.
         Respond in ENGLISH with plain text only — no JSON, no
         markdown, no quotes."
user:   <stats summary string built in DashboardActivity>
```

Example `statsSummary` sent as the user message:
```
Student stats — total questions asked: 12, current streak: 3 day(s),
XP: 750 (Beginner level), best quiz score: 80%, subject breakdown:
math=5, science=3, code=2, history=2.
```

Response parsing reuses the same `choices[0].message.content` path as
`GroqQuizService`/`GroqTabbedService`, just returns the trimmed string
directly instead of running it through Gson POJO parsing.

---

## Caching behaviour

- `Session.hasFreshInsight()` compares `KEY_INSIGHT_DATE` (yyyy-MM-dd) to
  today; returns true only if a non-empty cached string exists for today.
- On a fresh day, `DashboardActivity` shows `dashboard_insight_loading`
  ("Milo is looking at your progress…") immediately, then replaces it with
  the AI response (or the fallback string on error) once the network call
  returns.
- This matches the plan's requirement: "cache 1 lần/ngày" — the Groq API is
  called at most once per calendar day per device, regardless of how many
  times Dashboard is opened that day.

---

## Manual verification checklist for the user (next local session)

1. `./gradlew.bat assembleDebug`, install, open the app, sign in / seed a
   question via Chat so `questionDao().count() > 0`.
2. Navigate to `ProfileActivity → Dashboard`.
3. Confirm the insight card shows "Milo is looking at your progress…"
   briefly, then a real sentence referencing your actual stats (not the
   old hardcoded "algebraic equations" text).
4. Check logcat tag `GroqAI` for the HTTP request/response.
5. Kill and reopen Dashboard the same day — insight should appear
   instantly (from `SharedPreferences`), no new network call.
6. Change device date forward one day (or wait) and reopen — a new
   network call should fire and the text should refresh.
7. Turn off network / use an invalid key temporarily — confirm the card
   shows `dashboard_insight_fallback` instead of crashing or staying
   blank.

---

## Known follow-up (not in scope for Phase 3)

The two chips below the insight text ("Try calculus" / "Maybe later")
remain hardcoded — PLANNING.md Phase 3 only calls for the insight
*sentence* to become AI-generated, not the chip actions. Not touched here.
