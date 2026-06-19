# Compliance Audit Report
Generated: 2026-06-17

Scope: 22 Activities + 5 sensitive files
Rules audited: 10 mandatory conventions from CLAUDE.md §Conventions & Rules
Source reads: every .java file under app/src/main/java + AndroidManifest.xml, build.gradle, themes.xml, AppDatabase.java, Session.java

---

## Legend

| Code | Rule |
|------|------|
| R1 | Java only — no Kotlin / Compose |
| R2 | No Fragment |
| R3 | No NavComponent |
| R4 | XML layouts — no programmatic view creation for UI structure |
| R5 | Material 3 `Widget.Material3.*` styles only |
| R6 | No hardcoded strings in Java — use `R.string.*` |
| R7 | No hardcoded colors in Java — use `ContextCompat.getColor(this, R.color.*)` |
| R8 | Room on background thread — wrap in `executor().execute()` |
| R9 | ViewBinding — use `ActivityXxxBinding.inflate()` not `findViewById` |
| R10 | `SplashScreen.installSplashScreen(this)` BEFORE `super.onCreate()` |

Severity: **Cao** = crash / data-loss / security risk · **Trung** = must fix before production · **Thấp** = style deviation

---

## Activity / File Audit

| Activity / File | Quy tắc vi phạm | Dòng code cụ thể | Mức độ |
|-----------------|-----------------|------------------|--------|
| **SplashActivity** | R9 — `findViewById` thay vì ViewBinding | L33: `android.view.View mascot = findViewById(R.id.container_app_icon)` | Thấp |
| **MainActivity** | Đạt — không có layout/ViewBinding cần thiết (router thuần, không gọi `setContentView`) | — | — |
| **LoginActivity** | R9 — `findViewById` toàn bộ | L30–33: `tilEmail`, `tilPassword`, `inputEmail`, `inputPassword` qua `findViewById` | Thấp |
| **SignUpActivity** | R7 — `setBackgroundResource()` với `R.color.*` resource ID (sai API — nên dùng `setBackgroundColor(ContextCompat.getColor(...))`) | L84: `strengthBars[i].setBackgroundResource(color)` | Trung |
| **SignUpActivity** | R9 — `findViewById` toàn bộ | L39–44 | Thấp |
| **ForgotPasswordActivity** | R9 — `findViewById` toàn bộ | L32–36 | Thấp |
| **PersonalizeActivity** | R9 — `findViewById` toàn bộ | L26–27 | Thấp |
| **OnboardingActivity** | R4 — đặt kích thước layout bằng code Java thay vì XML (`lp.width = dp * density`) | L72: `lp.width = (int)(24 * getResources().getDisplayMetrics().density)` · L75: `lp.width = (int)(8 * ...)` | Thấp |
| **OnboardingActivity** | R9 — `findViewById` toàn bộ | L34–40 | Thấp |
| **HomeActivity** | R6 — chuỗi hardcode trong `setText` | L88: `streak + " days"` · L111: `" · Level "` · L112: `" / "` và `" XP"` · L118–121: `"Solve a problem"`, `"Ask a question"`, `"Debug some code"`, `"Explore a topic"` · L175–179: `"Master"`, `"Expert"`, `"Scholar"`, `"Explorer"`, `"Beginner"` | Trung |
| **HomeActivity** | R9 — `findViewById` toàn bộ | L76–77, L87, L94, L96, L107–113, L128 | Thấp |
| **ChatActivity** | R6 — chuỗi hardcode trong Snackbar | L196: `"Step-by-step breakdown ready"` · L197: `"View"` | Trung |
| **ChatActivity** | R8 — DB write (INSERT) trên main thread | L138: `questionDao().insert(q)` — trả về ID nên khó tách executor; cần refactor · L142: `messageDao().insert(userMsg)` — không cần return value, phải dùng `executor.execute()` | Trung |
| **ChatActivity** | R9 — `findViewById` toàn bộ | L62–68 | Thấp |
| **AnswerActivity** | R6 — chuỗi hardcode | L105: `s.title = "Full answer"` · L177: `"Q: "`, `"\n\nA: "`, `"\n\n— shared from AI Study Mentor"` | Trung |
| **AnswerActivity** | R8 — DB write (UPDATE) trên main thread | L162: `questionDao().update(question)` khi toggle bookmark | Trung |
| **AnswerActivity** | R9 — `findViewById` toàn bộ | L48, L54–58, L81, L118–129, L157 | Thấp |
| **HistoryActivity** | R6 — chuỗi hardcode | L182: `"You've asked " + count + " questions. Want a quick review quiz?"` | Trung |
| **HistoryActivity** | R8 — DB write (DELETE) trên main thread | L121: `questionDao().delete(q)` trong `showDeleteDialog()` | Trung |
| **HistoryActivity** | R9 — `findViewById` toàn bộ | L48–50 | Thấp |
| **DashboardActivity** | R6 — tên môn học và format string hardcode | L59–62: `"Math"`, `"Coding"`, `"Science"`, `"Languages"` · L96 (SubjectAdapter): `s.count + " Qs · " + s.pct + "%"` | Trung |
| **DashboardActivity** | R9 — `findViewById` | L43–44, L47–48, L65 | Thấp |
| **ProfileActivity** | R6 — badge labels/descriptions và activity feed texts hardcode | L61–68: tất cả `BadgeItem` label/desc (`"Week Warrior"`, `"7-day streak"`, `"First Steps"` …) · L78–81: tất cả `ActivityItem` text (`"Earned \"Sharp Shooter\" badge"` …) · L48: `"Nghia Mentor"` | Trung |
| **ProfileActivity** | R9 — `findViewById` | L40–41, L50, L55–58, L71, L84 | Thấp |
| **QuizActivity** | R6 — chuỗi hardcode phổ biến | L70: `"QUESTION " + (idx+1) + " / " + questions.size()` · L74: `" · MULTIPLE CHOICE"` · L98: `"Check answer"` · L126: `"0:00"` · L162: `"✓"` · L168: `"✗"` · L178: `"TIME'S UP"` · L181: `"CORRECT"` · L184: `"INCORRECT"` · L191: `"Next question"`, `"See results"` | Trung |
| **QuizActivity** | R9 — `findViewById` | L65, L69, L75–76, L78, L84, L89, L97, L105, L114, L119, L133, L172–176, L189 | Thấp |
| **QuizResultActivity** | R6 — format string và share text hardcode | L43: `score + " / " + total` · L44: `pct + "% accuracy · Quiz result"` · L79: `"I scored " + score + "/" + total + "…"` + `"— shared from AI Study Mentor"` | Trung |
| **QuizResultActivity** | R9 — `findViewById` | L43–46, L50, L51, L56, L60, L72 | Thấp |
| **LeaderboardActivity** | R6 — data podium và rank list hardcode | L41–45: `"AN"`, `"AnNguyen"`, `"5,460"`, … (6 entries) | Thấp |
| **LeaderboardActivity** | R9 — `findViewById` | L46–53, L68, L74–77 | Thấp |
| **NotificationsActivity** | R6 — chuỗi unread count và toàn bộ notification content hardcode | L41: `unread + " new"`, `"All caught up"` · L73–98: 6 `NotifItem` với title/body/time hardcode | Thấp |
| **NotificationsActivity** | R9 — `findViewById` | L40, L43, L55 | Thấp |
| **SettingsActivity** | R6 — giá trị hiển thị hardcode | L84: `"English"` · L92: `"On"` | Trung |
| **SettingsActivity** | R9 — `findViewById` | L31, L33–34, L37, L50–53, L81–84, L89–92, L102 | Thấp |
| **TwoFAActivity** | R6 — Toast và countdown text hardcode | L41: `"New code sent!"` · L70: `"Code expires in 0:"` · L73: `"Code expired"` · L84: `"Enter all 6 digits"` · L88: `"2FA enabled successfully!"` | Trung |
| **TwoFAActivity** | R9 — `findViewById` | L48, L66 | Thấp |
| **CameraActivity** | R7 — màu hex literal trong `cycleFlash()` | L224: `0xCCF5B544` · L225: `0xFF1A1610` · L232: `0x80000000` · L233: `0xFFFFFFFF` | **Cao** |
| **CameraActivity** | R9 — `findViewById` | L89–92, L94–102 | Thấp |
| **ScanPreviewActivity** | R6 — chuỗi hardcode | L149: `Toast.makeText(this, "Copied", ...)` · L164: `"Add a question first"` · L148: `ClipData.newPlainText("recognized", text)` | Trung |
| **ScanPreviewActivity** | R9 — `findViewById` | L84–98 | Thấp |
| **AnswerTabbedActivity** | R4 — tạo `TextView` bằng code Java, `addView()` vào container | L88: `new TextView(this)` · L93: `container.addView(tv)` · L92: `tv.setTextSize(14)` hardcode sp | **Cao** |
| **AnswerTabbedActivity** | R6 — toàn bộ nội dung tab hardcode | L32: default question string · L71–86: `titles[]` và `bodies[]` với 4 tab content | Trung |
| **AnswerTabbedActivity** | R7 — `getColor()` trực tiếp từ Activity thay vì `ContextCompat.getColor()` | L62: `getColor(active ? R.color.text_primary : R.color.text_tertiary)` · L90: `tv.setTextColor(getColor(R.color.text_primary))` | Thấp |
| **AnswerTabbedActivity** | R9 — `findViewById` | L31, L35–39, L49–55 | Thấp |
| **DashboardActivity.SubjectAdapter** | R7 — `context.getColor()` thay vì `ContextCompat.getColor()` (hoạt động OK trên API33+ nhưng vi phạm convention) | L100: `h.itemView.getContext().getColor(s.colorRes)` · L102 | Thấp |

