# Security Audit Report — Auth Flow
Generated: 2026-06-17

Scope: Session.java · LoginActivity · SignUpActivity · ForgotPasswordActivity · TwoFAActivity · ApiClient.java · proguard-rules.pro · AndroidManifest.xml · backup_rules.xml · data_extraction_rules.xml

Context: Đồ án học thuật — kiểm tra lỗi sơ đẳng phổ biến, không audit mức enterprise.

---

## Bảng tổng hợp

| # | Vị trí | Vấn đề | Mức độ | Đề xuất |
|---|--------|--------|--------|---------|
| S1 | `Session.java` L44–51 | `auth_token` lưu **plaintext** trong `SharedPreferences` mặc định (`PreferenceManager.getDefaultSharedPreferences`). Đọc được bằng ADB backup hoặc trên thiết bị root. | **Trung** | Thay bằng `EncryptedSharedPreferences` (Jetpack Security). File đã tự ghi chú: "Replace before shipping." |
| S2 | `Session.java` L44 | Token format là `"mock-token-" + System.currentTimeMillis()` — **tuần tự, đoán được**, không có entropy. Không phải JWT hay random UUID. | **Thấp** | Chỉ cần đổi khi nối backend thật: dùng JWT hoặc `UUID.randomUUID().toString()` cho mock token trong dev |
| S3 | `backup_rules.xml` L4 + `data_extraction_rules.xml` L5 | Đường dẫn loại trừ backup là **`auth_token.xml`** — file này không tồn tại. Auth token thực tế nằm trong file `com.studymentor.app_preferences.xml` (tên file của `PreferenceManager.getDefaultSharedPreferences`). Kết quả: **backup loại trừ sai target**, toàn bộ SharedPreferences kể cả `auth_token` + `user_email` vẫn được backup lên Cloud. | **Cao** | Sửa exclusion thành `path="com.studymentor.app_preferences"` (bỏ phần `.xml`). Hoặc tốt hơn: dùng `EncryptedSharedPreferences` để token được mã hóa ngay cả khi backup |
| S4 | `data_extraction_rules.xml` L7–9 | Khối `<device-transfer>` có `<include domain="sharedpref" path="."/>` **không có exclude** — toàn bộ SharedPreferences, bao gồm `auth_token`, được chuyển khi user đổi điện thoại qua Device Transfer. | **Trung** | Thêm `<exclude domain="sharedpref" path="com.studymentor.app_preferences"/>` vào `<device-transfer>`, hoặc chỉ exclude file prefs chứa auth |
| S5 | `LoginActivity.java` L51 + `SignUpActivity.java` L90 | Password đọc vào biến `String pw` trong bộ nhớ heap. Không có `Log.*` nào trong file → **không bị lộ ra logcat** ✅. Tuy nhiên `String` trong Java là immutable và không thể xóa khỏi memory chủ động. | **Thấp** | Đủ cho học thuật. Production: đọc password vào `char[]`, xóa bằng `Arrays.fill(arr, '\0')` sau dùng |
| S6 | `LoginActivity.java` L53–56 + `SignUpActivity.java` L92–95 | Validation email dùng `android.util.Patterns.EMAIL_ADDRESS` ✅. Validation password chỉ kiểm tra **độ dài ≥ 8** ✅. Không có `Log.*` lộ email hay password ✅. | — | Đạt — đủ cho học thuật |
| S7 | `SignUpActivity.java` L98–101 | Enforce **length ≥ 8** nhưng **không enforce điểm strength tối thiểu** — strength meter chỉ là gợi ý UI, không block đăng ký bằng mật khẩu yếu như `"12345678"` (đủ dài nhưng điểm = 1). | **Thấp** | Thêm kiểm tra `score < 2` → `tilPassword.setError(getString(R.string.error_password_weak))` trước khi cho phép đăng ký |
| S8 | `ForgotPasswordActivity.java` L56–57 | `mockSendResetLink(email)` là **no-op** nhưng `showSuccess(email)` luôn hiển thị thành công bất kể email có tồn tại hay không → **lộ behavior**: khi nối backend thật với pattern này, kẻ tấn công có thể enum email đã đăng ký. | **Thấp** | Pattern tốt hơn cho production: "Nếu email tồn tại, chúng tôi đã gửi link" — không xác nhận email có tồn tại hay không |
| S9 | `TwoFAActivity.java` L78–89 | `onVerify()` chỉ kiểm tra `code.length() < 6` — **không validate nội dung code** với bất kỳ giá trị kỳ vọng nào, không có cơ chế chống brute-force, không rate-limit. Nhập `"000000"` cũng thành công. | **Trung** | Mock behavior — chấp nhận được cho học thuật. Khi nối backend: POST code lên API, xử lý sai/đúng server-side |
| S10 | `TwoFAActivity.java` L40–42 | Sau khi timer hết hạn (`onFinish()`), nút Verify vẫn cho phép submit — **không disable** input hay button khi code expired. | **Thấp** | Khi `onFinish()`: `btn_verify.setEnabled(false)`, xóa OTP boxes |
| S11 | `ApiClient.java` L41–43 | Log level `HttpLoggingInterceptor.Level.BODY` trong debug — log **toàn bộ request/response** bao gồm headers. Nếu tương lai thêm `Authorization: Bearer <token>` header vào OkHttp interceptor, token sẽ xuất hiện trong logcat ở debug. | **Thấp** | Hiện tại chưa có auth header nên không lộ token ngay. Khi nối backend: dùng `Level.HEADERS` thay `Level.BODY` trong debug, hoặc thêm `redactHeader("Authorization")` trước khi log |
| S12 | `ApiClient.java` L37–39 | Khi `USE_MOCK_AI=true` (debug), hàm `build()` return sớm — **OkHttpClient không được tạo**, logger không chạy. Khi `USE_MOCK_AI=false` (release), `log.setLevel(Level.NONE)` ✅. Logging tắt đúng cách ở release. | — | Đạt ✅ |
| S13 | `ApiClient.java` | Không có interceptor nào đính kèm `Authorization` header từ `Session.auth_token` vào request. **Token trong Session không bao giờ được gửi lên server**. | **Trung** | Mock thì OK. Khi nối backend thật: thêm `OkHttpClient.Builder.addInterceptor` để đọc `Session.auth_token(ctx)` và đặt vào header `Authorization: Bearer <token>` |
| S14 | `proguard-rules.pro` L5–6 | `-keep class com.studymentor.app.api.** { *; }` giữ lại **`MockAiService`** trong release APK. Class này không có dữ liệu nhạy cảm nhưng là dead code không cần thiết, và nội dung mock có thể đọc được sau khi decompile APK. | **Thấp** | Dùng build flavor hoặc `BuildConfig.USE_MOCK_AI` với `@SuppressWarnings` để ProGuard shrink `MockAiService` khỏi release. Hoặc chấp nhận — không có dữ liệu nhạy cảm trong mock |
| S15 | `proguard-rules.pro` L12 | `-keep class com.studymentor.app.data.** { *; }` giữ nguyên tên tất cả data class. Không ảnh hưởng bảo mật (Room entity, QuizQuestion POJO) nhưng làm **tắt obfuscation** cho toàn bộ data layer. | **Thấp** | Acceptable — Room và Gson cần tên class/field thật. Không có thông tin nhạy cảm trong data entities |
| S16 | `AndroidManifest.xml` L19 | `android:allowBackup="true"` — nếu S3 chưa được sửa, ADB backup extract được SharedPreferences chứa `auth_token`. | **Trung** | Xem S3. Sau khi sửa backup exclusion, hoặc đổi sang `EncryptedSharedPreferences`, mức độ giảm xuống Thấp |
| S17 | `AndroidManifest.xml` | Không có `android:networkSecurityConfig`. Với `minSdk=33`, Android **tắt cleartext HTTP theo mặc định** ✅ — HTTPS bắt buộc. Không có certificate pinning, nhưng đây là mức enterprise, không cần thiết cho học thuật. | — | Đạt cho học thuật ✅ |
| S18 | Tất cả auth Activity | Không có `Log.d/Log.e/Log.v` nào log email, password hay token trong `LoginActivity`, `SignUpActivity`, `ForgotPasswordActivity`, `TwoFAActivity`, `Session.java` ✅ | — | Đạt ✅ |

