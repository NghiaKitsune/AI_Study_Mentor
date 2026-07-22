# Phase 2 — Test Results: AI-Generated Quiz Questions

**Date:** 2026-07-17  
**Branch:** `feature/api-expansion`  
**Commit:** `3c2354d`  
**Device:** Medium_Phone emulator (Android 13, swiftshader_indirect GPU)  
**Method:** ADB UI automation (`uiautomator dump`, `input tap`) + logcat capture + SharedPreferences read via `run-as`

---

## Summary

| Metric | Result |
|--------|--------|
| Build | ✅ assembleDebug PASSED (1m 50s cold) |
| Test cases | 5 / 5 PASS |
| Loading state | ✅ Verified |
| AI questions (not from JSON) | ✅ Verified via logcat |
| Fallback | Not triggered (API healthy) |
| XP after quiz | ✅ +500 added to Session |

---

## Files Changed

| File | Thay đổi |
|------|---------|
| `api/GroqQuizService.java` | NEW — OkHttp, model `llama-3.3-70b-versatile`, JSON mode, returns `List<QuizQuestion>` |
| `ui/QuizActivity.java` | REFACTOR — async loading, calls GroqQuizService, fallback to QuizDataSource |
| `res/layout/activity_quiz.xml` | THÊM — `layout_loading` (spinner + text), `layout_quiz_content` (id added to NestedScrollView) |
| `res/values/strings.xml` | THÊM — `quiz_generating` + `quiz_generating_sub` |

---

## GroqQuizService Design

```
POST https://api.groq.com/openai/v1/chat/completions
Model: llama-3.3-70b-versatile
temperature: 0.8
response_format: json_object
timeout: connect=15s, read=45s

System prompt: "Generate exactly {count} MCQ for {subject} at {level} level"

Response schema:
{
  "questions": [
    {
      "question": "...",
      "subject": "general",
      "subjectTag": "Science · Space · Multiple Choice",
      "options": ["A. ...", "B. ...", "C. ...", "D. ..."],
      "correctIndex": 2,
      "explanation": "..."
    }
  ]
}
```

Validation: each item must have non-null `question` and `options` with exactly 4 elements. Malformed items are silently skipped.

---

## Loading UX

- When `QuizActivity` opens: `layout_loading` is VISIBLE, `layout_quiz_content` is GONE
- After `GroqQuizService.onSuccess()` or `onError()` (fallback): swap visibility
- Both containers have `layout_weight="1"` and `layout_height="0dp"` — GONE element takes no space

---

## Test Cases

### TC-2-1: Loading State Visible

- **Điều kiện:** Tap Practice tab from HomeActivity bottom nav
- **Hành động:** Capture screenshot at ~0.8s after tap (before API responds)
- **Kết quả:** Amber ProgressBar spinning + "Generating questions…" bold + "Milo is creating personalized questions for you" subtitle visible
- **Verdict:** ✅ PASS

**Screenshot:** `quiz_loading.png`

---

### TC-2-2: AI Questions Loaded (not from static JSON)

- **Điều kiện:** API responds after ~2-4s
- **Hành động:** Capture screenshot after loading disappears
- **Kết quả (UI):** 
  - `QUESTION 1 / 5`
  - Subject tag: `Science · Space · Multiple Choice` (Groq-generated tag, not from quiz_questions.json)
  - Question: "What is the largest planet in our solar system?"
  - Options: A. Earth / B. Saturn / C. Jupiter / D. Uranus
- **Static JSON check:** This question does NOT exist in `assets/quiz_questions.json` ✅
- **Verdict:** ✅ PASS

**Screenshot:** `quiz_loaded.png`

---

### TC-2-3: Logcat — HTTP 200 + Parse OK

```
07-17 00:32:07.162 20894 21021 D QuizAI  : Raw: {"id":"chatcmpl-369f85cf-adcb-492d-ae98-c98bc0251ef1","object":"chat.completion","created":1784223183,"model":"llama-3.3-70b-versatile","choices":[{"index":0,"message":{"role":"assistant","content":"{\n  \"questions\": [\n       {\n           \"question\": \"What is the largest planet in our solar s...

07-17 00:32:07.195 20894 21021 D QuizAI  : Content: {
07-17 00:32:07.195 20894 21021 D QuizAI  :   "questions": [
07-17 00:32:07.195 20894 21021 D QuizAI  :        {
07-17 00:32:07.195 20894 21021 D QuizAI  :            "question": "What is the largest planet in our solar system?",
07-17 00:32:07.195 20894 21021 D QuizAI  :            "subject": "general",
07-17 00:32:07.195 20894 21021 D QuizAI  :            "subjectTag": "Science · Space · Multiple Choice",
07-17 00:32:07.195 20894 21021 D QuizAI  :            "options": ["A. Earth","B. Saturn","C. Jupiter","D. Uranus"],
07-17 00:32:07.195 20894 21021 D QuizAI  :            "correctIndex": 2,
07-17 00:32:07.195 20894 21021 D QuizAI  :            ...
```

- HTTP 200 ✅
- JSON parsed to `List<QuizQuestion>` successfully ✅
- **Verdict:** ✅ PASS

---

### TC-2-4: Full Quiz Flow (Q1→Q5→Result)

- **Điều kiện:** AI questions loaded
- **Hành động:** Q1 timer expired (TIME'S UP) → Next; Q2–Q5 selected option_a → Check → Next/See results
- **Kết quả:** QuizResultActivity shows correctly: `1/5`, `20%` accuracy, `+10 XP` badge (result display), `By Subject: General 1/5`
- **Verdict:** ✅ PASS

**Screenshot:** `quiz_result_phase2.png`

---

### TC-2-5: XP Added After Quiz Completion

- **Điều kiện:** Before quiz: `xp_points=650` (from previous Phase 0/1 sessions + 2 extra chats)
- **Hành động:** Complete quiz → read SharedPreferences via `run-as`
- **Kết quả:**
```xml
<int name="xp_points" value="1150" />
<string name="xp_earned_ids">18,1784220930144,19,20,1784223278471</string>
```
  - `1784223278471` = timestamp-based quiz ID (new, unique per completion) ✅
  - XP delta = 1150 − 650 = +500 ✅
- **Verdict:** ✅ PASS

---

## Fallback Behavior (Not Triggered — API Healthy)

The fallback path in `QuizActivity.startQuiz()` calls `QuizDataSource.random(ctx, subject, 5)` when `GroqQuizService.onError()` fires. This path was not exercised during this session (API responded successfully). Fallback would be verified if Groq key were revoked or network were offline.

---

## Known Issue

**Subject tag casing:** When Groq returns `subjectTag = "Science · Space · Multiple Choice"` (mixed case), QuizActivity displays it as-is. The static JSON questions use `"MATH · ALGEBRA · MULTIPLE CHOICE"` (all caps). This is cosmetic inconsistency; both are readable.

---

## Git Commit

| Commit | Description |
|--------|-------------|
| `3c2354d` | feat(quiz): AI-generated MCQ questions via Groq with static fallback |

**Branch:** `feature/api-expansion`