---

## Sensitive File Risk Audit

| File | Rủi ro | Mức độ |
|------|--------|--------|
| **AndroidManifest.xml** | `android:allowBackup="true"` — Android backup có thể export SharedPreferences chứa `auth_token` dạng plaintext | Trung |
| **AndroidManifest.xml** | `READ_MEDIA_IMAGES` được khai báo nhưng không cần thiết trên API33+ khi dùng Photo Picker (`ActivityResultContracts.PickVisualMedia`) — thừa permission | Thấp |
| **AndroidManifest.xml** | Tất cả non-launcher Activity đều `android:exported="false"` ✅ · SplashActivity exported=true (đúng, launcher) ✅ | — |
| **build.gradle** | `API_BASE_URL` là placeholder `api.studymentor.example.com` cho CẢ debug lẫn release. Release build có `USE_MOCK_AI=false` → Retrofit gọi endpoint không tồn tại → mọi request fail. Phải cập nhật trước `assembleRelease` | **Cao** |
| **themes.xml** | Tất cả style kế thừa từ `Theme.Material3.*` / `Widget.Material3.*` ✅ · Không có hex color inline ✅ · `Theme.StudyMentor.FullscreenBlack` dùng `@android:color/black` (system resource, OK) ✅ | — |
| **AppDatabase.java** | `fallbackToDestructiveMigration()` — bất kỳ thay đổi `version` nào sẽ xóa toàn bộ dữ liệu người dùng. Đã documented trong CLAUDE.md. Version vẫn là 1 → không có rủi ro ngay lập tức | Thấp |
| **AppDatabase.java** | `allowMainThreadQueries()` — tất cả reads đang chạy trên main thread. Documented là known stub trong CLAUDE.md | Thấp |
| **Session.java** | `auth_token` lưu trong `SharedPreferences` plaintext (`PreferenceManager.getDefaultSharedPreferences`). File tự document: "Replace with EncryptedSharedPreferences before shipping." | Trung |
| **Session.java** | Logic `updateStreak()`, `saveQuizResult()`, tất cả keys đều nhất quán và không có key conflict ✅ | — |

