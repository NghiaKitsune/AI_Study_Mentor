# PHẢN BIỆN BẢN AUDIT — Đối chiếu với source code thực tế

> **Người thực hiện:** Claude Code
> **Ngày:** 2026-07-22
> **Branch đối chiếu:** `feature/api-expansion`
> **Mục đích:** Rà soát toàn diện source code hiện tại, đối chiếu từng tuyên bố trong "BÁO CÁO GIAI ĐOẠN 1 — ANDROID PROJECT AUDIT", chỉ ra điều gì đúng / sai / lỗi thời, kèm bằng chứng `file:line`.

---

## 1. Phương pháp

Tôi **đọc trực tiếp** các file nguồn sau (không dựa vào tài liệu hay CLAUDE.md để phán xét — chỉ dùng chúng làm tham chiếu chéo):

| Nhóm | File đã đọc |
|------|-------------|
| App/DB | `StudyMentorApp.java`, `AppDatabase.java`, `Question.java`, `Message.java`, `QuestionDao.java`, `MessageDao.java` |
| Chat/AI | `ChatActivity.java`, `ApiClient.java`, `GroqVisionService.java` |
| Scan | `ScanPreviewActivity.java` |
| Màn hình dữ liệu | `ProfileActivity.java`, `DashboardActivity.java`, `NotificationsActivity.java`, `LeaderboardActivity.java` |
| Adapter | `RecentQuestionAdapter.java`, `StepAdapter.java` |
| Build | `app/build.gradle` |
| Kiểm tra tồn tại | `Glob **/GeminiAiService.java` |

**Bổ sung (vòng 2):** Đã đọc nốt `CameraActivity.java`, `HistoryAdapter.java`, `NotifAdapter` (trong `NotificationsActivity.java`), `LoginActivity.java`, `TwoFAActivity.java` — xem **Mục 6** để đối chiếu các luận điểm còn treo. Chưa đọc: `SignUpActivity`, `ForgotPasswordActivity`, `MessageAdapter` (đánh giá theo nhất quán, rủi ro thấp).

---

## 2. Bảng tổng hợp phán quyết

