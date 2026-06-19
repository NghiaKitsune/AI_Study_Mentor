# Performance & Threading Audit
Generated: 2026-06-17

Scope: tất cả Room DAO call · CameraActivity lifecycle · ScanPreviewActivity lifecycle · Glide · WorkManager reminder
Không sửa code — phân tích tĩnh từ source.

---

## 1. Room DAO Calls on Main Thread

Tất cả query dưới đây chạy trên main thread nhờ `allowMainThreadQueries()` trong `StudyMentorApp`.
Cột **Loại** phân biệt Read (đọc) và Write (ghi) — write nghiêm trọng hơn vì CLAUDE.md đã tuyên bố "Writes already moved to executor" nhưng thực tế chưa hết.

| # | Vị trí code | Loại | Mô tả query | Tần suất gọi |
|---|------------|------|-------------|-------------|
| 1 | `HomeActivity.bindXpProgress()` L100 | Read | `questionDao().count()` | Mỗi lần `onCreate` |
| 2 | `HomeActivity.bindRecent()` L136 | Read | `questionDao().recent(5)` | Mỗi lần `onCreate` |
| 3 | `HomeActivity.onResume()` L192 | Read | `questionDao().recent(5)` | **Mỗi lần quay về Home** (từ Chat, Quiz, …) |
| 4 | `ChatActivity.sendCurrent()` L76 | Read | `messageDao().forQuestion(questionId)` | Mỗi lần mở conversation cũ |
| 5 | `ChatActivity.sendCurrent()` L138 | **Write** | `questionDao().insert(q)` — trả về generated ID | Mỗi lần gửi tin nhắn đầu tiên |
| 6 | `ChatActivity.sendCurrent()` L142 | **Write** | `messageDao().insert(userMsg)` | Mỗi lần gửi tin nhắn |
| 7 | `AnswerActivity.onCreate()` L52 | Read | `questionDao().byId(qid)` | Mỗi lần mở màn answer |
| 8 | `AnswerActivity.bindBookmark()` L162 | **Write** | `questionDao().update(question)` | Mỗi lần toggle bookmark |
| 9 | `HistoryActivity.bindStats()` L62 | Read | `questionDao().count()` | `onCreate` + sau mỗi delete |
| 10 | `HistoryActivity.bindStats()` L63 | Read | `questionDao().bookmarkedCount()` | `onCreate` + sau mỗi delete |
| 11 | `HistoryActivity.bindList()` L105 | Read | `questionDao().all()` | `onCreate` |
| 12 | `HistoryActivity.showDeleteDialog()` L121 | **Write** | `questionDao().delete(q)` | Mỗi lần xóa |
| 13 | `HistoryActivity.reload()` L139 | Read | `questionDao().all()` | **Mỗi keystroke trong search + mỗi chip thay đổi** |
| 14 | `HistoryActivity.bindMiloNoticed()` L178 | Read | `questionDao().count()` | `onCreate` |
| 15 | `DashboardActivity.bindLiveStats()` L42 | Read | `questionDao().count()` | `onCreate` |
| 16 | `DashboardActivity.bindSubjects()` L52 | Read | `questionDao().countBySubject("math")` | `onCreate` |
| 17 | `DashboardActivity.bindSubjects()` L53 | Read | `questionDao().countBySubject("code")` | `onCreate` |
| 18 | `DashboardActivity.bindSubjects()` L54 | Read | `questionDao().countBySubject("science")` | `onCreate` |
| 19 | `DashboardActivity.bindSubjects()` L55 | Read | `questionDao().countBySubject("history")` | `onCreate` |
| 20 | `ProfileActivity.bindBadges()` L55 | Read | `questionDao().count()` | `onCreate` |
| 21 | `ProfileActivity.bindBadges()` L56 | Read | `questionDao().bookmarkedCount()` | `onCreate` |
| 22 | `ProfileActivity.bindBadges()` L57 | Read | `questionDao().countBySubject("math")` | `onCreate` |

**Đúng (executor):** ChatActivity L163-164 `updateAnswer()` · ChatActivity L182-183 `messageDao().insert()` cho assistant message ✅

