# AI Study Mentor — Next Steps Planning

> Tạo ngày 2026-06-12. File này dùng để Agent/Claude tham khảo trước khi bắt đầu session mới.
> Cập nhật sau mỗi phase hoàn thành.

---

## Trạng thái hiện tại (sau Phase A–D)

| Hạng mục | Trạng thái |
|----------|-----------|
| 22 màn hình Activity | ✅ Hoàn thành |
| Room DB (Question + Message) | ✅ Có data thực |
| Quiz (25 câu JSON) | ✅ Timer + reveal + score |
| Streak / XP / Badges | ✅ Logic thực |
| MockAiService | ✅ Subject-aware |
| Common mistakes | ✅ Dynamic từ AI |
| WorkManager reminder | ✅ Daily notification |
| Dark mode persist | ✅ |
| History search + delete | ✅ |
| Background DB writes | ✅ ExecutorService |
| ProGuard rules | ✅ |
| **Real API** | ❌ Vẫn dùng Mock |
| **Leaderboard data** | ❌ Fake strings |
| **Release APK tested** | ❌ Chưa verify |

---

## Phase 5 — UX Polish & Navigation Fix
**Mục tiêu:** Sửa các lỗi UX nhỏ còn sót, cải thiện trải nghiệm điều hướng.
**Ưu tiên: MEDIUM | Độ khó: Thấp**

### 5A — Back stack fix
**Vấn đề:** Bấm back nhiều lần từ Quiz/History có thể loop về Home nhiều lần.
**Cách sửa:**
- `BottomNavHelper.setup()`: thêm `FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_SINGLE_TOP` khi switch tab
- `HomeActivity`: thêm `finishAffinity()` thay vì `finish()` nếu cần
- **File:** `util/BottomNavHelper.java`

### 5B — Loading skeleton trong ChatActivity
**Vấn đề:** Khi AI đang trả lời chỉ có "Milo is thinking..." text, không có visual feedback tốt.
**Cách làm:**
- Thêm `item_message_typing.xml` — 3 chấm nhấp nháy (animated dots)
- `MessageAdapter` thêm viewType `TYPE_TYPING`
- Khi `callAi()` → insert typing message, khi có response → replace bằng real message
- **Files:** `adapter/MessageAdapter.java`, `res/layout/item_message_typing.xml`, `res/anim/`

### 5C — Swipe-to-refresh History
**Vấn đề:** History không có cách reload ngoài back và vào lại.
**Cách làm:**
- Bọc RecyclerView trong `SwipeRefreshLayout`
- `setOnRefreshListener` → gọi `reload()` + `bindStats()` + `setRefreshing(false)`
- **File:** `res/layout/activity_history.xml`, `HistoryActivity.java`
- **Dependency đã có:** `androidx.swiperefreshlayout` (có trong constraintlayout transitive)

### 5D — Empty state cho Chat history (reload khi resume)
**Vấn đề:** Mở ChatActivity từ History để xem conversation cũ — nếu không có message thì trắng.
**Cách làm:** Guard `if (messages.isEmpty())` trong `onResume` của `ChatActivity` khi load existing question.

---

## Phase 6 — Leaderboard Dynamic Data
**Mục tiêu:** Leaderboard không dùng fake strings cứng, có cảm giác dynamic dù vẫn là mock.
**Ưu tiên: LOW | Độ khó: Thấp**

### 6A — Seed leaderboard từ user data thực
**Cách làm:**
- Đọc `questionDao().count()` và `Session.streak()` làm base score của "You"
- Generate 9 fake players xung quanh score của user (±10–40%) bằng `Random` seed cố định
- Sort descending → highlight row của "You"
- **File:** `LeaderboardActivity.java`

### 6B — Share quiz result
**Vấn đề:** `QuizResultActivity` có nút Share nhưng chỉ share text đơn giản.
**Cách làm:**
- Build share string: "I scored X/Y (Z%) on AI Study Mentor! 🎉"
- Thêm subject name vào share text
- **File:** `QuizResultActivity.java` — `shareResult()` method

---

## Phase 7 — Content Expansion
**Mục tiêu:** Mở rộng nội dung học tập, thêm tính năng giúp user học hiệu quả hơn.
**Ưu tiên: MEDIUM | Độ khó: Trung bình**

### 7A — Mở rộng quiz_questions.json
**Hiện tại:** 25 câu (5/môn × 5 môn)
**Mục tiêu:** Tăng lên 50 câu (10/môn) — thêm difficulty level (easy/medium/hard)
**Thay đổi:**
- `quiz_questions.json`: thêm field `"difficulty": "easy"|"medium"|"hard"`
- `QuizQuestion.java`: thêm field `String difficulty`
- `QuizActivity`: hiện difficulty badge cạnh subject tag
- `QuizDataSource.random()`: thêm param `difficulty` (nullable = all)

