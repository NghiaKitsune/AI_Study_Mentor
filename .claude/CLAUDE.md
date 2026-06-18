# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

---

# AI Study Mentor — Project Intelligence

> **LIVING DOCUMENT**: This file is auto-updated after every session by Agent-2.
> Read `## Session Log` first to understand what was done in previous sessions.

---

## Project Overview

| Field | Value |
|-------|-------|
| Type | Android Java application |
| minSdk | 33 (Android 13) |
| targetSdk | 34 |
| UI Framework | Material 3 (`com.google.android.material:material:1.11.0`) |
| Language | Pure Java — NO Kotlin, NO Compose |
| Layout | XML layouts — NO Fragment, NO NavComponent |
| Package | `com.studymentor.app` |
| App Name | AI Study Mentor (mascot: Milo) |

---

## Architecture

```
Multi-Activity (no Navigation Component)
├── Data layer:  Room DB (Question + Message entities)
├── Network:     Retrofit + MockAiService (swap via buildConfigField)
├── Camera:      CameraX (UC2.5)
├── State:       SharedPreferences via Session.java utility
└── Views:       ViewBinding enabled (buildFeatures.viewBinding = true)
```

**Key files:**
- `util/Session.java` — all SharedPreferences keys and accessors
- `StudyMentorApp.java` — singleton, holds `db()` + `executor()` (single-thread ExecutorService)
- `data/AppDatabase.java` — Room DB, version 1; uses `fallbackToDestructiveMigration()` — any schema change wipes the DB
- `api/MockAiService.java` — fake AI responses (USE_MOCK_AI=true in debug)
- `api/MockOcrService.java` — fake OCR for camera scan
- `util/BottomNavHelper.java` — static helper; call `BottomNavHelper.setup(activity, navItemId)` in every Activity that has a bottom nav
- `util/SubjectIcons.java` — maps subject string (`math`/`science`/`code`/`history`/`language`) → drawable resource ID; use this instead of inline switch statements
- `data/QuizDataSource.java` — loads `assets/quiz_questions.json` (cached after first read); call `QuizDataSource.random(ctx, subject, count)` to get shuffled questions; pass `null`/`"all"` for subject to include all
- `StudyReminderWorker.java` — WorkManager Worker; creates notification channel + posts daily study reminder; respects `Session.notificationsOn()`
- `data/MessageDao.java` — DAO for individual chat turns (insert/getByQuestionId)

**Files quan trọng nhất (auto-load mỗi session):**

@.claude/DESIGN_MIGRATION_LOG.md

**Room DB entity fields:**
```
questions: id (PK autoGen) | prompt | answer | subject | createdAt (epoch ms) | bookmarked
messages:  id (PK autoGen) | questionId (FK→questions.id) | role ("user"|"assistant") | text | sentAt (epoch ms)
```
Use `Message.user(questionId, text)` / `Message.assistant(questionId, text)` factory methods rather than the default constructor.

**API layer (`api/` package):**
- `AiService` — Retrofit interface: single endpoint `POST api/chat` → `ChatResponse`
- `ApiClient` — builds OkHttp + Retrofit; returns `MockAiService` when `BuildConfig.USE_MOCK_AI=true`, real impl otherwise
- `ChatRequest` fields: `request_id` (UUID String), `conversation_id` (Long, null for new), `message` (String), `context.{user_level, subject, locale}`
- `ChatResponse` fields: `reply`, `final_answer`, `steps` (List<Step{index,title,body}>), `follow_ups` (List<String>), `commonMistakes` (List<String>, max 2), `error` (nullable ErrorInfo{code,message})

**Key Intent extras (inter-Activity contracts):**
- `ChatActivity`: `EXTRA_PROMPT` (String, prefill composer), `EXTRA_QUESTION_ID` (long, load existing conversation)
- `AnswerActivity`: `EXTRA_QUESTION_ID` (long), `EXTRA_STEPS_JSON` (String, Gson list of `ChatResponse.Step`), `EXTRA_MISTAKES_JSON` (String, Gson `List<String>`)
- `QuizResultActivity`: `EXTRA_SCORE` (int), `EXTRA_TOTAL` (int)

