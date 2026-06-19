# Design Consistency Audit Report
Generated: 2026-06-17

Scope: 22 Activities + 13 item layouts
Spec referenced by user: `brand_primary #5C6BC0`, `brand_primary_deep #3949AB`, `brand_primary_tint #E8EAF6`, `brand_accent #FF8F00`, `text_primary #1A1A2E`, `surface #FFFFFF`, `bg #F8F9FF`, Bricolage Grotesque (heading), Plus Jakarta Sans (body)
Source reads: all `activity_*.xml` + all `item_*.xml` + `colors.xml` + `dimens.xml`

---

## ⚠️ Phát hiện hệ thống — Đọc trước

### A. Lệch hệ thống màu (Color system drift — Session 4)

Spec mà user tham chiếu trong yêu cầu audit là **bảng màu indigo cũ**. Tuy nhiên, Session 4 (Logo.html integration, 2026-06-12) đã **viết lại toàn bộ `colors.xml`** sang bảng màu amber/warm. Đây là sự thay đổi hệ thống, không phải vi phạm từng Activity:

| Token | Spec cũ (user tham chiếu) | Giá trị thực tế hiện tại |
|-------|--------------------------|--------------------------|
| `brand_primary` | `#5C6BC0` (indigo) | `#F5B544` (amber) |
| `brand_primary_deep` | `#3949AB` (indigo dark) | `#C7800A` (amber dark) |
| `brand_primary_tint` | `#E8EAF6` (indigo tint) | `#FEF8E7` (warm cream) |
| `brand_accent` | `#FF8F00` (amber) | `#E47B47` (terracotta) |
| `text_primary` | `#1A1A2E` (cool dark) | `#2A2418` (warm brown) |
| `bg` | `#F8F9FF` (cool white) | `#FAF5EA` (warm cream) |
| `surface` | `#FFFFFF` | `#FFFFFF` ✅ |

**Hệ quả:** Mọi `@color/brand_primary` trong layout hiện là amber `#F5B544`, không phải indigo. Các vi phạm "hex cứng" bên dưới được đánh giá **dựa trên bảng màu thực tế (amber)** — tức là dùng hex thay vì dùng token `@color/*` tương ứng.

### B. Font hệ thống vắng mặt hoàn toàn

Thư mục `app/src/main/res/font/` **không tồn tại**. Bricolage Grotesque và Plus Jakarta Sans chưa bao giờ được thêm vào project. Toàn bộ typography đang dùng font hệ thống (Roboto). Đây là thiếu sót ở cấp độ `build.gradle` / assets, không thể vá từng layout riêng lẻ.

**Đề xuất sửa (toàn hệ thống):** Thêm font files vào `res/font/`, khai báo trong `themes.xml` qua `fontFamily` attribute trên `TextAppearance.*` styles, sau đó tất cả `@style/Text.*` tự kế thừa.

---

## Audit theo Activity / Layout

### Legend
| Ký hiệu | Loại vi phạm |
|---------|-------------|
| **HC-Color** | Hardcoded hex color trong XML thay vì `@color/*` |
| **HC-Dimen** | Hardcoded số `dp`/`sp` thay vì `@dimen/*` |
| **M3-Widget** | Dùng `@android:style/*` thay vì Material3 widget |
| **Inconsist** | Spacing/padding không nhất quán với màn hình cùng loại |
| **Tab-Raw** | Tab bar dùng raw `TextView` thay vì Material3 TabLayout |

---