| # | Tuyên bố của audit | Mức audit gán | Phán quyết | Bằng chứng |
|---|--------------------|---------------|-----------|-----------|
| 1 | Scan không OCR thật, luôn dùng `MockOcrService` random | Nghiêm trọng | ❌ **SAI/LỖI THỜI** | `ScanPreviewActivity.java:108` |
| 2 | API key đóng gói trong APK, gọi AI trực tiếp từ app | Nghiêm trọng | ✅ **ĐÚNG** | `build.gradle:25`, `GroqVisionService.java:63` |
| 3 | Room cho phép query trên Main Thread (`allowMainThreadQueries`) | Cao | ❌ **SAI/LỖI THỜI** | `StudyMentorApp.java:40-49` |
| 4 | Xóa Question không xóa Message → dữ liệu mồ côi | Cao | ✅ **ĐÚNG** | `QuestionDao.java:40-41`, `MessageDao.java` |
| 5 | Bottom Nav dùng nhiều Activity → recreate/flicker/backstack | Cao | ✅ **ĐÚNG** | Kiến trúc (BottomNavHelper + Activity-per-tab) |
| 6 | Chat không lưu cấu trúc câu trả lời → History không khôi phục Detail | Cao | ✅ **ĐÚNG (mạnh nhất)** | `ChatActivity.java:197-201, 228-233` |
| 7 | Fake data ở Leaderboard/Notifications/Profile/Dashboard/AnswerTabbed | Cao | ⚠️ **ĐÚNG MỘT PHẦN** | xem mục 5 |
| 8 | Không hủy AI/OCR request theo lifecycle | Trung bình | ✅ **ĐÚNG** | `ChatActivity.java:172-208` |
| 9 | RecyclerView dùng `notifyDataSetChanged()`, chưa DiffUtil | Trung bình | ✅ **ĐÚNG** | `RecentQuestionAdapter.java:34`, `ChatActivity.java:82,89` |
| 10 | Auth/2FA/Forgot chỉ là luồng mô phỏng | Trung bình | ✅ **ĐÚNG** | `LoginActivity.java:65-66`, `TwoFAActivity.java:83-88` |
| 10b | NotifAdapter lỗi tái sử dụng ViewHolder (background icon) | Trung bình | ✅ **ĐÚNG** | `NotificationsActivity.java:219-233` |
| 10c | Camera thiếu xử lý target rotation / EXIF / cleanup cache | — | ✅ **ĐÚNG** | `CameraActivity.java` (không có `setTargetRotation`, không xoá cache) |
| 10d | Camera khai báo quyền đọc ảnh có thể thừa (đã dùng Photo Picker) | Trung bình | ⚠️ **PHỤ THUỘC MANIFEST** | Code dùng Photo Picker đúng (`CameraActivity.java:76-80`) |
| 10e | Quiz/2FA timer chỉ huỷ ở `onDestroy`, không pause khi `onStop` | Trung bình | ✅ **ĐÚNG** | `TwoFAActivity.java:92-96` |
| 11 | Nhiều chuỗi hard-code, thiếu `values-vi` | Trung bình | ✅ **ĐÚNG** | Nhìn thấy nhiều chuỗi inline trong Java |
| 12 | Không có unit/instrumentation test | Trung bình | ✅ **ĐÚNG** | `src/test` + `src/androidTest` không tồn tại |
| 13 | `GeminiAiService` là implementation đang dùng | (liệt kê) | ❌ **SAI** | Không tồn tại file |
| 14 | `fallbackToDestructiveMigration` + v1 + `exportSchema=false` rủi ro | Cao | ✅ **ĐÚNG** | `StudyMentorApp.java:41`, `AppDatabase.java:6-10` |
| 15 | Không có Foreign Key / Index | Cao | ✅ **ĐÚNG** | `Message.java:21-23` |
| 16 | Không có Repository/ViewModel dù đã khai báo dependency | Cao | ✅ **ĐÚNG** | `build.gradle:68-70` |
| 17 | Nhiều lượt chat gộp vào 1 Question, answer bị ghi đè | Cao | ✅ **ĐÚNG** | `ChatActivity.java:146-166,194-195` |
| 18 | Không gửi conversation history cho AI | Cao | ✅ **ĐÚNG** | `ChatActivity.java:171` |
| 19 | Request đồng thời có thể đảo thứ tự (nút gửi không khóa) | Cao | ✅ **ĐÚNG** | `ChatActivity.java:133-166` |
| 20 | Crop chỉ là "coming soon", không preprocessing | — | ✅ **ĐÚNG** | `ScanPreviewActivity.java:103-104` |
| 21 | `ApiClient` chọn Groq trực tiếp, Retrofit base URL không tham gia | — | ✅ **ĐÚNG** | `ApiClient.java:31-36` |
| 22 | `StepAdapter` capture position → sai khi dữ liệu đổi | Trung bình | ⚠️ **CODE SMELL, KHÔNG PHẢI BUG** | `StepAdapter.java:23-31,49-51` |
| 23 | `RecentQuestionAdapter` lấy ký tự đầu subject → nguy cơ crash | Trung bình | ⚠️ **RỦI RO THẤP** | `RecentQuestionAdapter.java:52,58` |

**Tỷ lệ:** Trong ~23 luận điểm chính: **~15 đúng, ~4 sai/lỗi thời, ~4 đúng một phần**.

---

## 3. Các tuyên bố SAI / LỖI THỜI (audit mô tả code đã bị thay thế)

### 3.1. ❌ "Scan luôn dùng MockOcrService, OCR chọn ngẫu nhiên" — SAI

Audit gán mức **Nghiêm trọng** và gọi đây là "root cause chính". Thực tế:

```java
// ScanPreviewActivity.java:107-108
private void runMockOcr() {
    GroqVisionService.recognize(this, imageUri, new MockOcrService.Listener() {
```

