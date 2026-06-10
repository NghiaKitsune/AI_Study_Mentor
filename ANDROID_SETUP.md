# Android Studio Setup Guide

> First time opening this starter? Follow these steps.

## Prerequisites

| Tool | Version |
|---|---|
| Android Studio | Hedgehog (2023.1.1) or newer |
| Gradle JDK | **Java 17** (bundled with Android Studio) |
| Android SDK | API 34 (Android 14) — needed to compile against latest Material |
| AVD | **Pixel 6 · API 33** (Android 13) — for testing the target minSdk |

In Android Studio: **Tools → SDK Manager → SDK Platforms** → install:
- Android 14 (UpsideDownCake, API 34) — the `compileSdk`
- Android 13 (Tiramisu, API 33) — to run on a clean Android 13 device
- Build-Tools 34.0.0+

## 1 · Open the project

1. **File → Open…** → pick the `android-starter/` folder (not the parent).
2. Wait for Gradle sync. First sync takes 2–4 min (downloads dependencies).
3. If you see "Project SDK is not defined": **File → Project Structure → SDK Location** → point Gradle JDK to **Embedded JDK (17)**.

If Gradle complains about Java version even after that, open:
**Settings → Build, Execution, Deployment → Build Tools → Gradle**
and set Gradle JDK → **17**.

## 2 · Create an AVD

1. **Tools → Device Manager → Create Device**.
2. Pick **Pixel 6**.
3. System image: **Tiramisu (API 33, x86_64)** — the closest to your `minSdk 33` target.
4. Finish. The AVD appears in the Device dropdown.

## 3 · Run the app

1. Pick the AVD from the device dropdown in the toolbar.
2. Press the green ▶ Run button (or `Shift+F10`).
3. Wait for the launcher icon to install (~30 s). The launcher icon is **amber with the Milo mascot on top**.

### What you should see

1. **Splash** (~700 ms) — system splash + Milo
2. **Sign Up** — type any email + 8+ char password → check the box → **Create account**
3. **Personalize** — pick a level chip + 1+ subject chips → **Continue**
4. **Home** — daily challenge card, 4 quick-start tiles, recent questions (empty), composer at bottom
5. Tap the composer or a tile → **Chat** opens
6. Type something → mock AI replies after ~900 ms with a step-by-step answer
7. Tap an answer → **Answer** screen with expandable steps
8. Bottom-nav → **History** shows your conversation list
9. Bottom-nav profile → **Settings** → cycle theme, sign out

## 4 · Common first-run issues

| Symptom | Fix |
|---|---|
| "Manifest merger failed" | Clean & Rebuild (Build → Clean Project, then Build → Rebuild Project). |
| Resource linking errors after a layout edit | Invalidate caches: **File → Invalidate Caches…** → Invalidate and Restart. |
| `BuildConfig` red-underlined | Re-sync Gradle (the elephant icon). `buildConfig true` is set so it should generate. |
| App crashes on launch with `RoomDatabase` error | Uninstall the previous build from the emulator (long-press the icon → Uninstall). The DB schema may have changed. |
| Mock replies feel too slow / fast | Edit the delay in `api/MockAiService.java` (the `900` constant near the bottom). |
| Vietnamese strings show as `[??]` | Confirm the device locale is set to Vietnamese (Settings → System → Languages). Otherwise the English fallback in `values/strings.xml` is correct. |

## 5 · Wiring the real AI backend

When you're ready to swap the mock for a real backend:

1. **Stand up a backend** that implements `api-contract/chat-request.schema.json` ↔ `chat-response.schema.json`. A tiny Node/Express or Vercel function in front of Anthropic Claude / OpenAI is enough.
2. **Set the URL** in `app/build.gradle`:
   ```gradle
   buildConfigField "String",  "API_BASE_URL", "\"https://your-domain.example.com/\""
   buildConfigField "boolean", "USE_MOCK_AI",  "false"   // ← flip to false
   ```
3. Rebuild. `ApiClient.get()` will now return a real Retrofit client instead of `MockAiService`.
4. **DO NOT** put your OpenAI/Claude API key in the APK. Your backend holds the key and proxies the request.

## 6 · Project structure cheat-sheet

```
app/src/main/
├── AndroidManifest.xml             ← 13 Activities + permissions + FileProvider
├── java/com/studymentor/app/
│   ├── StudyMentorApp.java         ← Application — holds Room DB singleton
│   ├── ui/                         ← 13 Activities
│   ├── ui/adapter/                 ← 3 RecyclerView adapters (Message, History, Step)
│   ├── data/                       ← Room: Question, Message + DAOs + AppDatabase
│   ├── api/                        ← Retrofit (AiService) + Mocks + DTOs
│   └── util/Session.java           ← SharedPreferences helper
└── res/
    ├── drawable/    37+ vector icons + bg gradients + mascot
    ├── layout/      14 activity_* + 7 item_* + view_empty_state
    ├── values/      colors, dimens, strings, themes, styles
    ├── values-night/  dark theme overrides
    ├── values-vi/   Vietnamese strings (partial)
    ├── menu/        bottom_nav.xml
    └── xml/         backup_rules, data_extraction_rules, file_paths
```

## 7 · Recommended next steps

See `HANDOFF.md` Section 6 for the suggested order to extend this starter.

In short:
1. Wire `AnswerActivity` real-step parsing (small)
2. Stand up a backend & flip `USE_MOCK_AI=false`
3. Add empty/loading/error states using `view_empty_state.xml`
4. Polish the camera scan flow (already scaffolded — needs a real OCR/vision call)
5. Localize the rest of `values-vi/strings.xml`