---

## 2. Vấn đề hiệu năng / threading chi tiết

### 2.1 Room — Writes vẫn còn trên main thread

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| `ChatActivity` L138: `questionDao().insert(q)` | INSERT trả về `long id` — không thể trivially wrap executor | Jank khi gửi tin đầu tiên; ANR nếu DB bị lock | Dùng `Callable<Long>` + `Future<Long>` hoặc `LiveData`/callback pattern: `executor.submit(() -> questionDao().insert(q))` rồi `future.get()` trong doWork, hoặc tạm chấp nhận vì insert nhỏ |
| `ChatActivity` L142: `messageDao().insert(userMsg)` | INSERT không cần return value nhưng vẫn ở main thread | Jank hiển thị bubble | `StudyMentorApp.get().executor().execute(() -> messageDao().insert(userMsg))` |
| `AnswerActivity` L162: `questionDao().update(question)` | UPDATE bookmark ở main thread | Jank khi bấm bookmark | `executor.execute(() -> questionDao().update(question))` |
| `HistoryActivity` L121: `questionDao().delete(q)` | DELETE trong callback dialog | Jank sau xác nhận xóa | `executor.execute(() -> { questionDao().delete(q); runOnUiThread(() -> { reload(); bindStats(); }); })` |

### 2.2 Room — Reads nặng trên main thread

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| `HistoryActivity.reload()` L139 + `applySearch()` L145 | `questionDao().all()` được gọi **mỗi keystroke** qua `TextWatcher.afterTextChanged`. Với 100+ questions, mỗi keystroke block main thread | Lag gõ phím; ANR potential trên thiết bị chậm | Load toàn bộ list 1 lần vào bộ nhớ, filter in-memory mà không gọi DB lại; hoặc dùng `LiveData<List<Question>>` + Room `@Query` reactive |
| `DashboardActivity.bindSubjects()` L52–55 | 4 `COUNT(*)` queries riêng lẻ thay vì 1 `GROUP BY subject` | 4 main-thread round-trips | Thêm `@Query("SELECT subject, COUNT(*) as cnt FROM questions GROUP BY subject")` DAO method; 1 query thay 4 |
| `HomeActivity.onResume()` L192 | `questionDao().recent(5)` mỗi lần quay về HomeActivity | Main-thread read sau mỗi navigate-back; thường không thấy vì list ngắn | Chấp nhận ở MVP hoặc cache kết quả với `onPause` timestamp |

### 2.3 CameraActivity — Camera resource lifecycle

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| Không có `onPause()` override | CameraX `bindToLifecycle(this, ...)` tự pause khi Activity stop — OK theo thiết kế CameraX. **Nhưng** `FLAG_KEEP_SCREEN_ON` được set ở `onCreate()` và chỉ được clear ở `onDestroy()` | Khi phone call đến (Activity paused, không destroyed), màn hình vẫn sáng suốt cuộc gọi | Clear flag trong `onPause()`, set lại trong `onResume()` |
| `flipCamera()` L213 gọi `startCamera()` nhiều lần | `startCamera()` gọi `ProcessCameraProvider.getInstance()` → async future. Nếu user tap flip nhiều lần nhanh, nhiều futures chạy song song, mỗi future gọi `provider.unbindAll()` + rebind | Race condition: camera bị unbind/rebind chồng chéo; có thể preview bị đen hoặc duplicate bind exception | Add debounce (disable btn_flip ngay khi tap, re-enable sau rebind thành công) hoặc giữ `cameraProvider` reference sau lần bind đầu |
| `takePhoto()` L166 callback sau khi Activity destroyed | `imageCapture.takePicture()` callback dùng `ContextCompat.getMainExecutor(this)`. Nếu Activity finish trước khi callback về, `openScanPreview()` gọi `startActivity()` trên context đã gone | Unlikely crash (`startActivity` after finish) | Add `if (isDestroyed() || isFinishing()) return;` vào đầu `onImageSaved` callback |
| Không có `provider.unbindAll()` trong `onDestroy()` | CameraX lifecycle binding tự giải phóng — đây là behavior đúng của CameraX, không cần sửa | Không có vấn đề | ✅ Không cần sửa |