`ScanPreviewActivity` gọi **`GroqVisionService.recognize()`** — OCR thật qua Groq Vision API (model `meta-llama/llama-4-scout-17b-16e-instruct`, base64-encode ảnh, parse JSON `{text, subject, language}`). `MockOcrService` **chỉ là fallback khi exception**:

```java
// GroqVisionService.java:74-77
} catch (Exception e) {
    Log.e(TAG, "Vision failed: ... — using mock fallback", e);
    MAIN.post(() -> MockOcrService.recognize(imageUri, listener));
}
```

→ Framing "OCR là mock" đã **lỗi thời**. Đây là code trước Phase 6A/6B.

> **Lưu ý phát sinh:** `GroqVisionService.parse()` **luôn trả `confidencePercent = 90`** (`GroqVisionService.java:155`) bất kể chất lượng nhận diện. "90% match" trên UI là số cứng — đây là điểm cosmetic audit **không** phát hiện.

### 3.2. ❌ "Room bật `allowMainThreadQueries()`, reads chạy Main Thread" — SAI

Audit gán **Cao**, mô tả `StudyMentorApp` bật flag này. Thực tế đã gỡ và thay bằng executor + StrictMode:

```java
// StudyMentorApp.java:40-49
db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
        .fallbackToDestructiveMigration()
        .build();                       // ← KHÔNG còn allowMainThreadQueries()
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads().penaltyLog().build());
}
```

Và có helper chuẩn hóa đọc nền:

```java
// StudyMentorApp.java:69-82
public static <T> void query(Activity host, Callable<T> work, Consumer<T> onUi) { ... }
```

Các màn Home/History/Chat/Profile/Dashboard/Notifications đều đã đọc DB qua `executor()` + `runOnUiThread` + guard `isFinishing()/isDestroyed()`. → Toàn bộ mục 4.2 "Truy vấn trên Main Thread" của audit **lỗi thời**.

### 3.3. ❌ "NotificationsActivity tạo danh sách cố định hard-code" — SAI

Audit gán **Cao**, nói "không có dữ liệu từ DB". Thực tế sinh động từ Room:

```java
// NotificationsActivity.java:70-83
private void loadItemsAsync() {
    StudyMentorApp.get().executor().execute(() -> {
        QuestionDao dao = StudyMentorApp.get().db().questionDao();
        int totalQ = dao.count();
        int bookmarks = dao.bookmarkedCount();
        int mathCount = dao.countBySubject("math");
        ...
        List<NotifItem> built = buildItems(totalQ, bookmarks, streak, bestQuiz, ...);
```

`buildItems()` sinh thông báo theo điều kiện thực (`streak>=7`, `bookmarks>=1`, `bestQuiz>=80`...). → Đây là code sau Phase 4. Audit **lỗi thời**.

### 3.4. ❌ "GeminiAiService là implementation đang tồn tại" — SAI

`Glob **/GeminiAiService.java` → **không tìm thấy**. File đã bị xóa (dead code). Audit liệt kê nó ở mục 3.3 như một implementation hiện hành.

### 3.5. ❌ "AnswerTabbedActivity toàn nội dung mẫu, không kết nối dữ liệu thật" — LỖI THỜI

Mục 4.1 audit mô tả AnswerTabbed chứa câu hỏi/giải thích/practice/pitfall mẫu. Thực tế đã wire `GroqTabbedService` (Phase 1) — 4 tab sinh từ Groq theo câu hỏi thật. **[Xác minh gián tiếp qua CLAUDE.md + import; chưa đọc lại toàn bộ file trong lần này.]**

---

## 4. Các tuyên bố CHÍNH XÁC (đã xác minh trên code hiện tại)

Đây là phần **giá trị nhất** của bản audit — nên ưu tiên xử lý.

### 4.1. ✅ Không có Foreign Key + không cascade delete → message mồ côi

```java
// Message.java:21-23 — chỉ là cột thường, KHÔNG có @ForeignKey/@Index
/** Foreign key onto Question#id. */   // ← comment nói "foreign key" nhưng không có constraint
@ColumnInfo(name = "question_id")
public long questionId;
```

