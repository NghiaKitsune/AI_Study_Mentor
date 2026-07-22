# AI Study Mentor — Kế hoạch nâng cấp API Integration + XP System

> **Tạo ngày:** 2026-07-16 | **Cập nhật sau mỗi phase hoàn thành.**
> Session mới: đọc bảng Tiến trình trước, rồi xem phase tiếp theo cần làm.
> Plan đầy đủ tại: `C:\Users\ADMIN\.claude\plans\quizzical-crafting-beaver.md`

---

## Tiến trình

| Phase | Tên | Trạng thái | Session | Commit |
|-------|-----|-----------|---------|--------|
| **0** | XP System (Chat +50, Quiz +500) | ✅ Hoàn thành | 2026-07-16 | 844053c |
| **1** | AnswerTabbedActivity — dữ liệu thật | ✅ Hoàn thành | 2026-07-17 | e38c737 |
| **2A** | GroqQuizService — sinh câu hỏi AI | ✅ Hoàn thành | 2026-07-17 | 3c2354d |
| **2B** | Wire Quiz AI vào QuizActivity UI | ✅ Hoàn thành | 2026-07-17 | 3c2354d |
| **3** | Dashboard Milo Insight AI-generated | ✅ Hoàn thành | 2026-07-16 | b20b37b |
| **4** | Notifications DB-driven (không fake) | ✅ Hoàn thành | 2026-07-17 | 38e7c1f |
| **5** | Leaderboard local simulation | ✅ Hoàn thành | 2026-07-17 | 935da6d |
| **6A** | GroqVisionService — OCR thật (Groq thay Gemini) | ✅ Hoàn thành | 2026-07-17 | b24c6cd |
| **6B** | Wire OCR vào ScanPreviewActivity | ✅ Hoàn thành | 2026-07-17 | b24c6cd |
| **A** | Database Threading — bỏ `allowMainThreadQueries` | ✅ Hoàn thành | 2026-07-22 | (xem bên dưới) |

**Legend:** ⬜ Chưa làm · 🔄 Đang làm · ✅ Hoàn thành · ❌ Blocked

---

## ✅ TẤT CẢ PHASES HOÀN THÀNH — 2026-07-17

**Tested:** Phase 6 OCR với ảnh thật → model đọc đúng "2x + 5 = 17", detect subject=math, language=en.  
**Backend cuối:** `GroqVisionService` / `meta-llama/llama-4-scout-17b-16e-instruct` (Groq free tier).  
**Gemini bị loại:** cả 2 key đều quota=0 / hết credit.  
**Test report:** `TEST_PHASE_6_OCR.md`

---

## Nhóm A — Database Threading (bỏ `allowMainThreadQueries`) ✅ HOÀN THÀNH — 2026-07-22

> **Tạo ngày:** 2026-07-20 | Mục tiêu: chuẩn hoá DB access cho app chạy lâu dài (production-ready).
> Giải quyết Known Stub #2a (CLAUDE.md). Không đổi schema (đó là Nhóm B).

### Bối cảnh
`StudyMentorApp` đang khởi tạo Room với `allowMainThreadQueries()` → **mọi lệnh đọc DB chạy trên UI thread**. Data nhỏ thì không thấy vấn đề, nhưng khi câu hỏi/tin nhắn tăng → block UI thread → giật lag → ANR. Nhóm A chuyển toàn bộ reads sang `executor()`, cập nhật UI qua `runOnUiThread()`, rồi gỡ `allowMainThreadQueries()`.

### Bản đồ call site đọc DB (đã grep xác nhận)

| Activity | Dòng | Lệnh đọc | Ghi chú |
|----------|------|----------|---------|
| HomeActivity | 110, 156 | `recent(5)` | `bindRecent()` + `onResume()` |
| HistoryActivity | 62,63,101,135,174 | `count`,`bookmarkedCount`,`all` | stats + list + reload + miloNoticed |
| ChatActivity | 77 | `forQuestion(id)` | nạp hội thoại cũ |
| ChatActivity | 139,143 | `insert(q)` trả id, `insert(userMsg)` | ⚠️ write đọc-sau-ghi trên main thread |
| ProfileActivity | 58,94,95,113-115 | `count`,`bookmarkedCount`,`countBySubject` | stats + badges |
| AnswerActivity | 50 | `byId(qid)` | bind text/steps ngay sau |
| AnswerTabbedActivity | 41 | `byId(qid)` | nạp câu hỏi |
| DashboardActivity | 45,55-58,106,111-114 | `count`, 4× `countBySubject` | liveStats + subjects |
| NotificationsActivity | 77-85 | `count`,`bookmarkedCount`,4× `countBySubject` | buildItems |

**Đã đúng chuẩn (không sửa):** HistoryActivity delete, ChatActivity updateAnswer + appendAssistant, AnswerActivity bookmark — đã bọc `executor().execute()`.