---

## Tổng hợp vi phạm theo quy tắc

| Quy tắc | Số Activity vi phạm | Mức độ cao nhất | Ghi chú |
|---------|---------------------|-----------------|---------|
| R1 — Java only | 0 | — | Toàn bộ codebase Java thuần ✅ |
| R2 — No Fragment | 0 | — | Không có Fragment import nào ✅ |
| R3 — No NavComponent | 0 | — | Không có NavComponent ✅ |
| R4 — Không tạo View bằng code | 2 | **Cao** | `AnswerTabbedActivity` (new TextView + addView) · `OnboardingActivity` (programmatic LayoutParams width) |
| R5 — Material 3 styles | 0 | — | Tất cả style trong XML layout đều dùng Material3 parent ✅ |
| R6 — Không hardcode string | 16 | Trung | Phổ biến nhất: Quiz screens, Profile, Home, TwoFA |
| R7 — Không hardcode color | 3 | **Cao** | `CameraActivity.cycleFlash()` dùng literal hex: `0xCCF5B544`, `0xFF1A1610`, `0x80000000`, `0xFFFFFFFF` · `SignUpActivity.setBackgroundResource()` sai API |
| R8 — Room off main thread | 4 | Trung | Writes còn trên main thread: `ChatActivity` L138/L142 · `AnswerActivity` L162 · `HistoryActivity` L121 |
| R9 — ViewBinding | 21 | Thấp | Toàn bộ Activities dùng `findViewById`. `MainActivity` exception hợp lệ (không có layout). Binding classes đã được generate (viewBinding=true trong build.gradle) nhưng không sử dụng |
| R10 — SplashScreen trước super | 0 | — | `SplashActivity` L28 đúng thứ tự ✅ |