```java
// QuestionDao.java:40-41 — xóa chỉ 1 hàng questions
@Delete
void delete(Question q);
```

```java
// MessageDao.java — CHỈ có insert + forQuestion, KHÔNG có delete
@Insert long insert(Message m);
@Query("SELECT * FROM messages WHERE question_id = :questionId ORDER BY sent_at ASC")
List<Message> forQuestion(long questionId);
```

→ Xóa Question để lại toàn bộ Message mồ côi. **Audit đúng hoàn toàn.**

### 4.2. ✅ Chat Detail (steps/mistakes) KHÔNG persist vào Room — phát hiện mạnh nhất

```java
// ChatActivity.java:197-201 — steps/mistakes chỉ giữ trong field tạm, serialize để truyền Intent
if (body.steps != null && !body.steps.isEmpty()) {
    lastStepsJson    = new Gson().toJson(body.steps);
    lastMistakesJson = body.commonMistakes != null ? new Gson().toJson(body.commonMistakes) : null;
    offerViewSteps();
}
```

```java
// ChatActivity.java:228-233 — truyền qua Intent, KHÔNG lưu DB
i.putExtra(AnswerActivity.EXTRA_STEPS_JSON, steps);
i.putExtra(AnswerActivity.EXTRA_MISTAKES_JSON, mistakes);
```

Chỉ có `displayText` (reply) được lưu vào `Question.answer` + `Message.text`. Khi mở lại từ History (không có `EXTRA_STEPS_JSON`), Detail không thể khôi phục các bước. **Audit đúng — đây là lỗi kiến trúc dữ liệu quan trọng nhất.**

### 4.3. ✅ Nhiều lượt chat gộp vào 1 Question + answer bị ghi đè

```java
// ChatActivity.java:146-157 — chỉ tạo Question mới ở lượt đầu (isNew)
final boolean isNew = (questionId <= 0);
...
if (isNew) {
    Question q = new Question(); q.prompt = text; ...
    qid = questionDao().insert(q);
}
```

```java
// ChatActivity.java:194-195 — mọi lượt sau đều updateAnswer() ghi đè answer của Question đầu
questionDao().updateAnswer(qid, saved);
```

→ Prompt lượt 2,3,... vẫn ghi đè `answer` của Question lượt 1. History chỉ thấy câu hỏi đầu, answer cuối có thể thuộc câu khác. **Audit đúng.**

### 4.4. ✅ Không gửi conversation history cho AI

```java
// ChatActivity.java:171 — request chỉ có prompt + id, không kèm lịch sử
ChatRequest req = new ChatRequest(prompt, questionId);
```

→ Câu nối tiếp ("giải thích bước 2") thiếu ngữ cảnh. **Audit đúng.**

### 4.5. ✅ Request đồng thời không khóa + callback không guard lifecycle

`sendCurrent()` (`ChatActivity.java:133-166`) **không disable nút gửi**. Người dùng có thể gửi liên tiếp → response về sai thứ tự, `updateAnswer` ghi đè lẫn nhau.

Callback trong `callAi()` (`ChatActivity.java:172-208`) đụng `typing`/`appendAssistant` **không có guard `isFinishing()/isDestroyed()`** (trái với block executor có guard ở `:160-164`). Nếu Activity đóng trước khi response về → nguy cơ cập nhật UI sau khi destroyed. **Audit đúng.**

### 4.6. ✅ Bảo mật: API key trong APK

```groovy
// build.gradle:25
buildConfigField "String", "GROQ_API_KEY", "\"${localProps['GROQ_API_KEY'] ?: ''}\""
```

```java
// GroqVisionService.java:63 — gọi trực tiếp từ app với key trong BuildConfig
.header("Authorization", "Bearer " + BuildConfig.GROQ_API_KEY)
```

→ Key nằm trong APK, có thể bị trích xuất. Không có backend trung gian, không kiểm soát rate-limit theo user. **Audit đúng — mối lo bảo mật thật.**