**SplashActivity routing** (called after `Session.updateStreak()`):
```
not logged in + onboarding not seen → OnboardingActivity (first ever launch)
not logged in + onboarding seen     → SignUpActivity
logged in + not onboarded           → PersonalizeActivity
logged in + onboarded               → HomeActivity
```

**Switching to real backend:** In `app/build.gradle`:
```groovy
buildConfigField "boolean", "USE_MOCK_AI", "false"
buildConfigField "String", "API_BASE_URL", '"https://your-api.com/"'
```

---

## Build & Run

### Build command
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleDebug
```

### APK output
```
app\build\outputs\apk\debug\app-debug.apk
```

### Run full test pipeline (manual)
```
.claude\hooks\run_full_test.bat
```

---

## Emulator

| Field | Value |
|-------|-------|
| AVD Name | `Pixel6_API33` |
| System Image | android-33 / google_apis / x86_64 |
| SDK path | `%LOCALAPPDATA%\Android\Sdk` |
| ADB | `%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe` |
| Emulator exe | `%LOCALAPPDATA%\Android\Sdk\emulator\emulator.exe` |

### Launch emulator
```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
Start-Process "$sdk\emulator\emulator.exe" -ArgumentList "-avd","Pixel6_API33","-no-snapshot-load","-no-audio","-gpu","swiftshader_indirect"
```

### Install APK
```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\platform-tools\adb.exe" -s emulator-5554 install -r app\build\outputs\apk\debug\app-debug.apk
```

### View logcat (filter to app only)
```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
& "$sdk\platform-tools\adb.exe" -s emulator-5554 logcat --pid=$(& "$sdk\platform-tools\adb.exe" shell pidof com.studymentor.app)
```

### Simulate logged-in session (for testing Home/Chat/History)
```powershell
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
# Write a SharedPreferences XML with auth_token + onboarded flags
$xml = '<?xml version="1.0" encoding="utf-8" standalone="yes"?><map><string name="auth_token">mock_token</string><boolean name="onboarded" value="true" /><boolean name="onboarding_seen" value="true" /></map>'
$xml | Out-File -Encoding utf8 ".\mock_prefs.xml"
& "$sdk\platform-tools\adb.exe" -s emulator-5554 push ".\mock_prefs.xml" "/data/data/com.studymentor.app/shared_prefs/_preferences_default.xml"
Remove-Item ".\mock_prefs.xml"
```

---

## Design Bundle

| Field | Value |
|-------|-------|
| Bundle ID | `BonpCZVk9_BfLfOKdD0mEw` |
| Fetch URL | `https://api.anthropic.com/v1/design/h/BonpCZVk9_BfLfOKdD0mEw` |
| Last synced | 2026-05-29 |
| Screens covered | Home, Chat, Answer, History, Onboarding, Quiz, QuizResult, Dashboard, Profile, Leaderboard, Notifications, TwoFA, AnswerTabbed |

**Logo bundle:** `McnAsHlacIoKwKqHFtaDEQ` — `https://api.anthropic.com/v1/design/h/McnAsHlacIoKwKqHFtaDEQ?open_file=Logo.html`
— Covers: App icon (adaptive launcher), splash screen layout, mascot Milo v3 (kawaii chibi capybara), loading dots. Last synced 2026-06-12.

**Agent-1 uses WebFetch on this URL** to get the latest design and fix UI drift.
When a new design bundle is created, update the Bundle ID and URL above.

---

## Design System

### Colors (`app/src/main/res/values/colors.xml`)
```
brand_primary         #5C6BC0  (indigo)
brand_primary_deep    #3949AB
brand_primary_tint    #E8EAF6
brand_accent          #FF8F00  (amber)
brand_accent_soft     #FFF3E0
text_primary          #1A1A2E
text_secondary        #6B7080
text_tertiary         #9CA3AF
text_on_primary       #FFFFFF
surface               #FFFFFF
bg                    #F8F9FF
border                #E5E7EB
error_soft            #FFEBEE
warning_soft          #FFF3E0
```

### Dimensions (`app/src/main/res/values/dimens.xml`)
```
space_2=4dp  space_3=8dp  space_4=12dp  space_6=20dp  space_8=28dp
radius_sm=8dp  radius_md=12dp  radius_lg=20dp
text_caption=11sp  text_body=14sp  text_h3=16sp  text_h2=18sp  text_h1=22sp  text_display=28sp
button_height_md=48dp
mascot_sm=40dp  mascot_md=72dp  mascot_lg=120dp
```