### 2.4 ScanPreviewActivity — MockOcrService lifecycle

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| `runMockOcr()` L106 — `MockOcrService.recognize()` khởi chạy thread `"MockOcr"` (0.9–1.6s) | Nếu user bấm Back trước khi OCR xong: `"MockOcr"` thread giữ strong reference đến anonymous `Listener` class → `ScanPreviewActivity.this` không được GC trong ~1.6s | Memory leak tạm thời (~1.6s); sau đó GC bình thường vì delay ngắn. Không crash do `postToMain()` dùng `Handler(Looper.getMainLooper())` — safe | Thêm `AtomicBoolean cancelled = new AtomicBoolean(false)` trong Activity; set `true` trong `onDestroy()`; kiểm tra trong `onSuccess()` callback trước khi gọi `setText()` |
| `MockOcrService.recognize()` L88: `postToMain()` | Callback được post về main thread qua `new Handler(Looper.getMainLooper()).post(r)` → **không crash** khi Activity destroyed (setText trên detached view OK) | Không crash; view update silently ignored nếu Activity gone ✅ | Không cần sửa về crash, chỉ cần giải quyết memory leak trên |

### 2.5 Glide — Image loading

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| `ScanPreviewActivity.onCreate()` L71–74: `Glide.with(this).load(imageUri).centerCrop().into(imgCaptured)` | Load 1 lần duy nhất trong `onCreate`. `Glide.with(this)` lifecycle-aware — tự cancel nếu Activity destroyed | ✅ Không có vấn đề | — |
| Không có `Glide.with(this)` trong `onResume()` hay `onStart()` | Không load lặp lại ✅ | — | — |
| **Không có Glide usage nào khác trong toàn codebase** | Không có ảnh remote hay repeated load nào | ✅ Không có rủi ro Glide | — |

### 2.6 WorkManager — Study Reminder lifecycle

| Vị trí code | Vấn đề | Tác động | Cách sửa đề xuất |
|-------------|--------|----------|------------------|
| `SettingsActivity.showSignOutConfirm()` L111: `Session.clear(this)` → launch SignUp | **Không cancel WorkManager task** khi logout. `"study_reminder"` work tiếp tục chạy hằng ngày dù user đã đăng xuất | User nhận notification "Time to study!" sau khi đã logout — trải nghiệm xấu | Thêm trước `startActivity()`: `WorkManager.getInstance(this).cancelUniqueWork("study_reminder")` |
| `Session.notificationsOn()` luôn trả `true` | `SettingsActivity.bindNotificationsRow()` chỉ mở system settings, không ghi `KEY_NOTIFS` vào SharedPreferences. Default `KEY_NOTIFS = true`. Không có cách nào trong app để set `false` | Guard `if (!Session.notificationsOn(this)) return;` trong `HomeActivity.scheduleStudyReminder()` **không bao giờ trigger** — WorkManager luôn được enqueue | Trong `bindNotificationsRow()`: toggle `Session.setNotificationsOn()` trực tiếp hoặc đọc trạng thái permission thực tế (`NotificationManager.areNotificationsEnabled()`) thay vì dùng SharedPreferences flag |
| `StudyReminderWorker.doWork()` L36: `nm.notify()` không kiểm tra quyền | Nếu user revoke `POST_NOTIFICATIONS` permission: worker vẫn chạy đúng lịch, `nm.notify()` bị OS silently drop — không crash nhưng worker tiêu tốn battery vô ích | Battery drain nhỏ (1 lần/ngày); không crash | Thêm check: `if (!nm.areNotificationsEnabled()) return Result.success();` ở đầu `doWork()` |
| `HomeActivity.scheduleStudyReminder()` gọi trong `onCreate()` mỗi lần | `ExistingPeriodicWorkPolicy.KEEP` ngăn duplicate ✅. Nhưng nếu theme thay đổi → `SettingsActivity.recreate()` → HomeActivity recreate → `scheduleStudyReminder()` gọi lại | Không gây duplicate (KEEP policy). ✅ | Không cần sửa |
| `StudyReminderWorker` không có `onStopped()` override | Worker đơn giản, không có resource để giải phóng | ✅ Không có vấn đề | — |

