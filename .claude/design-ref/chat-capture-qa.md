Input cho thiết kế cụm Chat & Capture trên Claude Design — không cần đọc lại code, dùng trực tiếp file này.

---

## ChatActivity — 5 câu

**Q1. Bubble layout: user vs assistant bubble khác nhau thế nào về màu, shape, vị trí?**

User bubble: fill `@color/brand_primary` (#F5B544 amber), gravity=end (right-aligned), bottom-right corner radius nhỏ hơn (tạo "tail" bên phải).
Assistant bubble: fill `@color/surface` (#FFFFFF) + 1dp stroke `@color/border`, gravity=start (left-aligned), bottom-left corner radius nhỏ hơn (tạo "tail" bên trái). Không có avatar inline — chỉ có toolbar avatar ở top.
- Evidence: `app/src/main/res/drawable/bg_bubble_user.xml` — `<solid android:color="@color/brand_primary" />`
- Evidence: `app/src/main/res/drawable/bg_bubble_assistant.xml` — `<solid android:color="@color/surface" />` + strokeWidth=1dp, bottom-left radius 4dp

**Q2. Subject chip: chip hiển thị subject ở đâu trong ChatActivity?**

Không có chip hiển thị subject trong ChatActivity. Subject detect ngầm qua `detectSubject()` (keyword matching) khi user send message đầu tiên, lưu vào `question.subject` trong Room DB — không render ra UI trong chat screen.
- Evidence: `ChatActivity.java:136` — `q.subject = detectSubject(text);` — chỉ persist, không hiển thị
- Evidence: `activity_chat.xml` — không có subject chip view nào trong layout

**Q3. Input bar (composer): cấu trúc và components?**

LinearLayout horizontal, background=`@color/surface`, paddingHorizontal/Vertical = space_3:
1. `btn_camera` — IconButton 40dp, ic_camera, iconTint=text_secondary → mở CameraActivity
2. MaterialCardView (weight=1, cornerRadius=radius_xl, bg=bg, border 1dp) chứa TextInputEditText `input_message` — textMultiLine, maxLines=4, minHeight=icon_button_md, hint="Ask Milo anything…"
3. FloatingActionButton `btn_send` — 40dp, fabCustomSize=icon_button_md, maxImageSize=20dp, backgroundTint=brand_primary, ic_send tint=text_on_primary, elevation=0dp
- Evidence: `activity_chat.xml:166-224`

**Q4. Markdown: có render markdown trong message bubble không?**

Không. `MessageAdapter.java` dùng `h.bubble.setText(text)` — plain text thuần, không có markdown library (không có Markwon hay tương đương). Reply từ MockAiService là plain string.
- Evidence: `MessageAdapter.java` — TYPE_USER=1, TYPE_ASSISTANT=2; `onBindViewHolder` gọi `h.bubble.setText()`
- Implication cho design: nếu thiết kế có code block hay bold text, cần cân nhắc thêm markdown renderer hoặc giới hạn formatting

**Q5. Loading state: "Milo is thinking" hiện như thế nào?**

TextView `text_typing` (style=Text.Caption), `visibility=gone` mặc định, marginStart=space_5, marginBottom=space_2. Text cứng: "Milo is thinking…". Không có animation hay loading dots — chỉ text thuần.
Lifecycle: `callAi()` → `typing.setVisibility(VISIBLE)` → `onResponse`/`onFailure` → `typing.setVisibility(GONE)`.
- Evidence: `activity_chat.xml:155-163` — TextView khai báo
- Evidence: `ChatActivity.java:151` — set VISIBLE; `:156` + `:173` — set GONE trong cả 2 callback

---

## CameraActivity — 3 câu

**Q6. Preview: CameraX setup thế nào?**

`PreviewView` id=`camera_preview` — match_parent × match_parent, `scaleType=fillCenter`, `implementationMode=performance`. Là child đầu tiên của FrameLayout root (nằm dưới tất cả overlay).
Java: `ProcessCameraProvider.getInstance()` future → `bindCameraUseCases()` → tạo `Preview.Builder` + `ImageCapture.Builder` (CAPTURE_MODE_MINIMIZE_LATENCY, flashMode field) → `provider.bindToLifecycle(this, selector, previewUseCase, imageCapture)`.
- Evidence: `activity_camera.xml:20-26`
- Evidence: `CameraActivity.java:128-163`

**Q7. Overlay: có những overlay nào (từ trên xuống z-order trong FrameLayout)?**

FrameLayout root chứa 4 layer theo thứ tự khai báo:
1. `camera_preview` (PreviewView) — nền toàn màn
2. `perm_empty` (LinearLayout, `visibility=gone`) — hiện khi CAMERA bị từ chối: amber ic_camera 80dp + title bold + body + btn_grant_perm (Button.Primary) + btn_open_settings (TextButton amber). Background `#CC1A1610`.
3. `viewfinder_frame` (FrameLayout) — margin top=140dp, bottom=220dp, horizontal=28dp — 4 ImageView góc (`ic_frame_corner_tl/tr/bl/br`, 40×40dp) + center LinearLayout (`bg_camera_pill` với ic_target 14dp + hint text "#FFF6E0" 12sp bold)
4. `processing_overlay` (LinearLayout, `visibility=gone`) — hiện sau shutter: ProgressBar large 60dp (indeterminateTint=brand_primary) + text "#FFF6E0" 15sp bold. Background `#CC1A1610`.
Ngoài ra: top bar (gravity=top) và bottom bar (gravity=bottom) nằm trên tất cả.
- Evidence: `activity_camera.xml:28-340`

**Q8. Controls: các control ở top bar và bottom bar?**

Top bar (LinearLayout gravity=top, paddingTop=14dp):
- `btn_close` — IconButton 40dp, backgroundTint=`#80000000`, ic_close white, cornerRadius=20dp → finish()
- Title pill center — LinearLayout `bg_camera_pill`: 6dp dot amber + "SCAN" text white 12sp bold
- `btn_flash` — IconButton 40dp, backgroundTint=`#80000000`, ic_flash_off white → cycleFlash() (OFF→AUTO→ON): icon flip ic_flash_off↔ic_flash, bg đổi dark↔`#CCF5B544` amber, icon tint đổi white↔dark

Bottom bar (LinearLayout gravity=bottom, `#80000000`, paddingBottom=28dp):
- `btn_gallery` — 56×56dp `bg_camera_sidebtn`, ic_image 20dp + "Gallery" 9sp bold white. Dùng Photo Picker (không cần READ_MEDIA_IMAGES trên Android 13+)
- `btn_shutter` — 76×76dp `bg_camera_shutter` (custom drawable), không có icon bên trong → takePhoto()
- `btn_flip` — 56×56dp `bg_camera_sidebtn`, ic_camera_flip 20dp + "Flip" 9sp bold white → toggle LENS_FACING_BACK↔FRONT + rebind
- Evidence: `activity_camera.xml:150-311`
- Evidence: `CameraActivity.java:96-98`, `212-235`

---

## ScanPreviewActivity — 3 câu

**Q9. Layout split: màn hình chia thành mấy vùng?**

LinearLayout vertical, 3 vùng:
1. MaterialToolbar (appbar_height=56dp) — back nav, title centered "Scan Preview" (Text.H3), background=colorBackground
2. ScrollView (weight=1) chứa LinearLayout padding=16dp/8dp:
   - Photo card (MaterialCardView, cornerRadius=radius_lg, elev_2, bg=#1A1610): FrameLayout 240dp height — ImageView centerCrop (bg placeholder #EDE1C0) + confidence badge top-start + btn_crop top-end
   - Recognized text card (marginTop=14dp, border 1dp, elev_1): header row (ic_sparkles + "RECOGNIZED" label + btn_copy) + TextInputLayout FilledBox monospace editable (minLines=4, maxLines=8) + hint pill bottom
   - Milo suggests card (marginTop=12dp, border 1dp, no elevation): "MILO SUGGESTS" label + ChipGroup 3 chip (Math/Step-by-step/Vietnamese)
3. Bottom action bar cố định (colorBackground): 1dp divider + horizontal row: btn_retake + btn_send (weight=1)
- Evidence: `activity_scan_preview.xml:1-373`

**Q10. Action buttons: detail từng button và hành vi?**

4 buttons:
- `btn_crop` (overlay top-end trên photo card) — TonalButton 32dp height, bg=`#80000000`, ic_edit 12dp, text "Crop" 11sp → Toast "Coming soon" (stub)
- `btn_copy` (header recognized card) — OutlinedButton 28dp height, ic_copy 11dp, text "Copy" 11sp, strokeColor=border → copy `inputRecognized.getText()` vào clipboard
- `btn_retake` (bottom bar left) — Button.Secondary 52dp height, ic_camera 18dp, text "Retake" → `startActivity(CameraActivity, CLEAR_TOP | SINGLE_TOP)` + `finish()`
- `btn_send` (bottom bar, weight=1) — Button.Primary 52dp height, ic_send 18dp, text "Ask Milo" → `ChatActivity.EXTRA_PROMPT = R.string.scan_prompt_prefix + recognizedText`, `FLAG_ACTIVITY_CLEAR_TOP` + `finish()`. Disabled cho đến khi `MockOcrService.onSuccess()` trả về.
- Evidence: `activity_scan_preview.xml:99-121`, `168-186`, `339-371`
- Evidence: `ScanPreviewActivity.java:77`, `99-104`, `145-174`

**Q11. OCR confidence: hiển thị ở đâu, data từ đâu, update khi nào?**

2 chỗ hiển thị confidence:
1. `text_confidence` (badge overlay top-start trong photo card, LinearLayout `bg_scan_confidence`): ic_check white 12dp + text "94% MATCH" — default hardcode trong XML, override sau OCR: `textConfidence.setText(R.string.scan_confidence_label, r.confidencePercent)`
2. `text_hint` (hint pill cuối recognized text card): default "Milo recognized 94% — fix any wrong characters…" hardcode XML → override: `textHint.setText(R.string.scan_recognized_hint, r.confidencePercent)`

Data source: `MockOcrService.Result.confidencePercent` (mock service). `btnSend.setEnabled(false)` trước khi OCR xong → enabled trong `onSuccess()`. `onError()` set hint=camera_error_capture + enable send.
- Evidence: `activity_scan_preview.xml:66-96` (badge), `217-243` (hint pill)
- Evidence: `ScanPreviewActivity.java:77-80` (disable), `113-115` (update confidence + enable)
