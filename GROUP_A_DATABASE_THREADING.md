# Group A — Database Threading: Phân tích & Đánh giá

**Ngày hoàn thành:** 2026-07-22  
**Branch:** `feature/api-expansion`  
**Commit:** `8e961d8`  
**Phases:** A0 → A1 → A2 → A3 → A4  

---

## 1. Vấn đề ban đầu

`StudyMentorApp` khởi tạo Room với `.allowMainThreadQueries()` — tức là **mọi lệnh đọc database đều chạy trực tiếp trên UI thread**. Với data nhỏ thì không ai nhận ra, nhưng đây là anti-pattern nghiêm trọng: khi số câu hỏi/tin nhắn tăng lên, các query này sẽ block UI thread → giật lag → ANR (Application Not Responding).

Đây là **Known Stub #2a** trong CLAUDE.md từ đầu dự án, được đánh dấu "acceptable MVP stub; queries are small".

**Danh sách toàn bộ call site đọc DB trên main thread (đã grep xác nhận trước khi sửa):**

| Activity | Lệnh đọc | Ghi chú |
|----------|----------|---------|
| HomeActivity (×2) | `recent(5)` | `bindRecent()` + `onResume()` |
| HistoryActivity (×4) | `count()`, `bookmarkedCount()`, `all()` | stats + list + reload + miloNoticed |
| ProfileActivity (×3) | `count()`, `bookmarkedCount()`, `countBySubject("math")` | stats + badges |
| DashboardActivity (×5) | `count()`, 4×`countBySubject()` | liveStats + subject bars |
| NotificationsActivity (×6) | `count()`, `bookmarkedCount()`, 4×`countBySubject()` | buildItems |
| AnswerActivity | `byId(qid)` | bind câu hỏi + steps |
| AnswerTabbedActivity | `byId(qid)` | render 4 tabs |
| ChatActivity | `forQuestion(id)` | load hội thoại cũ |
| ChatActivity | `insert(q)` trả id, `insert(userMsg)` | ⚠️ write + đọc id ngay — trên main thread |

**Đã đúng chuẩn (không sửa):** HistoryActivity delete, ChatActivity `updateAnswer()` + `appendAssistant()`, AnswerActivity bookmark — đã bọc `executor().execute()` từ trước.

---

## 2. Thiết kế giải pháp

### Nguyên tắc

- **Không thêm thư viện mới** (không LiveData, không RxJava, không Coroutines — project Java thuần)
- **Tận dụng executor đã có** — `StudyMentorApp.executor()` là `Executors.newSingleThreadExecutor()`, đã dùng cho writes
- **Gom nhiều reads vào 1 executor pass** khi cùng màn hình cần nhiều giá trị — tránh nhiều lần context switch

### Hai pattern dùng trong codebase

**Pattern 1 — Màn đọc 1 giá trị:** dùng `StudyMentorApp.query()` helper

```java
StudyMentorApp.query(this,
    () -> dao.doSomething(),   // Callable<T> — chạy trên executor
    result -> { /* bind UI */ } // Consumer<T> — chạy trên UI thread
);
```

**Pattern 2 — Màn đọc nhiều giá trị:** gom trong 1 executor block

```java
StudyMentorApp.get().executor().execute(() -> {
    int a = dao.count();
    int b = dao.countBySubject("math");
    // ... đọc hết các giá trị cần
    runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) return;
        // bind tất cả vào views
    });
});
```

**Tại sao Pattern 2 không dùng `query()` nhiều lần?**  
Vì `query()` enqueue 1 task riêng cho mỗi lần gọi → n giá trị = n task = n lần context switch = n lần `runOnUiThread`. Gom 1 block: 1 task, 1 switch, 1 bind — đơn giản và hiệu quả hơn.

---

## 3. Phase A0 — Helper hạ tầng

### Thay đổi: `StudyMentorApp.java`

Thêm static generic method:

```java
public static <T> void query(Activity host, Callable<T> work, Consumer<T> onUi) {
    get().executor().execute(() -> {
        final T result;
        try { result = work.call(); }
        catch (Exception e) { Log.e("Db", "query failed", e); return; }
        host.runOnUiThread(() -> {
            if (!host.isFinishing() && !host.isDestroyed()) onUi.accept(result);
        });
    });
}
```

**Điểm quan trọng của helper:**
- `Callable<T>` — DB work unit, có thể throw Exception
- Nếu DB lỗi → log + return, không crash app
- `isFinishing() || isDestroyed()` — guard tránh crash khi user đã back ra khỏi Activity trong lúc DB đang xử lý
- `java.util.function.Consumer` — Java 8 API, available vì minSdk 33

---

## 4. Phase A1 — 7 Activities → background reads

### HomeActivity

**`bindRecent()`** (gọi từ `onCreate`):
```java
// Trước: adapter = new QuestionAdapter(questionDao().recent(5));
// Sau:
adapter = new QuestionAdapter(new ArrayList<>());
rv.setAdapter(adapter);
StudyMentorApp.query(this,
    () -> db().questionDao().recent(5),
    items -> adapter.setItems(items));
```

**`onResume()`**: tương tự — async `recent(5)` → `adapter.setItems()`. Cần thiết vì khi quay lại từ ChatActivity/HistoryActivity, list phải refresh.

### HistoryActivity

4 call site, chia thành 2 block:

**`bindStats()`** — gom 2 reads:
```java
executor().execute(() -> {
    int total = dao.count();
    int bookmarked = dao.bookmarkedCount();
    runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) return;
        text_total.setText(String.valueOf(total));
        text_bookmarked.setText(String.valueOf(bookmarked));
    });
});
```

**`reload()`** — async `all()` → filter in-memory → `adapter.setItems()`. `bindMiloNoticed()` cũng dùng async `count()` để quyết định show/hide card.

### ProfileActivity

3 reads gom trong 1 block (stats + badges cần cùng data):
```java
executor().execute(() -> {
    int total = dao.count();
    int bookmarked = dao.bookmarkedCount();
    int mathCount = dao.countBySubject("math");
    runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) return;
        bindStats(total);
        bindBadges(total, bookmarked, mathCount);
    });
});
```

### DashboardActivity

5 reads gom trong 1 block — sau đó mới gọi Groq insight:
```java
executor().execute(() -> {
    int total = dao.count();
    int math = dao.countBySubject("math");
    int science = dao.countBySubject("science");
    int code = dao.countBySubject("code");
    int history = dao.countBySubject("history");
    runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) return;
        bindLiveStats(total);
        bindSubjects(math, science, code, history);
        bindMiloInsight(total, math, science, code, history);
    });
});
```

### NotificationsActivity

6 reads gom → dựng `List<NotifItem>` hoàn chỉnh trên background thread (không đụng View), rồi mới `setItems()` trên UI thread:
```java
executor().execute(() -> {
    int total = dao.count();
    int bookmarked = dao.bookmarkedCount();
    int math = dao.countBySubject("math"); // ... 4 subjects
    List<NotifItem> items = NotificationGenerator.generate(ctx, total, bookmarked, ...);
    runOnUiThread(() -> {
        if (isFinishing() || isDestroyed()) return;
        adapter.setItems(items);
    });
});
```

### AnswerActivity

`byId(qid)` async → toàn bộ chuỗi bind trong callback:
```java
StudyMentorApp.query(this,
    () -> db().questionDao().byId(questionId),
    q -> {
        if (q == null) { finish(); return; }
        text_question.setText(q.prompt);
        bindSteps(stepsJson);
        bindMistakes(mistakesJson);
        bindFollowUps(q.subject);
        bindBookmark(q.bookmarked);
        bindDeepDive();
    });
```
Hiện view "—" / placeholder tạm cho đến khi data về.