### Thiết kế — helper chuẩn hoá
Thêm method tĩnh vào `StudyMentorApp`:
```java
public static <T> void query(Activity host, Callable<T> work, Consumer<T> onUi) {
    get().executor().execute(() -> {
        final T result;
        try { result = work.call(); } catch (Exception e) { Log.e("Db","query failed",e); return; }
        host.runOnUiThread(() -> { if (!host.isFinishing() && !host.isDestroyed()) onUi.accept(result); });
    });
}
```
- Màn 1 giá trị → `StudyMentorApp.query(...)`.
- Màn nhiều giá trị (Dashboard/Profile/Notifications/History stats) → raw `executor().execute()` gom hết read trong 1 lượt, `runOnUiThread` bind + guard `isFinishing()/isDestroyed()`.
- Adapter khởi tạo list rỗng trước, set vào RecyclerView, rồi async `setItems()`.

### Các Phase
- **A0 — Helper hạ tầng:** thêm `query(...)` vào `StudyMentorApp.java` + build check.
- **A1 — Màn chỉ đọc (làm trước, rủi ro thấp):** Home → History → Profile → Dashboard → Notifications → Answer → AnswerTabbed. Mỗi màn build + smoke test riêng.
- **A2 — ChatActivity (phức tạp đọc-sau-ghi):** onCreate `forQuestion` async; `sendCurrent()` tái cấu trúc optimistic UI + insert trong executor (capture id, gán field trên UI thread) → `callAi`.
- **A3 — Gỡ flag + StrictMode:** xoá `.allowMainThreadQueries()`; bật `StrictMode` ở debug; fix call site sót (ném `Cannot access database on the main thread`).
- **A4 — Build & Test:** `assembleDebug` + smoke test 8 màn trên `Medium_Phone`; logcat CLEAN, không ANR; cập nhật CLAUDE.md (Stub #2a → RESOLVED).

### Files sẽ sửa
`StudyMentorApp.java` (helper + bỏ flag + StrictMode) · 8 Activity: Home, History, Chat, Profile, Answer, AnswerTabbed, Dashboard, Notifications. **Không đụng DAO/entity/schema** (Nhóm B).

### Verification
Build PASS · smoke test 8 màn không crash/ANR · regression: History delete/search/filter, Chat gửi + reload hội thoại cũ, Answer bookmark vẫn chạy · logcat không có `IllegalStateException` DB main-thread.

---

## Phân tích: Tại sao mỗi feature cần API?

| Feature | Vấn đề hiện tại | Giải pháp |
|---------|----------------|-----------|
| XP & Level | Tính từ `totalQuestions * 10` (sai) | Session lưu XP thật; Chat/Quiz cộng điểm |
| AnswerTabbedActivity | 4 tab hardcoded "sky blue" cũ | Groq sinh nội dung 4 tab theo câu hỏi thật |
| QuizActivity | 25 câu cố định, lặp lại | Groq sinh MCQ mới theo subject + level |
| Dashboard Milo Insight | Text hardcoded trong XML | Groq tóm tắt stats của user thành insight |
| NotificationsActivity | 6 item giả hardcoded | Room DB → sinh thông báo có nghĩa |
| LeaderboardActivity | 9 tên giả hardcoded | Tính XP thật + simulate opponents |
| ScanPreviewActivity | MockOcrService trả random text | Gemini Vision API đọc ảnh thật |

---

## Phase 0 — XP System (1 session) ⬜

**Mục tiêu:** +50 XP mỗi lần chat thành công, +500 XP mỗi lần hoàn thành quiz.

### Thang bậc
```
Level 1:      0 –    999 XP  "Beginner"
Level 2:  1,000 –  2,999 XP  "Explorer"
Level 3:  3,000 –  5,999 XP  "Scholar"
Level 4:  6,000 –  9,999 XP  "Expert"
Level 5: 10,000+        XP  "Master"
```

### Files thay đổi
| File | Thay đổi |
|------|---------|
| `util/Session.java` | Thêm `KEY_XP`, `KEY_XP_EARNED_IDS`; method `xp()`, `addXp(ctx, amount, qId)`, `levelNumber()`, `levelTitle()` |
| `ui/ChatActivity.java` | Sau `appendAssistant()`: `Session.addXp(this, 50, questionId)` |
| `ui/QuizActivity.java` | Trong `openResult()`: `Session.addXp(this, 500, System.currentTimeMillis())` |
| `ui/ProfileActivity.java` | Đọc `Session.xp()` thay vì `totalQuestions * 10`; dùng `Session.levelTitle()` |

### Quy tắc chống farming
`Session.addXp()` kiểm tra `KEY_XP_EARNED_IDS` (CSV của questionId đã cộng). Cùng questionId → bỏ qua.

### Verification
1. Chat 1 câu → Profile → XP tăng +50
2. Chat cùng câu lần 2 → XP không đổi
3. Hoàn thành quiz → XP tăng +500
4. XP đủ 1000 → Level "Beginner" → "Explorer"

---

## Phase 1 — AnswerTabbedActivity: Wire Real Data (1 session) ⬜

**Mục tiêu:** 4 tab (Solution/Concept/Practice/Pitfalls) hiển thị nội dung thật từ Groq thay vì hardcoded "sky blue".

### Response schema mới (`TabbedResponse.java`)
```json
{
  "solution": [{"index":1,"title":"...","body":"..."}],
  "concept":  {"formula":"...","explanation":"...","funFact":"..."},
  "practice": [{"question":"...","options":["A","B","C","D"],"correctIndex":0,"hint":"..."}],
  "pitfalls":  ["Mistake 1", "Mistake 2", "Mistake 3"]
}
```

### Luồng dữ liệu
```
ChatActivity → AnswerActivity → [View full breakdown] → AnswerTabbedActivity
                                                           → GroqTabbedService
                                                           → renderContent() thật
```

### Files
- `api/GroqTabbedService.java` — NEW (OkHttp, cùng pattern GroqAiService)
- `api/TabbedResponse.java` — NEW POJO
- `ui/AnswerTabbedActivity.java` — nhận EXTRA_QUESTION_ID, gọi service
- `ui/AnswerActivity.java` — thêm button "View full breakdown"

---

## Phase 2A — GroqQuizService: Sinh câu hỏi AI (1 session) ⬜

**Mục tiêu:** Groq sinh 5 MCQ mới theo subject + user level. Fallback về JSON nếu offline.

### Files
- `api/GroqQuizService.java` — NEW; trả `List<QuizQuestion>` (POJO sẵn có)
- System prompt yêu cầu subject, level từ `Session.level()`, count=5

---

## Phase 2B — Wire Quiz AI vào QuizActivity (1 session) ⬜

### Files
- `ui/QuizActivity.java` — async loading, spinner, fallback về `QuizDataSource.random()`
- `res/layout/activity_quiz.xml` — thêm loading state

---

## Phase 3 — Dashboard Milo Insight (1 session) ✅

**Mục tiêu:** Thay text cứng bằng 1-2 câu nhận xét AI dựa trên stats thật.

### Files
- `api/GroqAiService.java` — thêm method `quickInsight(String context, Callback<String>)`
- `ui/DashboardActivity.java` — gọi sau `bindStats()`
- `util/Session.java` — thêm `KEY_CACHED_INSIGHT` + `KEY_INSIGHT_DATE` (cache 1 lần/ngày)

---

## Phase 4 — NotificationsActivity: DB-Driven (1 session) ⬜

**Mục tiêu:** Sinh thông báo từ Room DB thật thay vì 6 item hardcoded.

### Logic sinh thông báo (không cần Groq)
| Điều kiện DB | Thông báo |
|-------------|-----------|
| streak 1-2 ngày | "Keep going! Day N — ask 1 question to continue" |
| bookmarks ≥ 1 | "You have N bookmarked problems for review" |
| bestQuizPct ≥ 80 | "You scored X% — Sharp Shooter material!" |
| subject count = 0 | "You haven't tried [subject] yet — explore?" |
| totalQuestions ≥ 10 | "10 questions asked — building great habits!" |

### Files
- `util/NotificationGenerator.java` — NEW: đọc DB + Session → `List<NotifItem>`
- `ui/NotificationsActivity.java` — thay hardcoded list

---

## Phase 5 — Leaderboard Local Simulation (1 session) ⬜

**Mục tiêu:** Thứ hạng tính từ XP thật của user + 8 opponents giả seeded theo username.

### Files
- `util/LeaderboardSimulator.java` — NEW
- `data/QuestionDao.java` — thêm `recentSince(long timestampMs)` cho tab Weekly
- `ui/LeaderboardActivity.java` — thay hardcoded array

---

## Phase 6A — GeminiVisionService: OCR thật (1 session) ⬜

**Mục tiêu:** Chụp ảnh bài toán → Gemini Vision đọc text thật (Groq không xử lý ảnh).

### Files
- `api/GeminiVisionService.java` — NEW: Base64 encode ảnh, POST lên Gemini 1.5 Flash
- Dùng `BuildConfig.GEMINI_API_KEY` (đã có trong `local.properties`)

---

## Phase 6B — Wire OCR vào ScanPreviewActivity (1 session) ⬜

### Files
- `ui/ScanPreviewActivity.java` — gọi GeminiVisionService thay MockOcrService
- Fallback về MockOcrService nếu key không hợp lệ

---

## Git Workflow

```
Branch gốc: feature/groq-ai-integration (đã push)
Branch mới: feature/api-expansion

Commit mỗi phase:
  feat(xp-system): add XP gain for chat and quiz completion
  feat(answer-tabbed): wire real Groq data to 4 tabs
  feat(quiz): AI-generated MCQ via Groq with static fallback
  feat(dashboard): Milo insight from real user stats
  feat(notifications): DB-driven notification generation
  feat(leaderboard): local XP-based rank simulation
  feat(ocr): Gemini Vision replaces MockOcrService
```

---

## Files mới cần tạo (tổng hợp)

| File | Phase |
|------|-------|
| `api/GroqTabbedService.java` | 1 |
| `api/TabbedResponse.java` | 1 |
| `api/GroqQuizService.java` | 2A |
| `util/NotificationGenerator.java` | 4 |
| `util/LeaderboardSimulator.java` | 5 |
| `api/GeminiVisionService.java` | 6A |
