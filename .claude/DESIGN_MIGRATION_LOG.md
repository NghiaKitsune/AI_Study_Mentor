# Design Migration Log

Tài liệu theo dõi quá trình tích hợp Claude Design vào từng màn hình.
Design bundle: `BonpCZVk9_BfLfOKdD0mEw` · File handoff: `Redesign Home & Profile.html`

---

## 1. Trạng thái theo màn hình

### Main Screens

| Activity | Trạng thái | Commit |
|----------|-----------|--------|
| HomeActivity | ✅ Xong | session 6 |
| ProfileActivity | ✅ Xong | session 6 |

### History & Practice

| Activity | Trạng thái |
|----------|-----------|
| HistoryActivity | ✅ Xong | session 7 / d442922 |
| QuizActivity | ✅ Xong | session 7 / a20c044 |
| QuizResultActivity | ✅ Xong | session 8 / ea91bc4 |

### Onboarding & Auth

| Activity | Trạng thái |
|----------|-----------|
| OnboardingActivity | ⬜ Chưa làm |
| SplashActivity | ⬜ Chưa làm |
| SignUpActivity | ⬜ Chưa làm |
| LoginActivity | ⬜ Chưa làm |
| ForgotPasswordActivity | ⬜ Chưa làm |
| PersonalizeActivity | ⬜ Chưa làm |
| TwoFAActivity | ⬜ Chưa làm |

### Chat & Capture

| Activity | Trạng thái |
|----------|-----------|
| ChatActivity | ⬜ Chưa làm |
| AnswerActivity | ✅ Xong | session 8 / d9167ee |
| AnswerTabbedActivity | ✅ Xong | session 9 / 6613965 |
| CameraActivity | ⬜ Chưa làm |
| ScanPreviewActivity | ⬜ Chưa làm |

### Stats & Social

| Activity | Trạng thái |
|----------|-----------|
| DashboardActivity | ⬜ Chưa làm |
| LeaderboardActivity | ⬜ Chưa làm |
| NotificationsActivity | ⬜ Chưa làm |

### Utility

| Activity | Trạng thái |
|----------|-----------|
| SettingsActivity | ⬜ Chưa làm |
| MainActivity | ⬜ Chưa làm |

---

## 2. Token mới đã thêm vào design system

Thêm trong session 6 khi map giá trị từ Claude Design handoff sang Android tokens.

| Token | File | Giá trị | Lý do thêm |
|-------|------|---------|-----------|
| `brand_primary_gradient_end` | colors.xml | `#E89620` | Màu kết thúc gradient amber 315° trên challenge card và profile header |
| `text_label` | dimens.xml | `12sp` | Sub-label size: chip subject, XP label, tile subtitle — gap giữa text_micro (11sp) và text_caption (13sp) |
| `text_ring_value` | dimens.xml | `28sp` | Số lớn trong progress ring ("0") — giữa text_h1 (26sp) và text_display (32sp) |
| `progress_ring_size` | dimens.xml | `78dp` | Đường kính vòng tròn progress ring trong challenge card và profile XP ring |
| `avatar_xl` | dimens.xml | `84dp` | Avatar lớn trên Profile header — lớn hơn icon_button_lg (44dp), khác với mascot_md (72dp) |
| `bottomnav_indicator_size` | dimens.xml | `30dp` | Indicator tròn (pill) của BottomNav theo design — thay thế 80×90dp cũ |
| `progress_bar_height` | dimens.xml | `8dp` | Chiều cao XP progress bar nằm ngang — dùng chung Profile + Dashboard |
| `color_ok` | colors.xml | `#4FA37A` | Trạng thái đúng/giải xong/pass — cùng hex subject_science nhưng ngữ nghĩa khác |
| `color_ok_soft` | colors.xml | `#E1F0E8` | Nền nhạt cho trạng thái ok (quiz correct, SOLVED badge) |
| `dark_header` | colors.xml | `#1C1710` | Header tối của AnswerTabbedActivity |
| `text_score_hero` | dimens.xml | `62sp` | Số điểm lớn ở QuizResultActivity hero |

---

## 3. Giá trị đã làm tròn về token có sẵn

Không tạo token mới cho những giá trị này — làm tròn khi áp dụng vào layout.

### Spacing (4dp grid rounding)

| Giá trị gốc (CSS/design) | Token đích | Chênh lệch |
|--------------------------|-----------|-----------|
| 6dp | `space_2` (8dp) | +2dp |
| 10dp | `space_3` (12dp) | +2dp |
| 14dp | `space_4` (16dp) | +2dp |
| 18dp | `space_5` (20dp) | +2dp |

### Typography

| Giá trị gốc | Token đích | Ghi chú |
|-------------|-----------|---------|
| 9sp | `text_micro` (11sp) | Quá nhỏ để thêm riêng |
| 10sp | `text_micro` (11sp) | Quá nhỏ để thêm riêng |
| 17sp | `text_h3` (18sp) | Gần nhất, chênh 1sp không đáng kể |

### Dimensions

| Giá trị gốc | Token đích | Ghi chú |
|-------------|-----------|---------|
| 38dp (act-icon container) | `icon_button_md` (40dp) | Gần nhất trong thang icon button |

---

## 4. Log theo session

<!-- Append mỗi session khi hoàn thành redesign 1 màn hình trở lên -->

| Session | Ngày | Màn hình hoàn thành | Ghi chú |
|---------|------|---------------------|---------|
| 6 | 2026-06-18 | HomeActivity, ProfileActivity | Redesign Home & Profile.html |
| 7 | 2026-06-19 | HistoryActivity, QuizActivity | Phase 0 tokens + Phase 1 History + Phase 2 Quiz · df5bae5 / d442922 / a20c044 |
| 8 | 2026-06-19 | QuizResultActivity | Phase 3: mascot hero + 3 stats cards + subject breakdown + real data · ea91bc4 |
| 9 | 2026-06-19 | AnswerActivity, AnswerTabbedActivity | Phase 4: AnswerActivity custom appbar + hero gradient + follow-up chips; Phase 5: AnswerTabbedActivity dark_header + tab-ic circles + green SOLVED + full-width tab bar + styled content · d9167ee / 6613965 |