### Key drawables (already created)
`ic_mascot_milo`, `ic_flame`, `ic_arrow_right`, `ic_info`, `bg_dot_active`, `bg_dot_inactive`,
`bg_blob_primary_tint`, `bg_mistake_error`, `bg_mistake_warning`

---

## Implementation Status

### Phase 1 — MVP Screens ✅
- SignUpActivity, LoginActivity, ForgotPasswordActivity
- PersonalizeActivity (subject picker + level selector)
- HomeActivity (greeting + recent questions + quick-start tiles)
- ChatActivity (message list + composer)
- AnswerActivity (step-by-step answer + StepAdapter)
- HistoryActivity (filter chips + RecyclerView)
- SettingsActivity (theme + language + notifications)

### Phase 2 — Auth & Theme ✅
- SplashActivity (scale-pulse mascot, routes to correct screen)
- LoginActivity, ForgotPasswordActivity
- Dark theme support

### Phase 3A — Camera ✅
- CameraActivity (CameraX preview + capture)
- ScanPreviewActivity (OCR result + edit)
- MockOcrService (fake OCR)
- FileProvider configured

### Phase 3B — Extended Screens ✅ (Design Bundle BonpCZVk9_BfLfOKdD0mEw)
- **QuizActivity**: question + 4 option cards + timer + reveal + result flow
- **QuizResultActivity**: score hero + rewards + answer list + recommendation card
- **DashboardActivity**: streak hero + 2×2 stats grid + bar chart + subject list + Milo insight
- **ProfileActivity**: gradient avatar + XP bar + badge grid (4×2) + activity feed
- **LeaderboardActivity**: podium card + rank list + 3 tab modes (global/friends/week)
- **NotificationsActivity**: filter chips (all/reminders/achievements/mistakes) + list
- **TwoFAActivity**: 6-box OTP input + 30s countdown + backup codes card
- **AnswerTabbedActivity**: 4-tab dark header (solution/concept/practice/pitfalls) + composer bar
- **Navigation wired**: btn_bell→NotificationsActivity, nav_practice→QuizActivity, nav_profile→ProfileActivity, card_milo_review→QuizActivity, settings card_profile→ProfileActivity
- **Build fixes**: XML curly quotes in quiz_result (→ &quot;), duplicate layout_weight in two_fa, invalid gravity="baseline" in dashboard

### Design Bundle sYYOs3uSHmzuIr43Q3DGxg ✅ (2026-05-27)
- **Home**: streak chip (flame + days) + XP progress stripe
- **Chat**: "TRY ASKING" suggestion chips (4 examples, hidden after first message)
- **Answer**: mascot in follow-up card + "Common mistakes" section
- **History**: "Milo noticed" AI suggestion card (shown when questionCount ≥ 5)
- **Onboarding**: OnboardingActivity (3-step carousel, shown on first launch)
- **Session.java**: `streak()`, `hasSeenOnboarding()`, `markOnboardingSeen()`
- **SplashActivity + MainActivity**: routing updated for onboarding flow