| Activity / Layout | Vi phạm design token | Dòng / Vị trí | Đề xuất sửa |
|-------------------|---------------------|---------------|-------------|
| **SplashActivity** | HC-Dimen: `layout_marginBottom="32dp"` cho loading dots | L66 | Thêm `@dimen/splash_dot_bottom` = 32dp vào dimens.xml |
| **SplashActivity** | HC-Dimen: dot sizes 6dp, gap 6dp hardcode trong XML | L70–85 | Thêm `@dimen/loading_dot_size` = 6dp |
| **SplashActivity** | Tổng thể: clean — dùng `@dimen/text_h1`, `@dimen/text_micro`, `@dimen/space_4`, `@color/text_primary/secondary` ✅ | — | — |
| **SignUpActivity** | HC-Dimen: `android:padding="4dp"` trên avatar | L64 | Dùng `@dimen/space_1` (4dp) |
| **SignUpActivity** | HC-Dimen: `48dp` hardcode cho avatar size | L61 | Thêm `@dimen/avatar_sm` = 48dp |
| **SignUpActivity** | Chuỗi hint "Email", "Password", "Continue with Google/Apple" hardcode trong XML | L70, L91, L188, L196 | Extract sang `@string/hint_email`, `@string/hint_password`, `@string/sso_google` |
| **LoginActivity** | HC-Dimen: `48dp` avatar, `4dp` padding (giống SignUp) | L38 area | Như trên |
| **LoginActivity** | Chuỗi hint "Email", "Password" hardcode | L63, L83 | Extract sang `@string/` |
| **LoginActivity** | Tổng thể clean — dùng dimen tokens, Material3 TextInputLayout ✅ | — | — |
| **ForgotPasswordActivity** | Chuỗi hint "Email" hardcode | L67 | `@string/hint_email` |
| **ForgotPasswordActivity** | Tổng thể clean ✅ | — | — |
| **PersonalizeActivity** | Chuỗi "Skip" hardcode trong button | L55 | `@string/action_skip` |
| **PersonalizeActivity** | HC-Dimen: `android:progress="66"` hardcode (không phải dimension nhưng là magic number) | L47 | Acceptable — runtime progress value |
| **PersonalizeActivity** | `LinearProgressIndicator` dùng đúng ✅ | L38 | — |
| **OnboardingActivity** | Chuỗi "WELCOME", "Meet Milo, your AI mentor", body text, "Next" hardcode trong XML | L72, L81, L91, L130 | Extract toàn bộ sang `@string/onboard_*` |
| **OnboardingActivity** | HC-Dimen: blob `260dp` × `260dp` hardcode | L41–42 | `@dimen/onboard_blob_size` = 260dp |
| **OnboardingActivity** | HC-Dimen: dot active width `24dp`, dot size `8dp`, gap `6dp` hardcode | L110–118 | `@dimen/dot_active_width`, `@dimen/dot_size` |
| **OnboardingActivity** | Material3 `Widget.Material3.Button.TextButton` cho Skip ✅ | — | — |
| **HomeActivity** | HC-Color: `#80FFFFFF` (overlay gradient) và `#FFFFFF` hardcode trong hero card | L33, L53 (est.) | Thêm `@color/overlay_white_50` = `#80FFFFFF` vào colors.xml |
| **HomeActivity** | HC-Dimen: `android:padding="6dp"` (quick-start tile label area) | L148 | Dùng `@dimen/space_1` (4dp) hoặc thêm `@dimen/space_1_5` = 6dp |
| **HomeActivity** | HC-Dimen: `layout_marginBottom="84dp"` (bottom inset trên RecyclerView) | L174 | Thêm `@dimen/bottom_nav_inset` = 84dp |
| **ChatActivity** | Tổng thể clean — dùng đúng dimen tokens và `@color/*` ✅ | — | — |
| **AnswerActivity** | HC-Color: `#80FFFFFF`, `#FFFFFF` trong gradient hero card (final answer section) | hero card area | Dùng `@color/overlay_white_50` (xem Home fix) |
| **AnswerActivity** | Tổng thể design clean ngoài phần hero gradient | — | — |
| **AnswerTabbedActivity** | HC-Color: `#99F8EFD9` cho meta text trong dark header | L109, L116 | Thêm `@color/text_on_dark_tertiary` = `#99F8EFD9` |
| **AnswerTabbedActivity** | Tab-Raw: dùng raw `TextView` cho Solution/Concept/Practice/Pitfalls tabs thay vì Material3 | L135–153 | Dùng `TabLayout` + `TabItem` với `style="@style/Widget.Material3.TabLayout"` |
| **AnswerTabbedActivity** | HC-Dimen: `textSize="13sp"` inline cho tab labels | L136–152 | `@dimen/text_caption` (13sp = đúng giá trị, nhưng nên dùng token) |
| **AnswerTabbedActivity** | HC-Dimen: tab indicator `80dp` width, `2dp` height hardcode | L160–162 | `@dimen/tab_indicator_width` = 80dp; `2dp` → `@dimen/divider_height` |
| **HistoryActivity** | Tổng thể clean — top bar `paddingHorizontal=space_5` (20dp) ✅ | — | — |
| **DashboardActivity** | HC-Color: `#BF1A1610`, `#CC1A1610` alpha-blended trên streak hero text | streak hero area | Thêm `@color/text_on_dark_secondary` = `#BF2A2418`, `@color/text_on_dark_primary` = `#CC2A2418` |
| **DashboardActivity** | HC-Color: `app:tint="#7C5CE6"` trên 2 stat icons | stat icons | Thêm `@color/purple_accent` = `#7C5CE6` hoặc dùng `@color/brand_accent` nếu mục đích là accent |
| **DashboardActivity** | HC-Color: `app:tint="#4FA37A"` trên icon khác | stat icon | Thêm `@color/green_accent` = `#4FA37A` |
| **DashboardActivity** | HC-Dimen: `textSize="52sp"` cho streak number display | streak hero | Thêm `@dimen/text_hero` = 52sp vào dimens.xml |
| **DashboardActivity** | HC-Dimen: `textSize="16sp"` inline (gần với `@dimen/text_h3`=18sp nhưng không khớp) | L96 area | Dùng `@dimen/text_h3` hoặc thêm `@dimen/text_body_lg` = 16sp |
| **DashboardActivity** | HC-Dimen: weekly chart labels `textSize="10sp"`, `"11sp"` | chart area | `@dimen/text_micro` (11sp); 10sp → `@dimen/text_nano` = 10sp |
| **DashboardActivity** | Emoji 🔥 hardcode trong XML text | streak label | Extract sang `@string/label_streak_fire` |
| **ProfileActivity** | M3-Widget: `@android:style/Widget.ProgressBar.Horizontal` cho XP bar | XP bar | Dùng `com.google.android.material.progressindicator.LinearProgressIndicator` |
| **ProfileActivity** | HC-Dimen: `layout_width="280dp"` hardcode cho XP bar | XP bar | Dùng `layout_width="match_parent"` với constraints |
| **ProfileActivity** | HC-Color: `app:tint="#7C5CE6"` trên medals icon | profile area | Dùng `@color/purple_accent` (xem Dashboard) |
| **ProfileActivity** | HC-Dimen: `layout_marginTop="-16dp"` hardcode negative margin (badge grid overlap) | badge section | Sửa layout dùng `ConstraintLayout` với constraint âm thay vì hardcoded margin |
| **ProfileActivity** | Static placeholder text "4 / 8", "7", "1,840" trong XML | badge/XP area | Chỉ dùng `tools:text` nếu là placeholder; ensure populated từ code |
| **QuizActivity** | M3-Widget: `@android:style/Widget.ProgressBar.Horizontal` cho quiz progress | progress bar | Dùng `LinearProgressIndicator` |
| **QuizActivity** | HC-Dimen: `layout_width="140dp"` hardcode cho progress bar | progress area | `match_parent` với margin |
| **QuizActivity** | HC-Dimen: `textSize="13sp"` inline trên option circles | option circles | `@dimen/text_caption` |
| **QuizResultActivity** | HC-Color: `#CC1A1610` alpha-blended (2 lần) trong hero gradient card | L91, L113 | `@color/text_on_dark_primary` (xem Dashboard fix) |
| **QuizResultActivity** | HC-Dimen: `textSize="48sp"` cho score display | score text | Thêm `@dimen/text_score_hero` = 48sp |
| **QuizResultActivity** | HC-Dimen: `textSize="22sp"` (3 lần) cho reward numbers | reward row | Thêm `@dimen/text_reward` = 22sp; gần `@dimen/text_h2`=22sp — nên dùng `@dimen/text_h2` |
| **QuizResultActivity** | HC-Color: `app:tint="#7C5CE6"` trên badge icon | badge reward | `@color/purple_accent` |
| **QuizResultActivity** | Chuỗi "GREAT JOB!", "+45", "+1", "Day streak", "XP earned", "Badge", "Done", "Next quiz" hardcode trong XML | reward row | Extract sang `@string/` (phần lớn là static UI label, OK để extract) |
| **QuizResultActivity** | Hardcoded `layout_marginBottom="4dp"` và `3dp` trong reward cards | reward row | `@dimen/space_1` (4dp); 3dp → add `@dimen/space_0_75` = 3dp |
| **LeaderboardActivity** | HC-Color: `#C0C0C0` (silver), `#CD7F32` (bronze), `#F5B544` (gold tint) hardcode | podium bars | Thêm `@color/medal_gold` = `#F5B544`, `@color/medal_silver` = `#C0C0C0`, `@color/medal_bronze` = `#CD7F32` |
| **LeaderboardActivity** | Tab-Raw: dùng raw `TextView` cho Global/Friends/Weekly tabs | tab bar | Dùng `TabLayout` (như AnswerTabbed fix) |
| **LeaderboardActivity** | HC-Dimen: `textSize="13sp"` inline trong tab TextViews | tab bar | `@dimen/text_caption` |
| **LeaderboardActivity** | HC-Dimen: timer pill height `28dp` hardcode | timer pill | `@dimen/chip_height_sm` = 28dp |
| **LeaderboardActivity** | Inconsist: Tab bar pattern khác toàn bộ app (raw TextView) — các màn hình khác không có TabLayout | tab bar | TabLayout cho thống nhất, hoặc dùng `ChipGroup` single-selection như filter chips ở History/Notifications |
| **NotificationsActivity** | Inconsist: `paddingHorizontal=space_4` (16dp) cho top bar vs History `space_5` (20dp) | top bar | Thống nhất về `@dimen/space_5` (20dp) hoặc `@dimen/space_4` (16dp) — chọn một chuẩn |
| **SettingsActivity** | HC-Dimen: `android:padding="4dp"` trên avatar | avatar | `@dimen/space_1` |
| **SettingsActivity** | HC-Dimen: `48dp` avatar size hardcode | avatar | `@dimen/avatar_sm` = 48dp |
| **SettingsActivity** | Tổng thể clean — dùng Card.Default, dimen tokens ✅ | — | — |
| **TwoFAActivity** | M3-Widget: `@android:style/Widget.ProgressBar.Horizontal` cho step progress bar | top bar | `LinearProgressIndicator` |
| **TwoFAActivity** | HC-Dimen: `textSize="22sp"` trên OTP digits | OTP boxes | `@dimen/text_h2` (22sp — khớp giá trị, dùng token) |
| **TwoFAActivity** | HC-Dimen: `textSize="13sp"` trên resend button | resend row | `@dimen/text_caption` |
| **TwoFAActivity** | HC-Dimen: OTP box height `52dp` hardcode | OTP boxes | Thêm `@dimen/otp_box_height` = 52dp |
| **CameraActivity** | HC-Color: nhiều màu dùng hex trực tiếp cho fullscreen dark overlay | toàn bộ file | Thêm vào `colors.xml`: `@color/overlay_dark_80` = `#CC1A1610`, `@color/overlay_dark_50` = `#80000000`, `@color/text_on_dark` = `#FFF6E0`, `@color/text_on_dark_dim` = `#B5A57E` |
| **CameraActivity** | HC-Dimen: `textSize="9sp"` cho gallery/flip labels, `"12sp"` hint, `"15sp"` processing, `"18sp"` permission title | bottom bar, overlays | Thêm dimen tokens: `text_nano`=9sp, `text_small`=12sp; dùng `text_body`=15sp, `text_h3`=18sp |
| **CameraActivity** | HC-Dimen: padding `14dp`, `28dp`, `24dp`, `20dp` raw values | top/bottom bars | Dùng `@dimen/space_3`=12dp, `@dimen/space_4`=16dp, `@dimen/space_5`=20dp, `@dimen/space_6`=24dp |
| **ScanPreviewActivity** | HC-Color: `app:cardBackgroundColor="#1A1610"` trên photo card | photo card | Dùng `@color/text_primary` (hiện là `#2A2418`) hoặc thêm `@color/surface_dark` = `#1A1610` |
| **ScanPreviewActivity** | HC-Color: `android:background="#EDE1C0"` trên ImageView placeholder | photo placeholder | Thêm `@color/surface_warm` = `#EDE1C0` |
| **ScanPreviewActivity** | HC-Dimen: `paddingStart/End="16dp"`, `paddingTop="8dp"`, `paddingBottom="16dp"` raw trong ScrollView content | scroll content | Dùng `@dimen/space_4` (16dp), `@dimen/space_2` (8dp) |
| **ScanPreviewActivity** | HC-Dimen: `layout_marginTop="14dp"` giữa các card, `"12dp"`, `"10dp"` | card gaps | Dùng `@dimen/space_3`=12dp, `@dimen/space_3`=12dp. 14dp không có token — thêm hoặc round về 12/16dp |
| **ScanPreviewActivity** | HC-Dimen: `textSize="11sp"` confidence, `"14sp"` OCR text, `"12sp"` hint | text areas | `@dimen/text_micro`=11sp, `@dimen/text_body_sm`=14sp, tạo `@dimen/text_small`=12sp |
| **ScanPreviewActivity** | Inconsist: dùng `MaterialToolbar` thay vì custom LinearLayout như các màn hình khác | top bar | OK nếu muốn chuẩn hóa về MaterialToolbar; hiện tại tất cả Activity khác dùng custom LinearLayout |