---

## Ưu tiên sửa

### Cao — Sửa ngay

1. **CameraActivity lines 223–233** — thay 4 hex literal (`0xCCF5B544` v.v.) bằng `R.color.*` mới và `ContextCompat.getColorStateList()`
2. **AnswerTabbedActivity lines 68–93** — chuyển nội dung tab vào XML layout (ConstraintLayout + TextView riêng) thay vì `new TextView(this)` + `addView()`
3. **build.gradle** — cập nhật `API_BASE_URL` release thành endpoint thật trước `assembleRelease`

### Trung — Sửa trước demo

4. **ChatActivity L142** — wrap `messageDao().insert(userMsg)` trong `executor.execute()`
5. **AnswerActivity L162** — wrap `questionDao().update(question)` trong `executor.execute()`
6. **HistoryActivity L121** — wrap `questionDao().delete(q)` trong `executor.execute()`
7. **ChatActivity L138** — `insert(q)` trả về ID: refactor thành callback hoặc `Future<Long>` trên executor
8. **SignUpActivity L84** — thay `setBackgroundResource(R.color.x)` bằng `setBackgroundColor(ContextCompat.getColor(this, R.color.x))`
9. **R6 violations** — các chuỗi trong QuizActivity, TwoFAActivity, SettingsActivity, AnswerActivity có ảnh hưởng UX cao nhất (hiển thị cho user); extract vào `strings.xml`
10. **AndroidManifest** — xem xét `android:allowBackup="false"` hoặc dùng `EncryptedSharedPreferences` cho token

### Thấp — Refactor khi tiện

11. **R9 (ViewBinding)** — migrate từng Activity; bắt đầu từ `ChatActivity` + `HomeActivity` (phức tạp nhất)
12. **OnboardingActivity L72/75** — extract `24dp` và `8dp` vào `dimens.xml`, dùng `getResources().getDimensionPixelSize(R.dimen.*)`
13. **AndroidManifest** — remove `READ_MEDIA_IMAGES` (không cần với Photo Picker API33+)