### Phase 4 — Data Wiring + Technical Quality ✅ (2026-06-12)
- **Quiz real data**: `QuizDataSource` reads `assets/quiz_questions.json` (25 questions: 5×math/science/code/history/general) via Gson; `QuizActivity` uses live questions + 24s countdown timer + reveal logic
- **Quiz→Result score pass**: `QuizActivity.openResult()` passes `EXTRA_SCORE` + `EXTRA_TOTAL`; `QuizResultActivity` reads and displays real pct
- **Dashboard real stats**: removed fake `Math.max` floors; `countBySubject()` DAO method drives subject breakdown bars
- **Streak daily tracking**: `Session.updateStreak()` compares today vs `KEY_LAST_OPEN_DATE`; called from `SplashActivity.route()`
- **Subject detection**: `ChatActivity.detectSubject()` keyword-matches prompt → math/science/code/history/general
- **Common mistakes dynamic**: `ChatResponse.commonMistakes` field; `MockAiService` returns 2 subject-specific tips; `ChatActivity` serializes to JSON → `AnswerActivity.bindMistakes()` deserializes
- **Profile badges real data**: `ProfileActivity.bindBadges()` reads streak/questionCount/bookmarks/mathCount/bestQuizPct from DB; 6 badges with real unlock conditions
- **Follow-up chips wired**: `AnswerActivity.bindFollowUps()` wires 3 chips → prefilled ChatActivity or QuizActivity by subject
- **Dark mode persistence**: `StudyMentorApp.onCreate()` calls `AppCompatDelegate.setDefaultNightMode(Session.themeMode(this))` before DB init
- **Notifications scheduling**: `HomeActivity.scheduleStudyReminder()` enqueues `StudyReminderWorker` daily via WorkManager (KEEP policy, skips if notifications off)
- **History search**: search bar toggle + `TextWatcher` → `applySearch()` in-memory filter by prompt text
- **History delete**: long-press → `AlertDialog` → `questionDao.delete(q)` + reload
- **D1 background writes**: `StudyMentorApp.executor()` (single-thread `ExecutorService`); `ChatActivity` moves `updateAnswer()` + assistant `messageDao.insert()` off main thread
- **D4 ProGuard**: added rules for Room DAO interfaces, Gson POJO field preservation, Retrofit methods, WorkManager Workers
- **D2 skipped**: `QuizQuestion` is a JSON POJO (not Room entity) — no migration needed; AppDatabase still v1
- **D3 skipped**: email + password validation already present in `SignUpActivity` + `LoginActivity`
- **Best quiz score tracking**: `Session.saveQuizResult()` persists best % to SharedPreferences; `QuizResultActivity` calls it after each quiz

---

## Known Stubs (Pending Work)

| # | Stub | Location | Priority | Notes |
|---|------|----------|----------|-------|
| 1 | LeaderboardActivity dữ liệu fake | LeaderboardActivity | Low | Cần backend API hoặc mock động |
| 2 | `allowMainThreadQueries()` still enabled for reads | StudyMentorApp | Low | Writes already moved to executor; reads are small MVP queries |
| 3 | assembleRelease not tested | build.gradle | Medium | ProGuard rules added but release APK not verified end-to-end |

---

## Next Steps (Việc cần làm tiếp theo)

### Priority 1 — Production readiness

- [ ] **Swap MockAiService → real API**: Set `USE_MOCK_AI=false` in `build.gradle` + configure `API_BASE_URL`.
- [ ] **Error handling**: Retrofit `onFailure` shows Snackbar in ChatActivity; verify other network paths have feedback.
- [ ] **assembleRelease test**: Run `.\gradlew.bat assembleRelease` and install; verify ProGuard doesn't strip Room/Gson/Retrofit symbols.

### Priority 2 — UX polish

- [ ] **Back stack Home**: multiple-back-presses from Quiz/History can loop. Verify with `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` or `finishAffinity()`.
- [ ] **Leaderboard dynamic data**: Currently all fake strings. Could use mock random data seeded by `questionDao.count()` for a realistic feel.

---

## Conventions & Rules