---

## Spacing/Padding Inconsistency — Màn hình cùng loại

| Nhóm màn hình | Thuộc tính | Giá trị hiện tại | Đề xuất chuẩn |
|---------------|------------|-----------------|---------------|
| **Top bar paddingHorizontal** | History | `@dimen/space_5` (20dp) | Chọn 1 chuẩn |
| | Notifications | `@dimen/space_4` (16dp) | → Chuẩn về `@dimen/space_5` |
| | Leaderboard, Dashboard | `@dimen/space_4` hoặc raw | → `@dimen/space_5` |
| | Settings | `@dimen/space_5` (20dp) ✅ | |
| **Content body paddingHorizontal** | SignUp, Login, ForgotPassword | `@dimen/space_6` (24dp) ✅ | `@dimen/space_6` = chuẩn auth screens |
| | Personalize, TwoFA, Onboarding | `@dimen/space_5` hoặc `space_6` | Nhất quán về `space_6` cho auth/onboarding |
| | Home, History, Chat | `@dimen/space_5` (20dp) ✅ | `@dimen/space_5` = chuẩn main screens |
| | ScanPreview | raw `16dp` | → `@dimen/space_4` (16dp) |
| **Tab bar implementation** | History/Notifications (filter) | Material3 `ChipGroup` single-select ✅ | |
| | Leaderboard (mode tabs) | raw `TextView` ❌ | → `TabLayout` |
| | AnswerTabbed (content tabs) | raw `TextView` ❌ | → `TabLayout` |
| **ProgressBar (horizontal)** | Personalize | `LinearProgressIndicator` (M3) ✅ | |
| | Quiz, Profile, TwoFA, SubjectStatRow | `@android:style/Widget.ProgressBar.Horizontal` ❌ | → `LinearProgressIndicator` |

