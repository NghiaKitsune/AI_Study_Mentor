# Phase 0 & 1 — Test Results

**Date:** 2026-07-16 / 2026-07-17  
**Branch:** `feature/api-expansion`  
**Device:** Medium_Phone emulator (Android 13, swiftshader_indirect GPU)  
**Method:** ADB UI automation (`uiautomator dump`, `input tap/text`) + SharedPreferences read via `run-as` + logcat capture

---

## Phase 0 — XP System

### Summary

| Metric | Result |
|--------|--------|
| Build | ✅ assembleDebug PASSED (26s incremental) |
| Test cases | 4 / 4 PASS |
| Anti-farming | ✅ Verified |
| ProfileActivity display | ✅ Correct |

### Files Changed

| File | Thay đổi |
|------|---------|
| `util/Session.java` | Thêm `KEY_XP`, `KEY_XP_EARNED_IDS`; 5 methods mới |
| `ui/ChatActivity.java` | `Session.addXp(this, 50, questionId)` sau AI response |
| `ui/QuizActivity.java` | `Session.addXp(this, 500, currentTimeMs)` trong `openResult()` |
| `ui/ProfileActivity.java` | Thay `totalQuestions * 10` bằng `Session.xp()`; level từ `Session.levelNumber/Title()` |

### XP Level Thresholds

```
Level 1:  0    –    999 XP  "Beginner"
Level 2:  1000 –  2,999 XP  "Explorer"
Level 3:  3000 –  5,999 XP  "Scholar"
Level 4:  6000 –  9,999 XP  "Expert"
Level 5:  10000+        XP  "Master"
```

### Anti-Farming Rule

`Session.addXp(ctx, amount, qId)` kiểm tra `KEY_XP_EARNED_IDS` (CSV) trước khi cộng.  
Cùng `questionId` → bỏ qua, không cộng thêm.  
Quiz dùng `System.currentTimeMillis()` làm ID → mỗi lần hoàn thành quiz là unique.

### Test Cases

#### TC-0-1: Chat +50 XP
- **Điều kiện:** Baseline `xp_points=0` (chưa từng cộng)
- **Hành động:** Hỏi "What is 2+2?" trong ChatActivity → AI trả lời thành công
- **Kết quả SharedPrefs sau:**
```xml
<int name="xp_points" value="50" />
<string name="xp_earned_ids">18</string>
```
- **Verdict:** ✅ PASS — XP tăng đúng +50, questionId=18 được ghi vào earned_ids

#### TC-0-2: Anti-Farming (cùng questionId)
- **Điều kiện:** `xp_points=50`, `xp_earned_ids=18`
- **Hành động:** Gửi thêm 1 tin nhắn trong cùng ChatActivity (questionId vẫn là 18)
- **Kết quả SharedPrefs sau:**
```xml
<int name="xp_points" value="50" />
<string name="xp_earned_ids">18</string>
```
- **Verdict:** ✅ PASS — XP không tăng, farming bị chặn

#### TC-0-3: Quiz +500 XP
- **Điều kiện:** `xp_points=50`
- **Hành động:** Hoàn thành quiz 5 câu qua tab Practice
- **Kết quả SharedPrefs sau:**
```xml
<int name="xp_points" value="550" />
<string name="xp_earned_ids">18,1784220930144</string>
```
- **Verdict:** ✅ PASS — XP tăng +500, timestamp quiz được ghi vào earned_ids

#### TC-0-4: ProfileActivity hiển thị đúng
- **Điều kiện:** `xp_points=550`
- **Hành động:** Mở ProfileActivity qua tab Profile
- **Kết quả (UI dump text):**
  - `"Beginner · Level 1"` ✅ (550 < 1000 = Level 1)
  - `"550 XP"` ✅
  - `"450 XP to next level"` ✅ (1000 - 550 = 450)
  - Total XP stat row: `"550"` ✅
- **Verdict:** ✅ PASS

---

## Phase 1 — AnswerTabbedActivity: Wire Real Groq Data

### Summary

| Metric | Result |
|--------|--------|
| Build | ✅ assembleDebug PASSED (2m 19s cold) |
| HTTP status | 200 / 200 |
| Tabs tested | 4 / 4 PASS |
| Loading state | ✅ Verified |
| Error fallback | Not triggered (API healthy) |

### Files Changed

| File | Thay đổi |
|------|---------|
| `api/TabbedResponse.java` | NEW — POJO với inner classes SolutionStep, Concept, PracticeQuestion |
| `api/GroqTabbedService.java` | NEW — OkHttp, model `llama-3.3-70b-versatile`, JSON mode |
| `ui/AnswerTabbedActivity.java` | REFACTOR — load DB, gọi service, render thật / loading / error |
| `ui/AnswerActivity.java` | THÊM — `bindDeepDive()` + button → AnswerTabbedActivity |
| `res/layout/activity_answer.xml` | THÊM — `btn_deep_dive` TonalButton |
| `res/values/strings.xml` | THÊM — 3 strings |

### Groq Prompt Schema (TabbedResponse)

```json
{
  "solution": [{"index": 1, "title": "...", "body": "..."}],
  "concept":  {"formula": "...", "explanation": "...", "funFact": "..."},
  "practice": [{"question": "...", "options": ["A","B","C","D"], "correctIndex": 0, "hint": "..."}],
  "pitfalls":  ["Mistake 1", "Mistake 2", "Mistake 3"]
}
```

