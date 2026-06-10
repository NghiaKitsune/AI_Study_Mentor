# Error Log — AI Study Mentor

> Auto-appended by Agent-2 after each session. Newest entry at top.
> Format: Date | Error | Root Cause | Fix | File | Status

---

## [2026-05-28] SplashActivity crash — Theme.AppCompat ✅ FIXED

**Error:** `java.lang.IllegalStateException: You need to use a Theme.AppCompat theme (or descendant) with this activity.`

**Stack trace:**
```
at androidx.appcompat.app.AppCompatDelegateImpl.createSubDecor(AppCompatDelegateImpl.java:926)
at com.studymentor.app.ui.SplashActivity.onCreate(SplashActivity.java:29)
```

**Root cause:** `SplashActivity` uses `Theme.StudyMentor.Splash` which extends `Theme.SplashScreen`. `Theme.SplashScreen` is NOT an AppCompat theme. When `AppCompatActivity.setContentView()` checks the theme, it throws because the splash theme doesn't have AppCompat attributes. The fix requires calling `SplashScreen.installSplashScreen()` which internally swaps the theme to `postSplashScreenTheme` (which IS AppCompat) before AppCompat checks it.

**Fix applied:**
```java
// SplashActivity.java — added BEFORE super.onCreate()
SplashScreen.installSplashScreen(this);
super.onCreate(savedInstanceState);
// Also added import: androidx.core.splashscreen.SplashScreen
```

**File:** `app/src/main/java/com/studymentor/app/ui/SplashActivity.java` line 29
**Session:** 2026-05-28
**Agent:** Manual fix (no auto-fix agent yet)
**Status:** ✅ FIXED — verified on Pixel6_API33 emulator, 0 crashes

---

## [2026-05-29] Quiz option text empty (runtime) ✅ FIXED

**Error:** Quiz screen hiển thị 4 option card trống, không có text.

**Root cause:** `setupOptions()` trong `QuizActivity.java` chỉ attach click listener cho từng card, nhưng không gọi `.setText()` trên các `TextView` bên trong card. Mảng `OPTION_TEXT_IDS[]` và `OPTION_TEXTS[]` chưa được khai báo.

**Fix applied:**
```java
// Thêm vào QuizActivity.java
private static final int[] OPTION_TEXT_IDS = {
    R.id.text_option_a, R.id.text_option_b, R.id.text_option_c, R.id.text_option_d
};
private static final String[] OPTION_TEXTS = {
    "The process by which plants make food using sunlight",
    "A chemical reaction that releases energy in animals",
    "Conversion of light energy into chemical energy stored in glucose",
    "Absorption of minerals from soil through root hairs"
};
// Trong setupOptions() loop: ((TextView) findViewById(OPTION_TEXT_IDS[i])).setText(OPTION_TEXTS[i]);
// Trong setupQuestion(): if (question != null) question.setText("What is photosynthesis?");
```

**File:** `app/src/main/java/com/studymentor/app/ui/QuizActivity.java`
**Session:** 2026-05-29
**Agent:** Manual fix
**Status:** ✅ FIXED — verified on Pixel6_API33 emulator

---

## [2026-05-29] Bottom nav indicator quá nhỏ ✅ FIXED

**Error:** Active indicator trên `BottomNavigationView` chỉ ~64×32dp (Material 3 default), trông nhỏ và không vừa với thanh cao 64dp.

**Root cause:** Không override `itemActiveIndicatorStyle` — Material 3 dùng default pill size.

**Fix applied:**
```xml
<!-- themes.xml -->
<style name="BottomNav.ActiveIndicator"
       parent="Widget.Material3.BottomNavigationView.ActiveIndicator">
    <item name="android:width">72dp</item>
    <item name="android:height">40dp</item>
    <item name="android:color">@color/brand_primary_tint</item>
</style>
```

**File:** `app/src/main/res/values/themes.xml` + tất cả layout có `BottomNavigationView`
**Session:** 2026-05-29
**Status:** ✅ FIXED

---

## [2026-05-29] Bottom nav biến mất khi chuyển tab ✅ FIXED

**Error:** Khi bấm History / Profile / Practice từ Home, Activity mới mở lên không có `BottomNavigationView`.

**Root cause:** `BottomNavigationView` chỉ tồn tại trong `activity_home.xml`. Các Activity còn lại không có widget này trong layout.

**Fix applied:**
- Tạo `util/BottomNavHelper.java` — static helper bind nav cho mọi tab Activity
- Thêm `BottomNavigationView` vào `activity_history.xml`, `activity_profile.xml`, `activity_quiz.xml`
- Gọi `BottomNavHelper.setup(this, R.id.nav_*)` trong `onCreate()` của 4 Activities
- Dùng `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` khi switch tab

**Files:** `activity_history/profile/quiz.xml`, `HomeActivity/HistoryActivity/ProfileActivity/QuizActivity.java`, `BottomNavHelper.java` (NEW)
**Session:** 2026-05-29
**Status:** ✅ FIXED — screenshots xác nhận nav bar hiện đúng tab trên History + Profile

---

## [Template for future entries]

## [YYYY-MM-DD] Short description — Status emoji

**Error:** ...
**Root cause:** ...
**Fix applied:** ...
**File:** ...
**Session:** ...
**Agent:** Agent-1 / Agent-3 / Manual
**Status:** ✅ FIXED / ⚠ PARTIAL / ❌ NEEDS MANUAL FIX