### AnswerTabbedActivity

Tương tự AnswerActivity — `byId(qid)` async → render header câu hỏi trong callback.

---

## 5. Phase A2 — ChatActivity: trường hợp phức tạp nhất

ChatActivity có 2 vấn đề đặc biệt không gặp ở các màn khác.

### 5.1 Load hội thoại cũ (`onCreate`)

Khi mở với `EXTRA_QUESTION_ID` (tiếp tục hội thoại cũ):

```java
// Trước: messages.addAll(messageDao().forQuestion(questionId));
// Sau:
StudyMentorApp.query(this,
    () -> db().messageDao().forQuestion(qid),
    loaded -> {
        messages.addAll(loaded);
        adapter.notifyDataSetChanged();
        layoutSuggestions.setVisibility(View.GONE);
        scrollToBottom();
    });
```

### 5.2 Gửi tin nhắn — Optimistic UI pattern

Đây là trường hợp phức tạp nhất vì có **write đọc-sau-ghi** (cần id từ `insert(q)` để dùng ngay cho `insert(userMsg)` và `callAi()`), đồng thời muốn UI phản hồi **ngay lập tức** không chờ DB.

**Luồng sau khi sửa:**

```
[UI thread] User tap Send
  1. Lấy text từ input, clear input
  2. Tạo Message userMsg = Message.user(questionId, text)  ← tạm dùng questionId hiện tại
  3. messages.add(userMsg); adapter.notifyItemInserted()   ← bubble xuất hiện NGAY (optimistic)
  4. scrollToBottom()

[executor thread]
  5. Nếu hội thoại mới (questionId <= 0):
       Question q = new Question(); q.prompt = text; q.subject = detectSubject(text);
       long qid = questionDao().insert(q);   ← lấy real id
       userMsg.questionId = qid;             ← fix field trên background thread (safe vì UI chưa dùng lại)
  6. messageDao().insert(userMsg)

[runOnUiThread]
  7. questionId = finalQid;   ← cập nhật field TRÊN UI THREAD (tránh race condition)
  8. callAi(text)             ← gọi Groq API
```

**Tại sao phải cẩn thận với `questionId`?**

`questionId` là instance field của Activity, được đọc từ cả UI thread (`callAi`, `offerViewSteps`) và executor (trong `insert(q)` để kiểm tra `isNew`). Nếu executor gán `this.questionId = qid` trực tiếp từ background → **data race** (không thread-safe).

Giải pháp: capture `final long finalQid = qid` trên background, gán `questionId = finalQid` chỉ trong `runOnUiThread()` sau khi DB write xong. Lúc đó mới `callAi()` — đảm bảo `questionId` đã đúng trước khi AI response về.

---

## 6. Phase A3 — Gỡ flag + StrictMode

### Xóa `.allowMainThreadQueries()`

```java
// Trước:
db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
        .fallbackToDestructiveMigration()
        .allowMainThreadQueries()   // ← XÓA
        .build();

// Sau:
db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
        .fallbackToDestructiveMigration()
        .build();
```

Room bây giờ ném `IllegalStateException: Cannot access database on the main thread` nếu bất kỳ call site nào bị bỏ sót. Đây là safety net ở framework level.

### Thêm StrictMode

```java
// Enable StrictMode AFTER one-time init (SharedPrefs first-access triggers disk check)
if (BuildConfig.DEBUG) {
    StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
            .detectDiskReads()
            .penaltyLog()
            .build());
}
```

StrictMode log mọi disk read trên main thread — bắt được vi phạm tương lai ngay trong dev/test, trước khi chúng trở thành ANR trên production.

### Bug phát sinh khi test — False positive (vòng 1)

**Triệu chứng:** Chạy lần đầu xuất hiện 3 `DiskReadViolation` tại:
```
Session.p() → PreferenceManager.getDefaultSharedPreferences() → File.exists()
```