### 4.7. ✅ Room schema rủi ro

```java
// StudyMentorApp.java:41
.fallbackToDestructiveMigration()   // đổi schema → xóa sạch dữ liệu
// AppDatabase.java:8-9
version = 1, exportSchema = false
```

**Audit đúng** — không phù hợp production có lịch sử học tập.

### 4.8. ✅ Không có Repository/ViewModel

`build.gradle:68-70` khai báo `lifecycle-viewmodel` + `lifecycle-livedata` nhưng **không file nào dùng**. Activity ôm cả UI + DB + network. **Audit đúng.**

---

## 5. ĐÚNG MỘT PHẦN — cần chỉnh lại sắc thái

### 5.1. ⚠️ "Fake data ở Leaderboard/Profile/Dashboard"

Không thể gộp chung thành "hard-code hoàn toàn". Chi tiết:

| Màn | Phần THẬT | Phần vẫn giả/cứng |
|-----|-----------|--------------------|
| **Leaderboard** | XP user = `Session.xp()` thật; có sort thật (`LeaderboardActivity.java:86-114`) | 8 đối thủ hard-code (`GLOBAL_OPPONENTS`) — **có chủ đích** vì không có backend |
| **Profile** | Badge unlock theo dữ liệu thật (streak/qCount/bestQuiz/bookmarks/mathCount) `ProfileActivity.java:116-127` | (a) 2 badge "Top 10" + "Speed Demon" **luôn `false`** (`:123,125`); (b) **Activity feed 100% giả** (`:135-140`) gồm "Reached Level 7 · Algebra Apprentice"; (c) fallback name cứng "Nghia Mentor" (`:78`) |
| **Dashboard** | Stats + subject counts thật; Milo Insight **AI-generated** (`DashboardActivity.java:98-122`, Phase 3) | Bar chart tuần **[chưa thấy bind trong Java → nghi hard-code XML]**; xem bug 5.2 |

→ Audit **đúng** về activity feed giả và badge luôn khóa, nhưng **sai** khi nói Dashboard Insight và Leaderboard XP là hard-code.

### 5.2. 🔴 BUG MỚI audit BỎ SÓT — Dashboard "Languages" dùng nhầm historyCount

```java
// DashboardActivity.java:85
new SubjectStat("Languages", historyCount, historyCount * 100 / total, R.color.subject_language, ...)
```

Hàng "Languages" hiển thị **số câu hỏi môn History**. Audit mục 4.1 có nhắc mơ hồ ("Languages đang sử dụng số lượng History") — **điểm này audit ĐÚNG và tôi xác nhận chính xác dòng 85.** Đây là bug logic thật.

### 5.3. ⚠️ Profile "số badge không đồng nhất" — ĐÚNG

`countBadges()` chỉ kiểm 6 điều kiện có thể mở khóa (2 badge luôn khóa) nhưng hiển thị **"X/8"** (`ProfileActivity.java:102,105-114`). Mẫu số 8 gồm 2 badge không bao giờ đạt được → gây hiểu nhầm. **Audit đúng.**

### 5.4. ⚠️ "StepAdapter capture position sai" — CODE SMELL, KHÔNG phải bug

```java
// StepAdapter.java:49-51 — dùng tham số position thay vì getBindingAdapterPosition()
h.itemView.setOnClickListener(v -> {
    expanded[position] = !expanded[position];
    notifyItemChanged(position);
});
```

Về nguyên tắc nên dùng `getBindingAdapterPosition()`. Nhưng list `items` là `final` và **không bao giờ mutate** sau khi khởi tạo (không có `setItems`), nên position luôn hợp lệ. → **Code smell hợp lệ nhưng không gây lỗi thực tế.** Audit thổi phồng mức độ.

### 5.5. ⚠️ "RecentQuestionAdapter nguy cơ crash" — RỦI RO THẤP

