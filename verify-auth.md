# Auth Verification Report
Generated: 2026-06-17

---

## 1. LoginActivity — mock token

**VẪN MOCK** — dòng 66

```java
// LoginActivity.java : 65-66
// TODO: real auth call. Mock for the MVP — any valid email + 8+ char pw works.
Session.saveAuth(this, "mock-token-" + System.currentTimeMillis(), email);
```

Không có Retrofit call nào. Bất kỳ email hợp lệ + password ≥ 8 ký tự đều đăng nhập thành công.

---

## 2. SignUpActivity — mock token

**VẪN MOCK** — dòng 109-110

```java
// SignUpActivity.java : 109-110
// TODO: replace with a real auth call. Mock for the MVP.
Session.saveAuth(this, "mock-token-" + System.currentTimeMillis(), email);
```

Không có Retrofit call nào. Logic giống Login: validate format + terms checkbox, rồi tạo token giả.

---

## 3. ForgotPasswordActivity.mockSendResetLink() — no-op

**VẪN MOCK** — dòng 61-63

```java
// ForgotPasswordActivity.java : 60-63
/** Real implementation would POST to /api/auth/forgot-password. */
private void mockSendResetLink(String email) {
    // no-op for MVP
}
```

Method được gọi tại dòng 56 (`mockSendResetLink(email);`) nhưng không làm gì. Email đặt lại mật khẩu không thực sự được gửi đi.

---

## 4. TwoFAActivity.onVerify() — không so khớp mã thật

**VẪN MOCK** — dòng 78-89

```java
// TwoFAActivity.java : 78-89
private void onVerify() {
    StringBuilder code = new StringBuilder();
    for (int id : OTP_IDS) {
        code.append(((EditText) findViewById(id)).getText().toString().trim());
    }
    if (code.length() < 6) {
        Toast.makeText(this, "Enter all 6 digits", Toast.LENGTH_SHORT).show();
        return;
    }
    // Mock success
    Toast.makeText(this, "2FA enabled successfully!", Toast.LENGTH_LONG).show();
    finish();
}
```

Chỉ kiểm tra `code.length() < 6`. Nhập bất kỳ 6 ký tự nào (kể cả "000000") đều pass. Không có server call, không có mã thật để so khớp.

---

## Tổng kết

| File | Điểm kiểm tra | Trạng thái | Dòng |
|------|--------------|------------|------|
| `LoginActivity.java` | Mock token `"mock-token-" + currentTimeMillis()` | **VẪN MOCK** | 66 |
| `SignUpActivity.java` | Mock token `"mock-token-" + currentTimeMillis()` | **VẪN MOCK** | 110 |
| `ForgotPasswordActivity.java` | `mockSendResetLink()` là no-op | **VẪN MOCK** | 61–63 |
| `TwoFAActivity.java` | `onVerify()` chỉ check length < 6 | **VẪN MOCK** | 78–89 |

**Kết luận:** Toàn bộ 4 điểm đều VẪN MOCK — chưa có điểm nào được wire vào backend thật.
Khi swap MockAiService → real API (xem Next Steps trong CLAUDE.md), 4 điểm này cũng cần được thay thế đồng thời.
