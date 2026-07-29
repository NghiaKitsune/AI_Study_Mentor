# TEST REPORT — Phase A0: Database Threading Helper

**Ngày test:** 2026-07-20  
**Branch:** `feature/api-expansion`  
**Build:** `assembleDebug` — incremental 10s  
**Emulator:** `Medium_Phone` (API 33, emulator-5554)  
**App PID:** 9561 (stable suốt session, không restart)

---

## Phạm vi Phase A0

Phase A0 chỉ thêm method tĩnh `query(Activity, Callable<T>, Consumer<T>)` vào `StudyMentorApp.java`. **Chưa thay đổi bất kỳ Activity nào** — mục tiêu duy nhất là compile helper thành công và app không bị ảnh hưởng.

---

## Kết quả test

| TC | Mô tả | Kết quả | Ghi chú |
|----|-------|---------|---------|
| A0-1 | `assembleDebug` compile sạch | ✅ PASS | 10s incremental, 0 error |
| A0-2 | App launch không crash (Splash → Home) | ✅ PASS | SplashActivity → HomeActivity thành công |
| A0-3 | App process ổn định (không tự restart) | ✅ PASS | PID=9561 giữ nguyên suốt toàn bộ session |
| A0-4 | Không có `FATAL EXCEPTION` trong logcat | ✅ PASS | Không có dòng nào |
| A0-5 | Không có `Cannot access database on the main thread` | ✅ PASS | Còn `allowMainThreadQueries()` nên expected |
| A0-6 | WorkManager StudyReminderWorker khởi động | ✅ PASS | `Worker result SUCCESS` trong logcat |
| A0-7 | HomeActivity là foreground Activity | ✅ PASS | `mCurrentFocus=...HomeActivity` |

---

## Triệu chứng main-thread DB reads (EXPECTED — chưa fix ở A0)

Logcat ghi nhận các cảnh báo sau — đây là **triệu chứng đúng dự kiến** của `allowMainThreadQueries()`, Phase A1-A3 sẽ giải quyết:

```
Choreographer: Skipped 105 frames! The application may be doing too much work on its main thread.
Choreographer: Skipped 127 frames! The application may be doing too much work on its main thread.
Choreographer: Skipped 36 frames!  The application may be doing too much work on its main thread.
Davey! duration=1175ms  (Splash animation bị chặn)
Davey! duration=3307ms  (HomeActivity load bị chặn bởi DB read)
```

Nguyên nhân: `HomeActivity.bindRecent()` gọi `questionDao().recent(5)` trực tiếp trên UI thread trong `onCreate()`. Chính xác là vấn đề Phase A1 sẽ sửa.

---

## Logcat không có lỗi liên quan app

Tất cả errors trong logcat đến từ **system apps** (Google GMS, Play Store, Latin IME) và không liên quan đến `com.studymentor.app`. Đây là hành vi bình thường trên emulator mới boot.

---

## Kết luận

**Phase A0: PASSED ✅**

Helper `StudyMentorApp.query()` compile thành công và không gây regression. App vận hành đúng như trước. Sẵn sàng sang **Phase A1** (chuyển 7 màn chỉ đọc sang background thread).

---

## Điều chưa test ở A0

- Navigation sang History/Profile/Dashboard/Notifications: **chưa test** — Activities không exported (không thể dùng `adb am start`), tập lệnh `input tap` không chắc đúng tọa độ. Sẽ test đầy đủ sau Phase A1 khi behavior thực sự thay đổi.
- Chat gửi tin / bookmark: không thay đổi ở A0, test ở A2.