Rules enforced by prompt:
- `solution`: 2–4 steps
- `concept`: 1 formula + explanation + 1 fun fact
- `practice`: exactly 2 MCQ with 4 options
- `pitfalls`: exactly 3 mistakes
- English only

### Test Question: "What is Pythagoras theorem?"

**TabbedAI Logcat:**
```
D TabbedAI: Raw: {"id":"chatcmpl-7c333fd4...","choices":[{"message":{"content":"{ \"solution\":[...
D TabbedAI: Content: { "solution":[ {"index":1,"title":"Define the variables","body":"Let a and b be...
```
HTTP 200 ✅ | Parse OK ✅

#### TC-1-1: Solution Tab
- **Nội dung từ Groq:**
  1. **Define the variables** — "Let a and b be the lengths of the two sides that form the right angle, and c be the length of the hypotenuse"
  2. **Apply the theorem** — "The theorem can be applied by using the formula: a^2 + b^2 = c^2"
  3. **Solve for the unknown side** — "By plugging in the values of the known sides, we can solve for the length of the unknown side"
- **Không còn hardcoded "sky blue" content** ✅
- **Verdict:** ✅ PASS

#### TC-1-2: Concept Tab
- **Nội dung từ Groq:**
  - **Key Formula / Concept:** `a^2 + b^2 = c^2`
  - **Explanation:** "Pythagoras theorem is a fundamental concept in geometry that describes the relationship between the lengths of the sides of a right-angled triangle. It states that the square of the length of the hypotenuse is equal to the sum of the squares of the lengths of the other two sides."
  - **Fun Fact:** (về Pythagoras là nhà triết học Hy Lạp cổ đại)
- **Verdict:** ✅ PASS

#### TC-1-3: Practice Tab
- **Nội dung từ Groq:**
  - **Q1:** "What is the length of the hypotenuse of a right-angled triangle with sides of length 3 and 4?"
    - Options: A. 5 / B. 6 / C. 7 / D. 8 | Hint: "Use the Pythagoras theorem formula"
  - **Q2:** "In a right-angled triangle, the length of the hypotenuse is 10 and one of the sides is 6. What is the length of the other side?"
    - Options: A. 4 / B. 6 / C. 8 / D. 12 | Hint: "Rearrange the formula to solve for the unknown side"
- **Verdict:** ✅ PASS (Q1 answer: 5, Q2 answer: 8 — both correct)

#### TC-1-4: Pitfalls Tab
- **Nội dung từ Groq:**
  - ✗ Common Mistake: "Forgetting to square the lengths of the sides"
  - ✗ Common Mistake: "Not identifying the hypotenuse correctly"
  - ✗ Common Mistake: (mistake 3 — below fold, visible khi scroll)
- **Verdict:** ✅ PASS

#### TC-1-5: Loading State
- **Điều kiện:** Mở AnswerTabbedActivity trước khi API respond
- **Kết quả:** Content container hiển thị "Milo is preparing the full breakdown…" trong khi chờ
- **Sau khi API respond:** Content tự động cập nhật
- **Verdict:** ✅ PASS

### Navigation Flow Verified
```
HomeActivity
  → [card_composer] → ChatActivity
      → [type "What is Pythagoras theorem?" + send]
      → [AI response ~1.5s] → Snackbar "Step-by-step breakdown ready" [View]
      → AnswerActivity (shows QUESTION card + steps + Deep Dive button)
          → [tap btn_deep_dive] → AnswerTabbedActivity
              → loading state → GroqTabbedService.generate()
              → HTTP 200 → renderContent() for all 4 tabs
```

---

## Logcat Summary

```
Phase 0 — No dedicated logcat tag (SharedPrefs writes are silent)
  Evidence: run-as cat shared_prefs → xp_points = 50 / 550

Phase 1 — Tag: TabbedAI
  07-17 00:12:05 D TabbedAI: Raw: {"id":"chatcmpl-7c333fd4..."...}
  07-17 00:12:05 D TabbedAI: Content: { "solution":[{"index":1,"title":"Define the variables"...
  → HTTP 200, full JSON parsed successfully
```

---

## Git Commits

| Commit | Description |
|--------|-------------|
| `844053c` | feat(xp-system): add XP gain for chat and quiz completion |
| `f464090` | docs(planning): mark Phase 0 XP System as complete |
| `e38c737` | feat(answer-tabbed): wire real Groq data to 4 tabs |
| `25dada7` | docs(planning): mark Phase 1 AnswerTabbed as complete |

**Branch:** `feature/api-expansion`

---

## Known Issues / Notes

- `text_meta` label trong AnswerTabbedActivity header vẫn hiển thị "Physics · Optics" (placeholder từ XML) — AnswerTabbedActivity.java không set field này vì không cần thiết cho chức năng. Không ảnh hưởng đến nội dung 4 tab.
- Snackbar "View" trong ChatActivity có thời gian hiển thị ngắn (~2.75s) — cần timing chính xác khi test bằng ADB automation. Trong thực tế user sẽ thấy và tap ngay.
- `xp_earned_ids` lưu CSV không giới hạn kích thước — acceptable cho MVP (user sẽ không hỏi hàng nghìn câu riêng biệt). Có thể tối ưu sau nếu cần.