---

## Màu hex lạ (không thuộc design system hiện tại) — cần thêm vào colors.xml

| Hex | Ý nghĩa ngữ nghĩa | Đề xuất token |
|-----|------------------|---------------|
| `#7C5CE6` | Purple — badge/reward icon | `@color/purple_accent` |
| `#4FA37A` | Green — growth stat icon | `@color/green_accent` |
| `#C0C0C0` | Silver medal | `@color/medal_silver` |
| `#CD7F32` | Bronze medal | `@color/medal_bronze` |
| `#F5B544` | Gold medal (đã là `brand_primary`) | Dùng `@color/brand_primary` |
| `#CC1A1610` | Dark overlay 80% (text_primary 80%) | `@color/overlay_dark_80` |
| `#BF1A1610` | Dark overlay 75% | `@color/overlay_dark_75` |
| `#80000000` | Pure black overlay 50% | `@color/overlay_black_50` |
| `#FFF6E0` | Warm cream on dark (camera) | `@color/text_on_dark` |
| `#B5A57E` | Muted cream on dark (camera) | `@color/text_on_dark_dim` |
| `#99F8EFD9` | Light cream 60% (AnswerTabbed) | `@color/text_on_dark_tertiary` |
| `#EDE1C0` | Warm tan (scan placeholder bg) | `@color/surface_warm` |
| `#1A1610` | Deep dark (scan photo card) | `@color/surface_dark` |
| `#80FFFFFF` | White overlay 50% (home/answer hero) | `@color/overlay_white_50` |