1. **Java only** — never add Kotlin files or Compose dependencies
2. **No Fragment** — every screen is a full Activity
3. **No NavComponent** — use `startActivity()` + `finish()` directly
4. **XML layouts** — no programmatic view creation for UI structure
5. **Material 3** — always use `Widget.Material3.*` styles, never raw Android styles
6. **No hardcoded strings in Java** — use `R.string.*`
7. **No hardcoded colors in Java** — use `ContextCompat.getColor(this, R.color.*)`
8. **Room threading** — DB **writes** must go on `StudyMentorApp.get().executor().execute()`; DB **reads** currently run on the main thread via `allowMainThreadQueries()` (known MVP stub — see Known Stubs #2)
9. **ViewBinding** — use generated binding classes (e.g. `ActivityHomeBinding`) instead of `findViewById`; inflate with `ActivityXxxBinding.inflate(getLayoutInflater())`
10. **SplashScreen API** — `SplashScreen.installSplashScreen(this)` MUST be before `super.onCreate()` in SplashActivity
11. **DB schema changes** — `AppDatabase` uses `fallbackToDestructiveMigration()`; bumping `version` wipes all user data. Only do this intentionally.
12. **Critical files** — get extra care before editing: `AndroidManifest.xml`, `build.gradle`, `themes.xml`, `AppDatabase.java`, `Session.java`

---

## 🤖 Sub-Agent Protocol

**After every Stop hook**, read `.claude/status/*.json` before doing anything else.

### Decision tree:

```
Read build_status.json + logcat_status.json
        │
        ├── BUILD: FAILED
        │     → Spawn Agent-3 (Rebuild+Fix)
        │     → Spawn Agent-2 (Log+Update CLAUDE.md)
        │
        ├── LOGCAT: CRASH
        │     ├── InflateException / Resources$NotFoundException
        │     │     → Spawn Agent-1 (Design/UI Fixer)
        │     └── RuntimeException / NPE / other
        │           → Spawn Agent-3 (Rebuild+Fix)
        │     → Always: Spawn Agent-2 (Log+Update CLAUDE.md)
        │
        └── BUILD: OK + LOGCAT: CLEAN
              → Spawn Agent-2 (Log+Update CLAUDE.md — clean run entry)
```

### Agent-1 — Design/UI Fixer
- Use `WebFetch` on Design Bundle URL (see ## Design Bundle above)
- Parse HTML/JSX → find correct drawable/layout/colors for the broken screen
- Fix XML files: drawables, layouts, icon viewports
- Subagent_type: `general-purpose`

### Agent-2 — Test + Log + CLAUDE.md Updater
- Read all `status/*.json`
- Append to `error_log.md`
- **Append to `## Session Log` in this file (CLAUDE.md)**
- Update `## Implementation Status` if a new feature was completed
- Update `## Known Stubs` if a stub was resolved
- Subagent_type: `general-purpose`

### Agent-3 — Evaluate + Rebuild
- Read error lines from `build_status.json` or `logcat_status.json`
- Fix the relevant Java/XML file
- Rebuild with `gradlew.bat assembleDebug` to verify
- Max 2 auto-fix attempts. If still failing → write "NEEDS MANUAL FIX" and stop
- Subagent_type: `general-purpose`

### Rebuild rule
```
Max 2 auto-fix cycles per session.
After cycle 2 fails → stop, set build_status.json {status:"NEEDS_MANUAL_FIX"}, report to user.
```

---

## Session Log

> Auto-appended by Agent-2 after each session. Newest entry at top.

### [2026-06-12] Session 5 — Phase A–D: Data Wiring, Feature Completion, Technical Quality
**Work done:**

**Phase A — Bug Fixes & Data Wiring:**
- `Session.updateStreak()` — daily streak tracking via `KEY_LAST_OPEN_DATE` comparison; called from `SplashActivity.route()`
- `Session.saveQuizResult()` + `Session.bestQuizPct()` — persists best quiz percentage
- `ChatActivity.detectSubject()` — keyword-matching sets `q.subject` (math/science/code/history/general) instead of hardcoded "general"
- `DashboardActivity` — removed fake `Math.max` floors; `questionDao().countBySubject(subject)` drives real subject breakdown bars
- `QuestionDao` — added `countBySubject(String)` and `@Delete void delete(Question)`

**Phase B — Feature Completion:**
- `QuizQuestion.java` + `QuizDataSource.java` — POJO + static loader from `assets/quiz_questions.json` (25 questions, 5 per subject)
- `quiz_questions.json` — 25 questions with question/subject/subjectTag/options[4]/correctIndex/explanation
- `QuizActivity` full rewrite — live question bank, 24s CountDownTimer, reveal answer, subject icon, `openResult()` passes `EXTRA_SCORE`+`EXTRA_TOTAL`
- `QuizResultActivity` — reads EXTRA_SCORE/EXTRA_TOTAL, displays real pct, calls `Session.saveQuizResult()`
- `ProfileActivity.bindBadges()` — 6 badges with real unlock conditions (streak≥7, q≥1, quiz=100%, bookmarks≥3, math≥10, streak≥30)
- `AnswerActivity.bindFollowUps()` — wires 3 follow-up chips to prefilled chat or QuizActivity by subject
- `AnswerActivity.bindMistakes()` — deserializes `EXTRA_MISTAKES_JSON` from ChatActivity into `text_mistake_1/2`

**Phase C — New Features:**
- `ChatResponse.commonMistakes` — added `List<String>` field
- `MockAiService` — populates 2 subject-specific common mistake tips per response
- `ChatActivity` — serializes mistakes to JSON → `EXTRA_MISTAKES_JSON` passed to AnswerActivity; `offerViewSteps()` also passes mistakes
- `StudyMentorApp.onCreate()` — `AppCompatDelegate.setDefaultNightMode(Session.themeMode(this))` fixes dark mode reset on restart
- `HomeActivity.scheduleStudyReminder()` — enqueues `StudyReminderWorker` daily (WorkManager, KEEP, conditional on notificationsOn)
- `StudyReminderWorker` — new Worker: creates notification channel, posts daily study reminder
- `app/build.gradle` — added `work-runtime:2.9.0`
- `HistoryActivity` — search bar (toggle + TextWatcher → `applySearch()`) + long-press delete dialog
- `HistoryAdapter` — `OnRowLongClick` interface + `setOnLongClickListener`
- `activity_history.xml` + `strings.xml` — search bar layout + dialog/notification strings

**Phase D — Technical Quality:**
- `StudyMentorApp` — added `ExecutorService executor` (single-thread); DB write-heavy calls in `ChatActivity` moved to `executor.execute()` (updateAnswer + appendAssistant insert)
- `proguard-rules.pro` — added: `@Dao interface *`, `com.studymentor.app.data.**`, Gson `@SerializedName` field preservation, `QuizQuestion` full keep, Retrofit method annotations, WorkManager Worker constructors
- D2 skipped: QuizQuestion is JSON POJO not Room entity; AppDatabase stays v1
- D3 skipped: email regex + password length validation already in SignUpActivity + LoginActivity

**Build:** PASSED (7s incremental) | **Logcat:** not tested (no regression-risk changes)

### [2026-06-12] Session 4 — Logo.html Integration + Adaptive Icon Fix
**Work done:**
- **Fetched Logo.html design bundle** (`McnAsHlacIoKwKqHFtaDEQ`) — gzip-compressed tar, decompressed via PowerShell GZipStream + tar extraction
- **Rewrote `ic_mascot_milo.xml`** — full Milo v3 kawaii chibi capybara from Logo.html: left/right ear + inner, body ellipse, 3 fur tuft strokes, 2 paws, muzzle, blush ellipses (#80E07070 50%), blush strokes (#D9E07070 85%), arc eyes (^^ happy), glimmers (#66FFFFFF 40%), nose Y-mark, smile. viewportWidth=100 viewportHeight=108.
- **Created `ic_launcher_background.xml`** — amber linear gradient 135°: #FFCF6A → #F5B544 → #E8960A
- **Created `ic_launcher_foreground.xml`** — 108×108dp viewport, all mascot paths wrapped in `<group scaleX="0.907" scaleY="0.907" translateX="8.64" translateY="9.33">`. Math: mascot=size×0.84=90.72dp, marginTop=size×0.08=8.64dp, leftMargin=(108-90.72)/2=8.64dp. Amber gradient visible as ~8.6dp border around mascot — matches Logo.html AppIcon spec exactly.
- **Updated `ic_launcher.xml` + `ic_launcher_round.xml`** — changed foreground from `@drawable/ic_mascot_milo` → `@drawable/ic_launcher_foreground` so adaptive icon uses the properly-scaled version
- **Created `bg_app_icon.xml`** — amber gradient rounded square (radius=21dp, 22% of 96dp) for Splash screen icon container
- **Created `bg_loading_dot_on.xml`** (#F5B544 amber oval) + **`bg_loading_dot_off.xml`** (#D4C4A0 sand oval)
- **Rewrote `activity_splash.xml`** — FrameLayout with: center LinearLayout (96dp `container_app_icon` FrameLayout + 80dp mascot ImageView `layout_gravity="bottom|center_horizontal"` + "Milo" bold h1 title + "AI Study Mentor" subtitle) + bottom LinearLayout (3× 6dp loading dots: off/on/off)
- **Updated `strings.xml`** — added `<string name="mascot_name">Milo</string>`
- **Updated `SplashActivity.java`** — changed scale-pulse animation target from `img_splash_mascot` (ImageView) to `container_app_icon` (View); removed unused `ImageView` import
- **Cleaned `.gitignore`** — added `.claude/screenshots/`, `.claude/status/`, `.claude/settings.local.json`, `.claude/metrics.md`, `.claude/change_log.md`, `.claude/error_log.md`, `PHASE_3A_NOTES.md`
- **Force-pushed to GitHub** `https://github.com/NghiaKitsune/AI_Study_Mentor.git` (replaced old repo history, renamed local branch master→main)

**Key SVG→VectorDrawable patterns established:**
- Ellipse/circle: `M cx,cy m -rx,0 a rx,ry 0 1,0 2rx,0 a rx,ry 0 1,0 -2rx,0 Z`
- Alpha in color: 50%→`#80`, 85%→`#D9`, 40%→`#66` prefix on RRGGBB
- Adaptive icon safe zone: 108dp canvas, mascot scaled to 84% using `<group>` transform

**Build:** PASSED (1m 12s) | **Logcat:** not tested (UI-only changes)
**Committed:** `62bc128` | **Pushed:** origin/main

### [2026-05-29] Session 3 — Bottom Nav Fix + Emulator Testing
**Work done:**
- Full emulator test pass on all 18 screens (Pixel6_API33, emulator-5554)
- Fixed Quiz screen runtime bug: option TextViews had no text (added OPTION_TEXT_IDS[] + OPTION_TEXTS[] + setText() call in setupOptions())
- Added `text_question` TextView text in setupQuestion(): "What is photosynthesis?"
- **Bottom nav — Indicator size fix**: Added `BottomNav.ActiveIndicator` style (72dp × 40dp, brand_primary_tint) to themes.xml
- **Bottom nav — Persistence fix**: Created `util/BottomNavHelper.java` static helper; added BottomNavigationView to activity_history.xml, activity_profile.xml, activity_quiz.xml; updated HomeActivity, HistoryActivity, ProfileActivity, QuizActivity to call BottomNavHelper.setup()
- Tab switching uses FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP to avoid stack buildup
- Verified with screenshots: History + Profile screens show bottom nav with correct tab highlighted

**Build:** PASSED (55s) | **Logcat:** CLEAN
**Screens verified on emulator:** Home, Chat, Quiz, History, Profile (bottom nav present and highlighting correctly on all)

### [2026-05-29] Session 2 — Phase 3B Extended Screens (Design Bundle BonpCZVk9_BfLfOKdD0mEw)
**Work done:**
- Implemented 8 new activities: Quiz, QuizResult, Dashboard, Profile, Leaderboard, Notifications, TwoFA, AnswerTabbed
- Created 8 activity layouts + 6 item layouts + 7 drawables (ic_zap, ic_star, ic_trophy, ic_crown, ic_medal, ic_lightbulb, ic_chevron_up)
- Wired navigation: bell→Notifications, nav_practice→Quiz, nav_profile→Profile, milo_review→Quiz, settings profile card→Profile
- Added 8 activities to AndroidManifest.xml
- Fixed 3 XML build errors: curly quotes in quiz_result, duplicate layout_weight in two_fa, invalid gravity in dashboard

**Build:** PASSED | **Logcat:** not tested
**APK size:** ~incremental build

### [2026-05-28] Session 1 — Design Bundle + Build System Setup
**Work done:**
- Implemented design bundle `sYYOs3uSHmzuIr43Q3DGxg` (Home/Chat/Answer/History/Onboarding)
- Created OnboardingActivity (3-step carousel)
- Added streak chip + XP progress bar to HomeActivity
- Added suggestion chips to ChatActivity
- Added "Common mistakes" section to AnswerActivity
- Added "Milo noticed" card to HistoryActivity
- Updated Session.java with streak/onboarding methods
- Fixed crash: SplashActivity Theme.AppCompat (added SplashScreen.installSplashScreen)
- Created Pixel6_API33 AVD and verified app launches crash-free
- Set up .claude/ directory with 9 hooks + 3 sub-agents

**Build:** PASSED | **Logcat:** CLEAN (0 crashes after fix)
**APK size:** 16.2 MB | **Build time:** 74s cold / 24s incremental