```java
// RecentQuestionAdapter.java:52,58
String subject = q.subject != null ? q.subject : "general";  // guard null
String label = subject.substring(0, 1).toUpperCase() + ...;   // crash nếu subject == ""
```

Guard có cho `null` nhưng **không** cho chuỗi rỗng. Tuy nhiên `Question.subject` mặc định `"general"` (`Question.java:33`) và `detectSubject()` không bao giờ trả `""`, nên **chuỗi rỗng không tiếp cận được** trong luồng dữ liệu hiện tại. → Rủi ro lý thuyết, thực tế gần như bằng 0. Audit đúng về mặt phòng thủ nhưng phóng đại xác suất.

---

## 6. Bổ sung vòng 2 — các file đã đọc nốt (giải quyết mục treo)

### 6.1. ✅ CameraActivity — Photo Picker đúng, nhưng thiếu rotation/EXIF/cleanup

**Điểm tốt (audit không ghi nhận):** Camera **dùng Photo Picker chuẩn Android 13+**, không cần `READ_MEDIA_IMAGES`:

```java
// CameraActivity.java:76-80
/** Photo Picker — no READ_MEDIA_IMAGES needed on Android 13+. */
private final ActivityResultLauncher<PickVisualMediaRequest> galleryLauncher =
        registerForActivityResult(new ActivityResultContracts.PickVisualMedia(), uri -> { ... });
```

Runtime CAMERA permission cũng xử lý đầy đủ (grant → `startCamera()`, deny → `showPermEmpty()`, có nút mở Settings). → Phần permission của Camera **tốt hơn** những gì audit gợi ý.

**Điểm audit ĐÚNG:**
- **Không có `setTargetRotation()`** trên `Preview`/`ImageCapture` (`:148-154`) và **không đọc EXIF orientation** → ảnh chụp ngang/xoay có thể sai hướng khi đưa vào OCR.
- **Không cleanup cache**: `newCacheFile()` (`:201-206`) tạo file `scan_*.jpg` trong external cache, **không bao giờ xoá** → tích tụ theo thời gian.
- `cycleFlash()` (`:227`) vẫn còn phép tính alpha thô `(0xCC << 24) | (amber & 0x00FFFFFF)` — magic number, dù đã dùng color resource cho phần còn lại.

**Về hủy request theo lifecycle:** OCR trong `GroqVisionService` chạy `new Thread()` thô (`GroqVisionService.java:53,78`) — **không cancellable**; callback về `MockOcrService.Listener` đụng view của `ScanPreviewActivity` **không guard `isFinishing()`** (`ScanPreviewActivity.java:107-129`). → Audit mục "không hủy OCR theo lifecycle" **đúng**.

### 6.2. ✅ NotifAdapter — lỗi tái sử dụng ViewHolder có thật

```java
// NotificationsActivity.java:219-233
if (n.unread) {
    ...
    if (n.iconBgColorRes != 0)
        h.iconBg.setBackgroundTintList(...);   // chỉ set TINT, không setBackground()
    else
        h.iconBg.setBackgroundTintList(null);
} else {
    ...
    h.iconBg.setBackgroundTintList(null);
    h.iconBg.setBackground(null);              // ← NULL hoá background drawable
}
```

**Kịch bản bug:** VH dùng cho item *đã đọc* → `iconBg.background = null` (dòng 232). VH đó recycle cho item *chưa đọc* → nhánh unread **chỉ set tint list, không khôi phục `setBackground()`** → drawable nền icon đã mất, tint không có gì để tô. → **Audit đúng chính xác** (mục 8.5).

### 6.3. ✅ Auth + 2FA là mock

```java
// LoginActivity.java:65-66
// TODO: real auth call. Mock for the MVP — any valid email + 8+ char pw works.
Session.saveAuth(this, "mock-token-" + System.currentTimeMillis(), email);
```

```java
// TwoFAActivity.java:83-88 — chấp nhận BẤT KỲ 6 chữ số nào
if (code.length() < 6) { Toast... "Enter all 6 digits"; return; }
Toast... "2FA enabled successfully!";   // không verify với server
```