---

## Item Layout — Kích thước text không chuẩn

Các item layout dùng nhiều `textSize` hardcode không nằm trong `dimens.xml`:

| Item layout | TextSize dùng | Dimen token gần nhất | Ghi chú |
|-------------|--------------|---------------------|---------|
| `item_quiz_answer_row` | `13sp`, `12sp` | `text_caption`=13sp, `text_small`=12sp | Cần thêm `text_small`=12sp |
| `item_subject_stat_row` | `13.5sp`, `12sp` | Không có token 13.5sp | Dùng `text_caption`=13sp |
| `item_activity_row` | `13.5sp`, `11.5sp` | Không có token | Dùng `text_caption`=13sp, `text_micro`=11sp |
| `item_rank_row` | `16sp`, `14sp`, `13sp`, `10sp` | `text_h3`=18sp, `text_body_sm`=14sp, `text_caption`=13sp, `text_micro`=11sp | 16sp → thêm `text_body_lg`=16sp; 10sp → thêm `text_nano`=10sp |
| `item_notification_row` | `13.5sp`, `12.5sp`, `11sp` | Không có token 13.5/12.5sp | Round về 13sp/13sp/11sp |
| `item_badge_cell` | `10sp`, `9sp` | `text_micro`=11sp | 9sp là nhỏ nhất app — thêm `text_nano`=9sp hoặc raise lên 11sp |