**Root cause:** Đây không phải lỗi DB. Đây là hành vi của Android: lần đầu tiên gọi `getDefaultSharedPreferences()`, hệ điều hành kiểm tra xem file prefs đã tồn tại chưa bằng `File.exists()` — đây là disk read. Sau lần đầu, prefs object được cache trong memory, các lần tiếp theo không còn disk read nữa.

StrictMode được bật trước `Session.themeMode()` → bắt được lần đầu này → false positive.

**Fix:** Chuyển `StrictMode.setThreadPolicy()` xuống sau `Session.themeMode()` và `Room.databaseBuilder().build()`:

```java
@Override
public void onCreate() {
    super.onCreate();
    instance = this;
    executor = Executors.newSingleThreadExecutor();
    AppCompatDelegate.setDefaultNightMode(Session.themeMode(this));  // ← SharedPrefs first-access xảy ra ở đây
    db = Room.databaseBuilder(this, AppDatabase.class, "studymentor.db")
            .fallbackToDestructiveMigration()
            .build();
    // StrictMode AFTER init — catches future violations, not one-time prefs init
    if (BuildConfig.DEBUG) {
        StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                .detectDiskReads()
                .penaltyLog()
                .build());
    }
}
```

Kết quả vòng 2: **0 violations**.

---

## 7. Phase A4 — Dead code removal + Commit

### Files bị xóa

| File | Lý do |
|------|-------|
| `api/GeminiAiService.java` | Đã thay bởi `GroqAiService` từ Phase trước |
| `api/GeminiVisionService.java` | Đã thay bởi `GroqVisionService` (Gemini quota=0) |
| `ui/MainActivity.java` | Entry point cũ, không còn dùng |
| `res/layout/activity_main.xml` | Layout đi kèm MainActivity |
| AndroidManifest `<activity .MainActivity>` | Entry point đã xóa |

### Tài liệu cập nhật

- `CLAUDE.md` — Stub #2a → RESOLVED, Stub #1 → RESOLVED, Rule #8 updated, Group A entry + Session Log
- `PLANNING.md` — Group A → ✅ HOÀN THÀNH

---

## 8. Kết quả test

### TEST_PHASE_A1.md — 7 Activities
- 7/7 màn PASS
- 0 `Cannot access database on the main thread`
- 0 `IllegalStateException`
- Frame skips chỉ từ emulator GPU (surface creation) — không liên quan DB

### TEST_PHASE_A2.md — ChatActivity
- Optimistic bubble: xuất hiện trong 1 frame sau khi tap Send ✅
- Groq AI response nhận được sau ~15s ✅
- XP tăng đúng sau response ✅
- 0 DB violations

### TEST_PHASE_A3.md — 8 màn sau khi bỏ flag
- 0 `StrictMode policy violation`
- 0 `Cannot access database on the main thread`
- 0 `FATAL EXCEPTION`
- Groq AI vẫn hoạt động bình thường

---

## 9. Tổng kết

| Hạng mục | Trước Group A | Sau Group A |
|----------|---------------|-------------|
| DB reads | Main thread (`allowMainThreadQueries`) | Background executor |
| DB writes | Background executor (đã đúng) | Không đổi |
| UI blocking | Có thể xảy ra khi data lớn | Không thể — non-blocking |
| Room enforcement | Tắt (flag override) | Bật — throw ngay nếu vi phạm |
| StrictMode | Không có | Bật trong DEBUG |
| Dead code | 4 file không dùng | Đã xóa |
| Known Stub #2a | Pending | ✅ RESOLVED |
| Known Stub #1 | Pending | ✅ RESOLVED (Phase 5 trước đó) |

**Commit:** `8e961d8` — 16 files changed, 370 insertions(+), 597 deletions(−)  
(xóa nhiều hơn thêm — codebase gọn hơn sau khi bỏ dead code)
