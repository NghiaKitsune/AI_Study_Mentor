# Phase 3A — Camera Scan · Implementation Notes

> Date: May 2026 · Status: ✅ Code complete, ready to Gradle sync + test on device.

## What landed

### Design preview (HTML/React)
- `screens-camera.jsx` — two new screens:
  - **CameraScan** — viewfinder with 4 amber corner brackets, mock paper inside, flash toggle (off/auto/on), gallery + flip side buttons, mode pills (Photo/Document/Math), shutter w/ AI accent
  - **ScanPreview** — captured photo + 94% confidence badge + editable recognized text + Milo suggests chips + Retake / Ask Milo
- Wired into `preview-app.jsx` rail under new "Camera scan" group (screens 8–9 of 20).

### Android code
| File | Purpose |
|---|---|
| `ui/CameraActivity.java` | CameraX preview · runtime CAMERA permission · shutter → cache file · gallery via Photo Picker · flash cycle · flip lens |
| `ui/ScanPreviewActivity.java` | Loads captured image via Glide · runs `MockOcrService` · editable recognized text · copy/retake/send → ChatActivity |
| `api/MockOcrService.java` | Async stand-in for vision OCR — returns 1 of 6 sample texts + confidence + detected subject/style/lang after 0.9–1.6s |
| `res/layout/activity_camera.xml` | Fullscreen black · `PreviewView` · permission empty state · processing overlay |
| `res/layout/activity_scan_preview.xml` | Photo card · recognized-text TextInputLayout · chips · Retake / Ask Milo bar |
| `res/drawable/ic_flash.xml` · `ic_flash_off.xml` · `ic_camera_flip.xml` · `ic_edit.xml` · `ic_refresh.xml` · `ic_copy.xml` | New vector icons |
| `res/drawable/bg_camera_shutter.xml` · `bg_camera_pill.xml` · `bg_camera_sidebtn.xml` · `bg_camera_frame.xml` · `bg_scan_confidence.xml` · `bg_scan_hint.xml` | Backgrounds |
| `res/drawable/ic_frame_corner_*.xml` × 4 | Amber L-shaped corner brackets for the viewfinder |
| `res/xml/file_paths.xml` | FileProvider mappings for camera cache |

### Wiring changes
- `AndroidManifest.xml` — registered `CameraActivity` (theme `Theme.StudyMentor.FullscreenBlack`, portrait-locked) and `ScanPreviewActivity` (parent = CameraActivity). Added `FileProvider` declaration.
- `themes.xml` — new `Theme.StudyMentor.FullscreenBlack`.
- `strings.xml` — UC2.5 string bundle (titles, hints, errors, prompt prefix).
- `app/build.gradle` — added 4 CameraX deps (`camera-core`, `camera-camera2`, `camera-lifecycle`, `camera-view` @ 1.3.1).
- `HomeActivity` — `btn_compose_camera` now opens `CameraActivity` (was Toast).
- `ChatActivity` — `btn_camera` in composer now opens `CameraActivity` (was unwired).

## Flow

```
Home/Chat composer ─ tap camera ─► CameraActivity
                                      │
                                      ├─ no CAMERA perm → empty state + Grant button
                                      │                     │
                                      │                     └─ ActivityResultContracts.RequestPermission
                                      │
                                      ├─ shutter → cache file → ScanPreviewActivity (uri)
                                      │
                                      └─ gallery → Photo Picker → ScanPreviewActivity (uri)
                                                  (no READ_MEDIA_IMAGES needed)

ScanPreviewActivity ─ MockOcrService.recognize() ─ 0.9–1.6s ─►
   text + confidence + detected subject/style/lang populate the form
   │
   ├─ Retake  → CameraActivity (CLEAR_TOP)
   ├─ Copy    → clipboard
   ├─ Crop    → Toast "Coming soon" (Phase 3A.1)
   └─ Ask Milo → ChatActivity with EXTRA_PROMPT = "Help me solve this problem:\n\n<text>"
```

## Permissions

- `CAMERA` — requested at runtime in `CameraActivity.onCreate()` via `ActivityResultContracts.RequestPermission`. Empty state with "Grant access" + "Open settings" deep link if denied.
- `READ_MEDIA_IMAGES` — **not** required because we use the Android 13+ Photo Picker (`ActivityResultContracts.PickVisualMedia`).
- `POST_NOTIFICATIONS` — unchanged, still asked once on Home first launch.

## Swapping mock OCR for real

`api/MockOcrService.java` returns a `Result(text, confidence, subject, style, lang)`. To swap:

1. Add a new method in `AiService.java` (Retrofit):
   ```java
   @Multipart
   @POST("api/vision")
   Call<OcrResponse> recognize(@Part MultipartBody.Part image);
   ```
2. In `ScanPreviewActivity.runMockOcr()`, replace the `MockOcrService.recognize` call with `ApiClient.get().recognize(...)`.
3. Backend proxies to OpenAI Vision / Claude Vision and returns the same shape.

## Known TODOs / Phase 3A.1

- **Crop tool** — currently shows Toast. Easiest: integrate `uCrop` library or roll a simple `MaterialCardView` with drag-handles over an `ImageView`.
- **Tap-to-focus on viewfinder** — CameraX supports this via `previewView.setOnTouchListener` + `FocusMeteringAction`. ~30 lines.
- **Multi-page scanning** — not in scope for MVP.
- **Real flash effect** — `FLASH_MODE_AUTO/ON` is already plumbed to `ImageCapture`, but only fires when the shutter is pressed. No torch mode (continuous light) yet.
- **Glide → captured image** — using `centerCrop`; might want to add `dontTransform()` if user crops to a non-rectangular region in Phase 3A.1.

## Verifying locally

1. Gradle sync (will pull CameraX 1.3.1, ~3MB).
2. Run on Pixel 6 API 33 emulator or real device.
3. Home → tap camera in composer → Grant CAMERA → point at any paper → shutter.
4. Spinner → ScanPreview with a random sample question + ~85–95% confidence.
5. Tap "Ask Milo" → Chat opens with prompt prefilled. Hit send → mock AI reply.

If you see `java.lang.SecurityException` around FileProvider — make sure `applicationId` in `app/build.gradle` matches the manifest authority pattern `${applicationId}.fileprovider`. It does by default.