---

## Tổng hợp vi phạm theo loại

| Loại vi phạm | Số file bị ảnh hưởng | Mức độ | Ghi chú |
|-------------|---------------------|--------|---------|
| **Color system drift** (Session 4) | Toàn bộ app | Hệ thống | Amber palette thay indigo — cần quyết định giữ hay revert |
| **Thiếu font** (Bricolage Grotesque / Plus Jakarta Sans) | Toàn bộ app | Hệ thống | Cần thêm font files + khai báo trong themes.xml |
| **HC-Color** — hex cứng trong XML layout | 9 activity + 1 item | Trung | Nhiều nhất ở Camera, Dashboard, QuizResult, AnswerTabbed |
| **M3-Widget** — ProgressBar.Horizontal cũ | 3 activity + 1 item | Trung | Quiz, Profile, TwoFA, item_subject_stat_row |
| **Tab-Raw** — raw TextView thay TabLayout | 2 activity | Trung | Leaderboard, AnswerTabbed |
| **HC-Dimen** — raw dp/sp inline | 15+ activity + 6 item | Thấp | item layouts và Camera nặng nhất |
| **Inconsist** — spacing giữa màn hình cùng loại | 3 nhóm | Thấp | Top bar padding, tab implementation, ProgressBar variant |

---

## Ưu tiên sửa

### Cao — Ảnh hưởng trực quan rõ ràng
1. **Thêm font Bricolage Grotesque + Plus Jakarta Sans** vào `res/font/`, khai báo trong `TextAppearance.*` styles — tất cả `@style/Text.*` tự kế thừa
2. **Quyết định về color system**: giữ amber (Logo.html) hay revert về indigo (spec cũ) — sau đó audit lại token usage
3. **`@android:style/Widget.ProgressBar.Horizontal` → `LinearProgressIndicator`** trong Quiz, Profile, TwoFA, item_subject_stat_row (4 file, ~10 phút)
4. **Tab bar** LeaderboardActivity và AnswerTabbedActivity → Material3 TabLayout

### Trung — Khai báo token còn thiếu
5. **Thêm vào `colors.xml`**: `overlay_dark_80`, `overlay_dark_75`, `overlay_white_50`, `purple_accent`, `green_accent`, `medal_silver`, `medal_bronze`, `overlay_black_50`, `text_on_dark`, `text_on_dark_dim`, `text_on_dark_tertiary`, `surface_warm`, `surface_dark`
6. **Thêm vào `dimens.xml`**: `text_hero`=48sp, `text_score_hero`=48sp, `text_reward`=22sp (có thể reuse `text_h2`), `text_nano`=9sp, `text_small`=12sp, `text_body_lg`=16sp, `avatar_sm`=48dp, `bottom_nav_inset`=84dp, `otp_box_height`=52dp
7. **Chuẩn hóa top bar paddingHorizontal** — chọn một trong `space_4` (16dp) hoặc `space_5` (20dp) và áp đồng đều

### Thấp — Refactor khi tiện
8. **item_*.xml** — migrate `textSize` raw values về dimen tokens sau khi step 6 hoàn tất
9. **CameraActivity** padding/textSize raw values (camera UI design khác biệt intentional, thấp ưu tiên)
10. **ScanPreviewActivity** padding/margin raw values
11. **Hint text** "Email", "Password" trong TextInputLayout → `@string/hint_email`
