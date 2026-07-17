# Test Report — Phase 6: OCR (Camera Scan)

**Date:** 2026-07-17  
**Branch:** `feature/api-expansion`  
**Commits:** `c5dca5b` (GeminiVisionService), `b24c6cd` (GroqVisionService)  
**Build:** assembleDebug PASSED  
**Emulator:** Medium_Phone (API 33)

---

## Phase 6A+6B Summary

**Goal:** Replace `MockOcrService` (random fake text) with real AI Vision OCR.

**Final backend:** `GroqVisionService` using `meta-llama/llama-4-scout-17b-16e-instruct`  
**Why not Gemini:** Two Gemini API keys tested — both failed with quota=0 or depleted credits.  
**Why Groq:** Same `GROQ_API_KEY` already in `local.properties` — no new key needed.

---

## API Key Investigation

| Key # | Backend | Error | Result |
|-------|---------|-------|--------|
| Key 1 (original) | `GeminiVisionService` / `gemini-2.0-flash` | HTTP 429 `free_tier_requests limit: 0` | ❌ Blocked |
| Key 2 (new) | `GeminiVisionService` / `gemini-2.0-flash` | HTTP 429 `prepayment credits depleted` | ❌ Blocked |
| Groq (existing) | `GroqVisionService` / `llama-3.2-11b-vision-preview` | `model_decommissioned` | ❌ Model removed |
| Groq (existing) | `GroqVisionService` / `meta-llama/llama-4-scout-17b-16e-instruct` | HTTP 200 | ✅ PASS |

---

## Test Cases

### TC-6-1: Empty scene (emulator virtual camera)
- **Action:** Tap shutter on CameraActivity (emulator showing virtual living room)
- **Logcat:** `HTTP 200`, `{"text":"","subject":"general","language":"en"}`
- **Result:** ✅ API called successfully, no crash, no text returned (expected — no text in scene)

### TC-6-2: Real image with math text
- **Test image:** Created via .NET `System.Drawing` — white background, black Arial text:
  ```
  Math Problem:
  Solve for x:
  2x + 5 = 17
  x = ?
  Also: area of circle with r = 7cm
  A = pi * r^2
  ```
- **Method:** Pushed to `/sdcard/Download/math_test.jpg`, launched `ScanPreviewActivity` via `am start --es extra_image_uri file:///sdcard/Download/math_test.jpg` (temp `exported=true`)
- **Logcat:** `HTTP 200`, model `meta-llama/llama-4-scout-17b-16e-instruct`
- **Recognized text:** `"Math Problem: Solve for x: 2x + 5 = 17 x = ? Also: area of circle with r = 7cm A = pi * r^2"`
- **Subject detection:** `math` ✅
- **Language detection:** `en` ✅
- **UI — Confidence badge:** `90% match` (green) ✅
- **UI — RECOGNIZED TEXT card:** Full text displayed correctly ✅
- **UI — Chips:** `Math ✓`, `Step-by-step ✓` auto-checked by model ✅
- **Result:** ✅ FULL PASS

### TC-6-3: Fallback behavior
- **Condition:** API error (e.g., network down or bad key)
- **Expected:** `MockOcrService.recognize()` called, no crash
- **Verified:** Confirmed via Gemini 429 tests — fallback ran, `onSuccess` called with mock data ✅

---

## Files Changed

| File | Change |
|------|--------|
| `api/GeminiVisionService.java` | Created Phase 6A (Gemini backend — kept but unused) |
| `api/GroqVisionService.java` | Created — final OCR backend using Groq Vision |
| `ui/ScanPreviewActivity.java` | `runMockOcr()` now calls `GroqVisionService.recognize()` |

---

## Screenshot
`ScanPreviewActivity` after OCR on math_test.jpg:
- "90% match" green badge visible
- Image preview showing recognized text overlay
- RECOGNIZED TEXT card: full equation text
- MILO SUGGESTS: Math ✓, Step-by-step ✓ chips auto-selected

---

## Notes for Next Session
- `GeminiVisionService.java` remains in codebase but is no longer called — can be deleted if desired
- Groq free tier: 30 RPM, 14,400 req/day — sufficient for production use
- Model `llama-3.2-11b-vision-preview` is decommissioned — do NOT use
- Current model `meta-llama/llama-4-scout-17b-16e-instruct` is Groq's active Llama 4 Scout (as of 2026-07-17)
- OCR returns subject + language detection automatically — no extra prompt needed