---

## Chi tiết phát hiện nghiêm trọng nhất

### S3 — Backup exclusion sai tên file (Cao)

```xml
<!-- backup_rules.xml (hiện tại) -->
<exclude domain="sharedpref" path="auth_token.xml"/>  <!-- ← file này KHÔNG tồn tại -->

<!-- data_extraction_rules.xml (hiện tại) -->
<exclude domain="sharedpref" path="auth_token.xml"/>  <!-- ← tương tự -->
```

`PreferenceManager.getDefaultSharedPreferences(context)` lưu vào file có tên theo **package name**: `com.studymentor.app_preferences.xml`. Không có file nào tên là `auth_token.xml`. Do đó exclusion không có hiệu lực và toàn bộ prefs (auth_token, user_email, streak, quiz score...) **đều được backup** lên Google Cloud Backup.

```xml
<!-- Sửa thành (bỏ phần .xml theo Android convention cho path attribute): -->
<exclude domain="sharedpref" path="com.studymentor.app_preferences"/>
```

### S4 — Device Transfer không có exclusion

```xml
<!-- data_extraction_rules.xml (hiện tại) -->
<device-transfer>
    <include domain="sharedpref" path="."/>
    <!-- không có exclude! -->
</device-transfer>
```

Khi user đổi điện thoại qua Quick Switch Adapter hoặc Smart Switch, `auth_token` được chuyển sang thiết bị mới. Nếu thiết bị cũ bị bán đi sau khi factory reset, session sẽ invalid (vì server giữ trạng thái). Nhưng nếu thiết bị cũ được giữ lại mà không factory reset, **cả hai thiết bị cùng dùng một token**.

---

## Tổng kết theo mức độ

| Mức độ | Số vấn đề | Issues |
|--------|-----------|--------|
| **Cao** | 1 | S3 — Backup exclusion sai tên file |
| **Trung** | 5 | S1 (plaintext token), S4 (device transfer), S9 (OTP no verify), S13 (no auth header), S16 (allowBackup) |
| **Thấp** | 7 | S2, S5, S7, S8, S10, S11, S12, S14, S15 |
| **Đạt** | 5 | S6, S12, S17, S18 + file self-documents known limitations |

**Ưu tiên sửa ngay (trước khi nối backend):**
1. **S3** — sửa tên file exclusion trong `backup_rules.xml` + `data_extraction_rules.xml` (5 phút)
2. **S4** — thêm exclude vào `<device-transfer>` block
3. **S1** — migrate sang `EncryptedSharedPreferences` (1–2 giờ, dependency `androidx.security:security-crypto:1.1.0-alpha06`)
