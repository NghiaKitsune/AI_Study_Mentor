# Gamification Verification Report
Generated: 2026-06-17

---

## 1. QuizActivity — vòng lặp nhiều câu + dữ liệu thật

**ĐÃ SỬA hoàn toàn.**

```java
// QuizActivity.java : 51
questions = QuizDataSource.random(this, subject, 5);
if (questions.isEmpty()) { finish(); return; }
```

- Dòng 38–43: khai báo `List<QuizQuestion> questions`, `currentIdx`, `score` — có vòng lặp nhiều câu.
- Dòng 51: gọi `QuizDataSource.random()` load từ `assets/quiz_questions.json`.
- Dòng 194–200: `advanceQuestion()` tăng `currentIdx`, gọi lại `showQuestion()` — đã có full loop.
- Dòng 202–208: `openResult()` truyền `EXTRA_SCORE` + `EXTRA_TOTAL` sang QuizResultActivity.
- File `assets/quiz_questions.json` tồn tại (thư mục `app/src/main/assets/` có trong untracked — chưa commit nhưng đã có trên disk).

---

## 2. QuizResultActivity — điểm / streak / danh sách câu hỏi

**MIX: score+streak ĐÃ SỬA · danh sách câu trả lời VẪN MOCK.**

### Score + streak: ĐÃ SỬA — dòng 38–46
```java
// QuizResultActivity.java : 38–46
score = getIntent().getIntExtra(EXTRA_SCORE, 0);
total = getIntent().getIntExtra(EXTRA_TOTAL, 1);
com.studymentor.app.util.Session.saveQuizResult(this, score, total);
int pct = total > 0 ? (score * 100 / total) : 0;

((TextView) findViewById(R.id.text_score)).setText(score + " / " + total);
((TextView) findViewById(R.id.text_accuracy)).setText(pct + "% accuracy · Quiz result");
int streak = com.studymentor.app.util.Session.streak(this);
((TextView) findViewById(R.id.text_streak)).setText(String.valueOf(streak));
```
Score và pct đọc từ intent thật. Streak đọc từ Session.

### Danh sách câu hỏi breakdown: VẪN MOCK — dòng 63–75
```java
// QuizResultActivity.java : 63–75
private void setupAnswerList() {
    List<QuizAnswer> answers = Arrays.asList(
        new QuizAnswer("What is Newton's first law?", true),
        new QuizAnswer("Define acceleration", true),
        new QuizAnswer("What is a force in physics?", true),
        new QuizAnswer("Unit of force is...", false),
        new QuizAnswer("Gravity on the Moon vs Earth", true)
    );
    ...
}
```
5 câu physics hardcode, không liên quan đến quiz vừa làm. Kết quả đúng/sai cũng hardcode.

---

## 3. LeaderboardActivity — tên/XP hardcode

**VẪN MOCK — dòng 41–66.**

```java
// LeaderboardActivity.java : 41–54
String[][] top3 = {
    {"AN", "AnNguyen", "5,460"},
    {"MI", "MinhKhoi", "4,820"},
    {"TH", "ThaoP",    "4,310"},
};

// dòng 59–66
List<RankEntry> entries = Arrays.asList(
    new RankEntry(4, "LinhT",   "3,980", false),
    new RankEntry(5, "PhuongN", "3,640", false),
    new RankEntry(6, "TienM",   "3,210", false),
    new RankEntry(7, me.isEmpty() || me.equals("Friend") ? "NghiaM" : me, "1,840", true),
    new RankEntry(8, "HaiD",    "1,680", false),
    new RankEntry(9, "BinhV",   "1,520", false)
);
```
Tất cả tên và XP đều viết tay. Chỉ có tên "YOU" (rank 7) lấy từ `Session.name()`.

---

## 4. ProfileActivity — bindBadges() / bindActivity()

### bindBadges() unlock conditions: MIXED — dòng 53–74

**6/8 badge ĐÃ SỬA** — đọc từ DB và Session thật:
```java
// ProfileActivity.java : 54–58
int streak      = Session.streak(this);                                   // real
int qCount      = StudyMentorApp.get().db().questionDao().count();        // real
int bookmarks   = StudyMentorApp.get().db().questionDao().bookmarkedCount(); // real
int mathCount   = StudyMentorApp.get().db().questionDao().countBySubject("math"); // real
int bestQuizPct = Session.bestQuizPct(this);                              // real
```

**2/8 badge VẪN MOCK** — hardcode `false`:
```java
// ProfileActivity.java : 65
new BadgeItem(R.drawable.ic_trophy, "Top 10",     "Weekly leaderboard", false, ...),
// dòng 67
new BadgeItem(R.drawable.ic_zap,    "Speed Demon","Quiz in < 30s",       false, ...),
```

