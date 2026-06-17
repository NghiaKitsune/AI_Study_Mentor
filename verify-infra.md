# Infrastructure Verification Report
Generated: 2026-06-17

---

## 1. WorkManager dependency trong build.gradle

**CÓ** — dòng 77:

```groovy
// app/build.gradle : 77
implementation 'androidx.work:work-runtime:2.9.0'
```

---

## 2. Worker files trong project

**CÓ 1 file:**

| File | Package | Trạng thái trong git |
|------|---------|----------------------|
| `app/src/main/java/com/studymentor/app/StudyReminderWorker.java` | `com.studymentor.app` | **Untracked** (chưa commit) |

Nội dung `StudyReminderWorker`:
- Extends `androidx.work.Worker`
- `doWork()` gọi `ensureChannel()` rồi post notification qua `NotificationManager`
- Channel ID: `"study_reminder"`, Notification ID: `1001`
- Đọc title/body từ `R.string.notif_reminder_title` / `R.string.notif_reminder_body`
- Return `Result.success()` — không có retry logic

Không có file Worker nào khác trong codebase.

---

## 3. AppCompatDelegate.setDefaultNightMode() — gọi ở đâu?

Kết quả grep toàn bộ `app/src/main/java`:

```
StudyMentorApp.java:34      → AppCompatDelegate.setDefaultNightMode(Session.themeMode(this))
SettingsActivity.java:57    → value.setText(themeLabel(Session.themeMode(this)))   ← chỉ đọc label
SettingsActivity.java:60    → int next = nextTheme(Session.themeMode(this))        ← đọc để cycle
SettingsActivity.java:62    → AppCompatDelegate.setDefaultNightMode(next)          ← gọi khi toggle
```

### StudyMentorApp.java — dòng 34 ✅
```java
// StudyMentorApp.java : 29–35
@Override
public void onCreate() {
    super.onCreate();
    instance = this;
    executor = Executors.newSingleThreadExecutor();
    AppCompatDelegate.setDefaultNightMode(Session.themeMode(this));  // ← dòng 34
    db = Room.databaseBuilder(...)
```
Được gọi trong `Application.onCreate()` — chạy **trước khi bất kỳ Activity nào khởi tạo**.
Đây là nơi đúng để restore theme: theme được áp lại ngay khi process start.

### SplashActivity.java — không có
Không có lời gọi `setDefaultNightMode` hay `Session.themeMode()` nào.
Chỉ có routing logic + animation + `Session.updateStreak()`.

### MainActivity.java — không có
Không có lời gọi `setDefaultNightMode` hay `Session.themeMode()` nào.
Chỉ là router thuần (kiểm tra isLoggedIn → startActivity → finish).

### SettingsActivity.java — dòng 62 (toggle lúc runtime)
```java
// SettingsActivity.java : 60–62  (dự đoán từ grep)
int next = nextTheme(Session.themeMode(this));
Session.saveThemeMode(this, next);
AppCompatDelegate.setDefaultNightMode(next);
```
Gọi khi user bấm nút đổi theme — áp theme ngay lập tức cho session hiện tại.

---

## Tổng kết

| Mục kiểm tra | Kết quả | Chi tiết |
|-------------|---------|---------|
| WorkManager dependency | **CÓ** | `work-runtime:2.9.0` — build.gradle dòng 77 |
| Worker files | **CÓ 1** | `StudyReminderWorker.java` (untracked) |
| Theme restore lúc startup | **ĐÃ CÓ** | `StudyMentorApp.onCreate()` dòng 34 — chạy trước mọi Activity |
| SplashActivity gọi themeMode | **KHÔNG** | Không có lời gọi nào |
| MainActivity gọi themeMode | **KHÔNG** | Không có lời gọi nào (chỉ là router) |
| SettingsActivity gọi setDefaultNightMode | **CÓ** | Dòng 62 — chỉ khi user bấm toggle |

**Kết luận:** Theme được restore đúng cách qua `StudyMentorApp.onCreate()` — không phụ thuộc vào
SplashActivity hay MainActivity. Nếu user đang ở dark mode và kill process, lần mở lại vẫn đúng theme
vì `Application` khởi tạo trước khi SplashScreen render.
