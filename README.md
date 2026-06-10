# AI Study Mentor — Android Starter Package

> **Drop-in handoff for Android Studio (Java).** Generated from the design system.
> Target **Android 13+** (minSdk 33, targetSdk 34) · Material Components 3 · Room · Retrofit.

## 📦 What's in here

```
android-starter/
├── README.md                      ← you are here
├── HANDOFF.md                     ← screen-by-screen map + checklist
├── build.gradle                   ← project-level
├── settings.gradle
├── gradle.properties
├── app/
│   ├── build.gradle               ← module-level (deps, sdks)
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/studymentor/app/
│       │   ├── StudyMentorApp.java         (Application)
│       │   ├── ui/
│       │   │   ├── MainActivity.java       (router / launcher)
│       │   │   ├── SignUpActivity.java
│       │   │   ├── PersonalizeActivity.java
│       │   │   ├── HomeActivity.java
│       │   │   ├── ChatActivity.java
│       │   │   ├── AnswerActivity.java
│       │   │   ├── HistoryActivity.java
│       │   │   └── SettingsActivity.java
│       │   ├── ui/adapter/
│       │   │   ├── MessageAdapter.java
│       │   │   └── HistoryAdapter.java
│       │   ├── data/
│       │   │   ├── AppDatabase.java
│       │   │   ├── User.java
│       │   │   ├── Question.java
│       │   │   ├── Message.java
│       │   │   ├── QuestionDao.java
│       │   │   └── MessageDao.java
│       │   ├── api/
│       │   │   ├── AiService.java          (Retrofit interface)
│       │   │   ├── ApiClient.java          (Retrofit builder + interceptors)
│       │   │   ├── MockAiService.java      (offline mock for development)
│       │   │   ├── ChatRequest.java
│       │   │   └── ChatResponse.java
│       │   └── util/
│       │       └── Session.java
│       └── res/
│           ├── values/             colors · dimens · strings · themes · styles
│           ├── values-night/       dark theme
│           ├── values-vi/          Vietnamese strings
│           ├── drawable/           icons (vector) + backgrounds + mascot
│           ├── layout/             activity_* + item_*
│           ├── menu/               bottom_nav.xml
│           ├── font/               Google Fonts XML
│           └── mipmap-anydpi-v26/  ic_launcher
└── api-contract/
    ├── README.md
    ├── chat-request.schema.json
    ├── chat-response.schema.json
    ├── sample-request.json
    └── sample-response.json
```

## 🚀 Open in Android Studio (5 minutes)

1. **Android Studio → New → Project from Existing Sources** (or _File → Open_), choose this `android-starter/` folder.
2. Wait for Gradle sync. If it asks to install missing SDK platforms (API 34) — accept.
3. The `font/` directory uses Downloadable Fonts (Google Fonts). The fonts XMLs need a `font_certs.xml` — Android Studio will create it automatically the first time it sees them, **OR** copy from `res/values/font_certs.xml` (already included).
4. Run on an **Android 13+** emulator or device (Pixel 6, API 33 recommended).
5. The launch flow: `MainActivity` → `SignUpActivity` → `PersonalizeActivity` → `HomeActivity` → `ChatActivity`. Chat uses `MockAiService` by default (offline, no network needed) — see `api/ApiClient.java` to switch to a real backend.

## 🎨 Design system at a glance

| Token | Light | Dark | Use |
|---|---|---|---|
| `colorPrimary` | `#F5B544` (warm amber) | same | Buttons, FAB, focus |
| `bg` | `#FAF5EA` (warm cream) | `#1A1610` | Screen background |
| `surface` | `#FFFFFF` | `#252017` | Cards, sheets |
| Text primary | `#2A2418` | `#F5EFE0` | Headlines, body |
| Font display | Bricolage Grotesque | — | Headings |
| Font body | Plus Jakarta Sans | — | Body text |
| Font mono | JetBrains Mono | — | Code / equations |

All 56 tokens live in `res/values/colors.xml` + `res/values/dimens.xml`. **Don't hardcode hex values** — always reference `@color/...` and `@dimen/...`.

## ✅ MVP acceptance

The app launches and you can:
- [x] Sign up → personalize → land on Home
- [x] Tap composer → ChatActivity, send a message, receive a (mocked) AI reply
- [x] Open any answer → AnswerActivity with step list
- [x] Switch bottom nav: Home ⇄ History
- [x] Open Settings → Sign out clears session

## 🔌 Wiring a real AI backend

Open `app/src/main/java/com/studymentor/app/api/ApiClient.java` and set:
```java
private static final boolean USE_MOCK = false; // ← flip this
private static final String BASE_URL = "https://your-backend.example.com/";
```

The contract is at `api-contract/`. Your backend POSTs to `/api/chat` with the schema in `chat-request.schema.json` and returns the schema in `chat-response.schema.json`. Sample payloads included.

## 📋 What's NOT in this starter

- Camera scan screen (UC2) — design pending
- UC6 Quiz, UC7 Progress, UC8 Gamification, UC9 Notifications — stretch screens
- Splash + Login (separate from SignUp) — easy to add when needed
- Vietnamese full translation — `values-vi/strings.xml` has the first batch only

See `HANDOFF.md` for the screen-by-screen status.
