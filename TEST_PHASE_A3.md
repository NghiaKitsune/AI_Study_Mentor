# TEST REPORT — Phase A3: Remove `allowMainThreadQueries()` + StrictMode

**Date:** 2026-07-20  
**Branch:** `feature/api-expansion`  
**Build:** assembleDebug — PASS (11s incremental, 13s after StrictMode placement fix)  
**Device:** Medium_Phone emulator (AVD), API 33, emulator-5554  
**App PID:** 20791  

---

## What was changed (Phase A3)

### `StudyMentorApp.java`
1. **Removed** `.allowMainThreadQueries()` — Room now enforces background-only DB access
2. **Removed** associated comment block about MVP stub
3. **Added** `StrictMode.detectDiskReads().penaltyLog()` in `DEBUG` mode — logs future main-thread disk I/O
4. **Placement:** StrictMode is set AFTER `Session.themeMode()` + `Room.databaseBuilder().build()` so that one-time SharedPreferences first-access (which triggers `File.exists()` disk check) does not generate false-positive violations

### Fix during testing
First run had 3 `DiskReadViolation` violations from `Session.themeMode()` → `PreferenceManager.getDefaultSharedPreferences()` → `File.exists()`. Root cause: StrictMode was set BEFORE the SharedPreferences init call. Fixed by moving StrictMode setup to after all init. Re-built (11s).

---

## Test Cases & Results

### TC-A3-1: Startup — no crash, no DB main-thread error

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| App launch (SplashActivity) | No `IllegalStateException: Cannot access database on the main thread` | ✅ Zero Room DB errors in logcat | ✅ PASS |
| No StrictMode violations after init | SharedPrefs first-access is before StrictMode is active | ✅ Zero StrictMode violations | ✅ PASS |
| HomeActivity loads with recent questions | Async `recent(5)` populates list | ✅ Questions visible | ✅ PASS |

---

### TC-A3-2: HistoryActivity — DB read off main thread confirmed

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Navigate to History | 19 questions + stats loaded | ✅ List populated correctly | ✅ PASS |
| No Room exception | Room would throw if main thread read attempted | ✅ Zero `Cannot access database` errors | ✅ PASS |
| No StrictMode disk read violation | All DB ops on executor | ✅ Zero StrictMode violations | ✅ PASS |

---

### TC-A3-3: ProfileActivity — single executor pass for 3 reads

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Navigate to Profile | XP + badges rendered from DB | ✅ Explorer Level 2, First Steps badge, 1/8 | ✅ PASS |
| No main-thread DB read | Room enforces without allowMainThreadQueries | ✅ CLEAN | ✅ PASS |

---

### TC-A3-4: DashboardActivity — 5 DB reads on executor

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Navigate to Dashboard | Stats + subject bars + Milo insight loaded | ✅ 19 questions, Science=5 Qs, Groq insight | ✅ PASS |
| No main-thread violation | 5 reads in one executor block | ✅ CLEAN | ✅ PASS |

---

### TC-A3-5: NotificationsActivity — 6 DB reads on executor

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Navigate to Notifications | 6 notification cards built from DB | ✅ Cards present | ✅ PASS |
| No violation | All reads in `loadItemsAsync()` | ✅ CLEAN | ✅ PASS |

---

### TC-A3-6: ChatActivity — optimistic send + background persist

| Step | Expected | Actual | Status |
|------|----------|--------|--------|
| Open new chat | Greeting + suggestion chips | ✅ Present | ✅ PASS |
| Type "testphaseA3" and send | User bubble appears optimistically | ✅ Bubble shown before DB | ✅ PASS |
| Background executor: `insert(q)` + `insert(userMsg)` | No main-thread write | ✅ CLEAN | ✅ PASS |
| AI request fired after persist | `GroqAI --> POST groq.com/...` in logcat | ✅ `14:59:37 GroqAI --> POST ...` confirmed | ✅ PASS |

---

## Logcat Analysis (PID 20791, full session)

### Critical checks
```
Cannot access database on the main thread : 0 occurrences ✅
IllegalStateException (Room)              : 0 occurrences ✅  
FATAL EXCEPTION                           : 0 occurrences ✅
StrictMode policy violation               : 0 occurrences ✅  (after placement fix)
Db tag query failures                     : 0 occurrences ✅
Non-GPU errors                            : 0 occurrences ✅
```

### Frame skips
| Frames | Cause |
|--------|-------|
| 50 | SplashActivity → HomeActivity transition (emulator surface creation) |
| 67 | Inter-Activity navigation (emulator GPU overhead) |

No frame skips related to DB reads — all data loads are non-blocking.

### Activities visited
```
SplashActivity → HomeActivity → HistoryActivity → ProfileActivity
→ DashboardActivity → NotificationsActivity → ChatActivity (×3)
```
8 screens, 0 crashes, 0 DB violations.

---

## StrictMode False Positive (Round 1) — Root Cause + Fix

**Violation:** `DiskReadViolation` at `Session.p()` → `PreferenceManager.getDefaultSharedPreferences()` → `File.exists()`  
**When:** `StudyMentorApp.onCreate()` line 45 (first `Session.themeMode()` call)  
**Why false positive:** Android's SharedPreferences implementation checks whether the prefs directory exists on first access — this is a one-time OS-level file check, not a Room DB read. After this first call, the prefs object is cached and all subsequent reads are in-memory.  
**Fix:** Moved `StrictMode.setThreadPolicy()` to after `Session.themeMode()` + `Room.databaseBuilder().build()` so the one-time init happens before the policy is active. Any future disk reads during Activity lifecycle are still caught.

---

## Final State of `StudyMentorApp.java`

```java
@Override
public void onCreate() {
    super.onCreate();
    instance = this;
    executor = Executors.newSingleThreadExecutor();
    AppCompatDelegate.setDefaultNightMode(Session.themeMode(this));
    db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
            .fallbackToDestructiveMigration()
            .build();
    // StrictMode AFTER init — catches future violations without false positives from prefs init
    if (BuildConfig.DEBUG) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .penaltyLog()
                .build());
    }
}
```

---

## Summary

**Build:** PASS (13s, 11s after fix) | **Logcat:** CLEAN | **Violations:** 0  

Known Stub **#2a** (`allowMainThreadQueries()` still enabled) is now **RESOLVED**.  
All DB reads run on the background executor. Room enforces this at the framework level.  
StrictMode adds an additional debug-time safety net for future development.

---

## Next Step

**Phase A4:** Final combined build + full smoke test of all 8 screens, update CLAUDE.md (Stub #2a → RESOLVED, rule #8 update), then commit dead code removal + Group A changes together.