---

## 3. Tổng hợp theo mức độ tác động

| Vị trí code | Vấn đề | Tác động | Mức độ |
|-------------|--------|----------|--------|
| `HistoryActivity.reload()` L139 — gọi `questionDao().all()` mỗi keystroke | Block main thread trên mỗi ký tự gõ search | Lag gõ phím / ANR potential | **Cao** |
| `SettingsActivity` logout không cancel WorkManager | Reminder tiếp tục sau logout | UX xấu (thông báo cho user đã logout) | **Cao** |
| `ChatActivity` L138: `questionDao().insert(q)` write trên main thread | Block main thread khi gửi tin đầu | Jank UI / ANR potential | Trung |
| `ChatActivity` L142: `messageDao().insert(userMsg)` write | Block main thread mỗi tin | Jank | Trung |
| `AnswerActivity` L162: `questionDao().update()` write | Block khi toggle bookmark | Jank | Trung |
| `HistoryActivity` L121: `questionDao().delete()` write | Block khi xóa item | Jank | Trung |
| `DashboardActivity` — 4 COUNT queries riêng lẻ | 4 sequential main-thread round-trips | Slow render Dashboard | Trung |
| `CameraActivity.flipCamera()` race condition | Multiple futures → potential camera black screen | Glitch UX | Trung |
| `CameraActivity` `FLAG_KEEP_SCREEN_ON` không clear khi pause | Màn hình sáng suốt phone call | Battery drain | Thấp |
| `Session.notificationsOn()` guard không hoạt động | WorkManager luôn enqueue dù setting off | Notification bị gửi không mong muốn | Thấp |
| `StudyReminderWorker` không check `areNotificationsEnabled()` | Worker chạy vô ích khi permission bị revoke | Battery drain nhỏ | Thấp |
| `ScanPreviewActivity` OCR thread giữ Activity reference ~1.6s | Memory leak tạm thời khi back nhanh | Không thấy được trên thiết bị | Thấp |
| `takePhoto()` callback sau Activity destroyed | `startActivity()` trên gone context | Unlikely crash | Thấp |
| `HomeActivity.onResume()` L192 read on main thread | Read mỗi navigate-back | Không rõ ràng với list nhỏ | Thấp |
| Glide trong ScanPreviewActivity | Lifecycle-aware, load 1 lần, không leak | ✅ Không có vấn đề | — |

---

## 4. Quick-fix checklist (theo thứ tự ưu tiên)

```
[ ] 1. HistoryActivity.reload() — cache list vào field, filter in-memory thay vì gọi DB
[ ] 2. SettingsActivity.showSignOutConfirm() — thêm WorkManager.cancelUniqueWork("study_reminder") trước startActivity
[ ] 3. ChatActivity L142 — wrap messageDao().insert(userMsg) trong executor.execute()
[ ] 4. AnswerActivity L162 — wrap questionDao().update(question) trong executor.execute()
[ ] 5. HistoryActivity L121 — wrap questionDao().delete(q) trong executor.execute() + runOnUiThread callback
[ ] 6. DashboardActivity — gộp 4 countBySubject() thành 1 GROUP BY query trong DAO
[ ] 7. CameraActivity.flipCamera() — disable btn_flip ngay khi tap, re-enable sau rebind
[ ] 8. CameraActivity.takePhoto() — thêm isDestroyed() check trong onImageSaved callback
[ ] 9. CameraActivity — clear FLAG_KEEP_SCREEN_ON trong onPause(), set lại onResume()
[ ] 10. StudyReminderWorker — thêm areNotificationsEnabled() check đầu doWork()
[ ] 11. Session.notificationsOn() — fix flow để thực sự set KEY_NOTIFS=false khi user tắt
[ ] 12. ScanPreviewActivity — thêm AtomicBoolean cancelled check trong OCR callback
```