### 7B — Daily Challenge
**Tính năng:** Mỗi ngày 1 câu hỏi đặc biệt trên HomeActivity
**Cách làm:**
- Dùng ngày hiện tại làm seed: `new Random(dayOfYear).nextInt(questions.size())`
- Thêm card "Daily Challenge" vào `activity_home.xml` (giữa streak và quick-start tiles)
- Bấm vào → mở QuizActivity với đúng câu đó
- Lưu `KEY_DAILY_DONE = true` vào Session khi hoàn thành, reset mỗi ngày mới
- **Files:** `activity_home.xml`, `HomeActivity.java`, `Session.java`, `QuizDataSource.java`

### 7C — Bookmark từ AnswerActivity
**Vấn đề:** Không có UI để bookmark ngay từ màn answer.
**Cách làm:**
- Thêm icon bookmark (toggle) vào toolbar của `AnswerActivity`
- Click → `questionDao().setBookmark(id, !current)` trên executor
- Sync với `HistoryActivity` khi resume
- **Files:** `AnswerActivity.java`, `activity_answer.xml`, `QuestionDao.java`

---

## Phase 8 — Release Readiness
**Mục tiêu:** Đảm bảo app có thể build Release và chạy đúng.
**Ưu tiên: HIGH (trước khi nộp bài) | Độ khó: Trung bình**

### 8A — Test assembleRelease
**Việc cần làm:**
```powershell
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat assembleRelease
```
- Install APK lên emulator, test các flow: Login → Home → Chat → Answer → Quiz → History
- Nếu crash → đọc logcat, likely ProGuard strip Room/Gson → thêm rule vào `proguard-rules.pro`
- **Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

### 8B — Network error handling
**Vấn đề:** ChatActivity có Snackbar khi `onFailure`, nhưng các màn khác không check.
**Cách làm:**
- Tạo helper `UiHelper.showNetworkError(View anchor)` → Snackbar với message `R.string.error_network`
- Dùng trong: `ChatActivity.onFailure` (đã có), kiểm tra các Retrofit call khác
- Thêm string: `<string name="error_network">No internet connection. Please try again.</string>`

### 8C — Move all DB reads to background (optional, nếu có thời gian)
**Vấn đề:** `allowMainThreadQueries()` vẫn còn — reads chạy trên main thread
**Cách làm full:**
- Remove `allowMainThreadQueries()` khỏi `StudyMentorApp`
- Wrap tất cả `db().questionDao().*` calls trong `executor.execute()` + `runOnUiThread()` callback
- **Ưu tiên thấp** — chỉ cần nếu muốn hoàn thiện technical chất lượng cao

### 8D — Accessibility pass
**Việc cần làm:**
- Kiểm tra tất cả `ImageView` có `contentDescription` không
- Icon buttons (bell, search, camera) cần có `android:contentDescription`
- Quiz option cards cần announce đúng khi chọn
- **Files:** Scan toàn bộ `res/layout/*.xml`

---

## Phase 9 — Real API Integration (Stretch Goal)
**Mục tiêu:** Thay MockAiService bằng AI thực (nếu có backend).
**Ưu tiên: LOW (BTEC assignment dùng mock là đủ) | Độ khó: Cao**

### 9A — Swap MockAiService → Real API
**Thay đổi:**
1. `app/build.gradle`: `USE_MOCK_AI = "false"`, `API_BASE_URL = "https://your-api.com/"`
2. `api/ApiClient.java`: đọc `BuildConfig.USE_MOCK_AI` để chọn implementation
3. Tạo `RealAiService.java` implement cùng interface với `MockAiService`
4. Backend cần endpoint: `POST /chat` nhận `{prompt, questionId}` trả về `{reply, steps[], commonMistakes[]}`

### 9B — Auth token real
**Thay đổi:**
- `LoginActivity.onSuccess` → lưu real JWT token vào `Session.saveToken()`
- `ApiClient` thêm `OkHttp Interceptor` inject `Authorization: Bearer <token>` header
- **File:** `api/ApiClient.java`

---

## Thứ tự thực hiện đề xuất

```
Ngay tiếp theo:
  Phase 8A (Release test) ← quan trọng nhất trước khi nộp
  Phase 5A (Back stack)   ← bug thực
  Phase 5C (Swipe refresh)← UX nhỏ, nhanh

Nếu còn thời gian:
  Phase 7B (Daily Challenge) ← tính năng ấn tượng cho demo
  Phase 7C (Bookmark từ Answer)
  Phase 6A (Leaderboard dynamic)

Stretch:
  Phase 5B (Typing animation)
  Phase 7A (Thêm quiz questions)
  Phase 8C (Full background reads)
  Phase 9 (Real API)
```

---

## Files quan trọng cần đọc khi bắt đầu session mới

| File | Lý do |
|------|-------|
| `.claude/CLAUDE.md` | Toàn bộ context, conventions, session log |
| `.claude/PLANNING.md` | File này — roadmap tiếp theo |
| `util/Session.java` | Keys + accessors cho SharedPreferences |
| `StudyMentorApp.java` | Singleton DB + Executor |
| `data/AppDatabase.java` | Schema Room, version |
| `data/QuestionDao.java` | Tất cả DB queries hiện có |