### bindActivity(): VẪN MOCK — dòng 76–87
```java
// ProfileActivity.java : 77–82
List<ActivityItem> items = Arrays.asList(
    new ActivityItem(R.drawable.ic_medal,  "Earned \"Sharp Shooter\" badge", "10m",   ...),
    new ActivityItem(R.drawable.ic_target, "Completed Physics quiz (80%)",   "12m",   ...),
    new ActivityItem(R.drawable.ic_flame,  "7-day streak — keep going!",     "Today", ...),
    new ActivityItem(R.drawable.ic_zap,    "Reached Level 7 · Algebra Apprentice", "2d", ...)
);
```
4 mục cứng, thời gian cứng, không phản ánh hoạt động thực tế của user.

---

## 5. NotificationsActivity.buildItems() — list cố định

**VẪN MOCK — dòng 72–99.**

```java
// NotificationsActivity.java : 72–99
private List<NotifItem> buildItems() {
    return Arrays.asList(
        new NotifItem("achievements", ..., "New badge unlocked!", "...", "10m ago", true),
        new NotifItem("reminders",    ..., "Review your bookmark","...", "1h ago",  true),
        new NotifItem("mistakes",     ..., "You keep mixing these up","...","2h ago", true),
        new NotifItem("reminders",    ..., "Keep your streak alive","...", "5h ago", false),
        new NotifItem("achievements", ..., "Daily summary",       "...", "Yesterday",false),
        new NotifItem("reminders",    ..., "Weekly recap is ready","...","2d ago",  false)
    );
}
```
6 mục cứng, thời gian cứng. Filter chip hoạt động nhưng data là fake.

---

## 6. DashboardActivity — bindLiveStats() / bindSubjects()

### bindLiveStats(): ĐÃ SỬA — dòng 41–48
```java
// DashboardActivity.java : 42–48
int questionCount = StudyMentorApp.get().db().questionDao().count();   // real DB
TextView tvQ = findViewById(R.id.text_stat_questions);
if (tvQ != null) tvQ.setText(String.valueOf(questionCount));

int streak = com.studymentor.app.util.Session.streak(this);           // real
TextView tvStreak = findViewById(R.id.text_streak);
if (tvStreak != null) tvStreak.setText(String.valueOf(streak));
```
Không còn `Math.max` floors trên số liệu thật. Đọc trực tiếp từ DB và Session.

### bindSubjects(): ĐÃ SỬA — dòng 51–67
```java
// DashboardActivity.java : 52–56
int mathCount    = StudyMentorApp.get().db().questionDao().countBySubject("math");
int codeCount    = StudyMentorApp.get().db().questionDao().countBySubject("code");
int scienceCount = StudyMentorApp.get().db().questionDao().countBySubject("science");
int historyCount = StudyMentorApp.get().db().questionDao().countBySubject("history");
int total        = Math.max(mathCount + codeCount + scienceCount + historyCount, 1);
```
`Math.max(..., 1)` ở dòng 56 chỉ dùng làm divisor để tránh chia-cho-0, **không phải** ép sàn số liệu. Các con số `mathCount`, `codeCount`, v.v. đọc thẳng từ DB, không bị inflate.
Tên môn học ("Math", "Coding", "Science", "Languages") là display label — expected.

---

## Tổng kết

| File | Mục kiểm tra | Trạng thái | Dòng chính |
|------|-------------|-----------|------------|
| `QuizActivity` | Một câu cứng / không vòng lặp / không assets | **ĐÃ SỬA** | 51, 194–200 |
| `QuizResultActivity` | Score+streak từ intent | **ĐÃ SỬA** | 38–46 |
| `QuizResultActivity` | Danh sách câu hỏi breakdown | **VẪN MOCK** | 63–75 |
| `LeaderboardActivity` | Tên/XP hardcode | **VẪN MOCK** | 41–66 |
| `ProfileActivity.bindBadges()` | Unlock conditions | **MIX** (6/8 thật, 2 cứng `false`) | 54–68 |
| `ProfileActivity.bindActivity()` | Activity feed | **VẪN MOCK** | 76–87 |
| `NotificationsActivity.buildItems()` | List thông báo | **VẪN MOCK** | 72–99 |
| `DashboardActivity.bindLiveStats()` | Math.max floors + số liệu thật | **ĐÃ SỬA** | 41–48 |
| `DashboardActivity.bindSubjects()` | Môn học hardcode | **ĐÃ SỬA** | 51–67 |

**Phần còn mock cần xử lý trước khi demo thật:**
- `QuizResultActivity.setupAnswerList()` — cần truyền danh sách câu hỏi + kết quả thật từ QuizActivity
- `LeaderboardActivity` — toàn bộ data cần mock động hoặc API
- `ProfileActivity.bindActivity()` — cần log hoạt động thật vào DB
- `NotificationsActivity.buildItems()` — cần sinh từ WorkManager events hoặc DB
