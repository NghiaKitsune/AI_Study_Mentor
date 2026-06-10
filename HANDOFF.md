# AI Study Mentor — Handoff Notes

> Treat this file as the developer's "first 30 minutes" guide. Read this, then `README.md`, then start coding.

---

## 1 · What is shipped vs what is stubbed

### ✅ Fully wired (you can run + interact)

| Screen | Activity | Layout | State |
|---|---|---|---|
| Launcher (router) | `MainActivity` | `activity_main.xml` | ✅ routes by session |
| Sign Up | `SignUpActivity` | `activity_sign_up.xml` | ✅ form + strength meter + mock auth |
| Personalize | `PersonalizeActivity` | `activity_personalize.xml` | ✅ saves to SharedPreferences |
| Home | `HomeActivity` | `activity_home.xml` | ✅ greeting + tiles + recent + composer + bottom nav |
| Chat | `ChatActivity` | `activity_chat.xml` | ✅ multi-turn against `MockAiService` |
| Answer | `AnswerActivity` | `activity_answer.xml` | ⚠ prompt + final answer rendered; **steps RV is empty** |
| History | `HistoryActivity` | `activity_history.xml` | ✅ list from Room + stats + filter chips |
| Settings | `SettingsActivity` | `activity_settings.xml` | ✅ rows + sign out |

### ⚠ Known stubs / TODOs in the code

- `AnswerActivity.rv_steps` is wired but has no adapter — wire one from `ChatResponse.steps` once the backend returns them. Each step renders into `item_answer_step.xml`.
- Filter chips in `HistoryActivity` re-query the full list. Add subject/bookmark filtering in `bindFilter()`.
- Streak chip in Home (`btn_streak`) is hard-coded — no streak logic yet.
- Bookmark icon in Answer doesn't toggle visually (no `ic_bookmark_filled` drawable yet — add one if you want filled state).
- Bell, camera button, "Practice" tab → all `Toast.makeText(R.string.toast_coming_soon)`.

### ❌ Not in this starter (call out for the next sprint)

- **Splash** (Android 12+ splash API is wired via `Theme.StudyMentor.Splash` but no full splash screen layout — the system splash kicks in automatically).
- **Login** (separate from Sign Up). Stub a `LogInActivity` modeled on `SignUpActivity`.
- **Forgot password**.
- **Camera scan** (UC2). Need a `CameraActivity` using CameraX.
- **Empty / Loading / Error states**. Each list screen has data-driven RVs but no empty placeholder.
- **Notification permission prompt** (POST_NOTIFICATIONS on Android 13). Use `ActivityResultContracts.RequestPermission` on first launch from Home.
- **Delete account**.

---

## 2 · Project layout cheat-sheet

```
app/src/main/
├── AndroidManifest.xml         ← perms: INTERNET, CAMERA, READ_MEDIA_IMAGES, POST_NOTIFICATIONS
├── java/com/studymentor/app/
│   ├── StudyMentorApp.java     ← Application — builds the Room DB
│   ├── ui/                     ← 8 Activities (1 router + 7 screens)
│   ├── ui/adapter/             ← MessageAdapter, HistoryAdapter
│   ├── data/                   ← Room: Question, Message, DAOs, AppDatabase
│   ├── api/                    ← Retrofit interface + Mock + DTOs
│   └── util/Session.java       ← SharedPreferences helper
└── res/
    ├── values/      colors, dimens, strings, themes, styles, font_certs
    ├── values-night/ dark theme
    ├── values-vi/   Vietnamese strings
    ├── drawable/    icons (vector) + backgrounds + mascot
    ├── layout/      activity_* + item_*
    ├── menu/        bottom_nav.xml
    └── font/        Google Fonts XMLs
```

---

## 3 · Navigation map

```
MainActivity (router)
        │
        ├─ not logged in ──► SignUpActivity ──► PersonalizeActivity ──► HomeActivity
        ├─ logged, no onboard ──► PersonalizeActivity
        └─ logged + onboarded ──► HomeActivity
                                      │
                                      ├── BottomNav: Home ⇄ History ⇄ Profile(Settings)
                                      │
                                      └── composer / tile / recent ──► ChatActivity
                                                                              │
                                                                              └── (long answer) ──► AnswerActivity
```

| From → To | Trigger | Extras |
|---|---|---|
| Home → Chat | composer / tile / recent | `EXTRA_PROMPT` (optional) |
| Home → History | bottom nav | — |
| Home → Settings | bottom nav (Profile) | — |
| Chat → Answer | long-form assistant reply | `EXTRA_QUESTION_ID` |
| History → Answer | row click | `EXTRA_QUESTION_ID` |
| Settings → SignUp | sign out | `FLAG_ACTIVITY_CLEAR_TASK` |

---

## 4 · Wiring the real AI backend (5-minute swap)

1. Open `app/build.gradle`. In `buildTypes.debug`:
   ```gradle
   buildConfigField "boolean", "USE_MOCK_AI", "false"
   buildConfigField "String",  "API_BASE_URL", "\"https://YOUR-DOMAIN/\""
   ```
2. Rebuild. `ApiClient` now returns a real Retrofit client instead of `MockAiService`.
3. Backend must implement the contract in `api-contract/` — `POST /api/chat` with the JSON schemas provided.
4. **Do NOT bundle your OpenAI / Claude API key in the APK** — proxy through your own backend.

---

## 5 · First run on Android 13+ emulator

1. Open this folder in Android Studio.
2. Gradle sync (~2 min first time).
3. Make sure you have **SDK 34** installed (Tools → SDK Manager → "Android 14.0 (UpsideDownCake)" platform).
4. Create an AVD: **Pixel 6 · API 33** (Android 13).
5. Run. The launcher icon is amber with Milo on top.
6. Sign up with any email + password (≥ 8 chars) → Personalize → Home.
7. Tap the composer at the bottom → Chat. Type anything. The mock service replies after ~1s.

If Gradle complains about Java version: **Settings → Build → Gradle → Gradle JDK → Java 17**.

---

## 6 · Recommended order to extend

1. **Wire AnswerActivity steps** (small — 1 hour). Create `StepAdapter` modeled after `MessageAdapter`. Read steps from `ChatResponse.steps` and pass into AnswerActivity via Intent extras or by re-fetching.
2. **Add Splash + Login** (medium — 2-3 hours). Mirror SignUp layout. Add a "Have an account? Log in" path.
3. **Camera scan** (UC2) — biggest piece. CameraX preview → take picture → POST image to a vision endpoint → seed the chat with the recognized prompt.
4. **Real backend** — see section 4.
5. **Empty / loading / error states** — extract a small `<include>` layout `view_empty_state.xml` (mascot + message + CTA) and toggle visibility from each list activity.
6. **Notification permission** — request POST_NOTIFICATIONS on Home first-launch.

---

## 7 · Things to gut-check before you submit

- [ ] Run on **API 33 emulator** at least once end-to-end (signup → chat → answer → history → sign out).
- [ ] Toggle system **dark mode** in the emulator — verify all screens still readable. (Some tile icons may need tint review.)
- [ ] Rotate the device — `windowSoftInputMode="adjustResize"` is set on Chat + SignUp; verify scroll behavior.
- [ ] Touch targets — all `MaterialButton.IconButton` are 40 or 44 dp; that meets the 48 dp WCAG recommendation when including padding.
- [ ] Strings — `values/strings.xml` is English; `values-vi/strings.xml` is partial. Localize the rest before final.