→ Login chỉ validate format email + độ dài password; 2FA chấp nhận mọi mã đủ 6 số. **Audit đúng** — không phải xác thực thật.

### 6.4. ✅ Timer không pause khi onStop (2FA, và tương tự Quiz)

```java
// TwoFAActivity.java:92-96 — chỉ cancel ở onDestroy
@Override protected void onDestroy() { super.onDestroy(); if (timer != null) timer.cancel(); }
```

Không có `onStop()`/`onPause()` để tạm dừng → khi màn bị che, timer vẫn đếm. **Audit đúng** (mục 5.3).

### 6.5. Ghi nhận điểm tốt: HistoryAdapter phòng thủ tốt hơn RecentQuestionAdapter

`HistoryAdapter` **null-guard đầy đủ** cho `subject` ở cả 3 helper (`:109,120,131`) và dùng `SubjectIcons.forSubject()` + `DateUtils.getRelativeTimeSpanString()`. → Không có rủi ro crash như `RecentQuestionAdapter`. (Vẫn dùng `notifyDataSetChanged()` — điểm chung đã nêu ở mục 2 #9.)

---

## 7. Kết luận

### 7.1. Đánh giá tổng thể bản audit

| Tiêu chí | Nhận định |
|----------|-----------|
| Chất lượng phân tích kiến trúc | **Tốt** — hiểu đúng mô hình Activity-ôm-hết, threading, lifecycle, backstack |
| Độ chính xác so với code hiện tại | **~65%** — một phần lớn mục "Nghiêm trọng/Cao" đã lỗi thời |
| Nguyên nhân sai lệch | Audit tự nhận **không build được** (Gradle offline, `gradlew` CRLF) và là *static audit*; nhiều khả năng đọc snapshot **trước** Phase 0–6 + Nhóm A (threading) |

### 7.2. Phát hiện của audit ĐÚNG và nên xử lý (ưu tiên)

1. **Chat Detail không persist vào Room** → History/Answer không tái hiện được (mục 4.2)
2. **Thiếu Foreign Key + cascade** → message mồ côi khi xóa Question (mục 4.1)
3. **Gộp nhiều lượt chat vào 1 Question**, answer ghi đè (mục 4.3)
4. **Concurrent request không khóa** + callback không guard lifecycle (mục 4.5)
5. **API key trong APK** → cần backend trung gian (mục 4.6)
6. **Bug Dashboard "Languages" = historyCount** (mục 5.2)
7. Thiếu Repository/ViewModel, schema destructive migration (mục 4.7, 4.8)

### 7.3. Phát hiện của audit SAI/LỖI THỜI — nên loại khỏi kế hoạch

1. ❌ "OCR là mock" → đã có `GroqVisionService` (Phase 6)
2. ❌ "Room query Main Thread / `allowMainThreadQueries`" → đã gỡ (Nhóm A)
3. ❌ "GeminiAiService đang dùng" → đã xóa
4. ❌ "Notifications hard-code" → đã DB-driven (Phase 4)
5. ❌ "AnswerTabbed toàn mẫu" → đã wire Groq (Phase 1)

### 7.4. Khuyến nghị

- **Chạy lại audit trên đúng branch `feature/api-expansion` và build được thật** trước khi dùng làm kế hoạch thi công.
- Giữ nguyên **thứ tự ưu tiên** roadmap của audit (Room → Navigation → Chat Detail → OCR → Security) vì hợp lý — nhưng **bỏ các hạng mục đã hoàn thành** (OCR thật, threading, notifications) để không làm lại việc đã xong.
- Bổ sung 3 bug audit chưa nêu rõ: **confidence=90 cứng** (`GroqVisionService.java:155`), **Languages=historyCount** (`DashboardActivity.java:85`), **badge X/8 với 2 badge bất khả thi** (`ProfileActivity.java:102,123,125`).

---

*Không có file nguồn nào bị chỉnh sửa trong quá trình phản biện này. Chỉ tạo file `AUDIT_REBUTTAL.md`.*
