# AI Core Verification Report
Generated: 2026-06-17

---

## 1. MockAiService.fakeReply() — số lượng case xử lý

**ĐÃ SỬA một phần — không còn đúng 2 case, đã mở rộng thành 3 case reply + 4 case commonMistakes.**

### Reply branch (dòng 65–87)

```java
// MockAiService.java : 65–87
if (prompt.contains("2x") || prompt.contains("x^2") || prompt.contains("quadratic")) {
    // step-by-step math, 4 steps                                           ← case 1 (như cũ)
} else if (prompt.contains("photosynthesis")) {
    // giải thích + phương trình 6CO₂                                      ← case 2 (như cũ)
} else if (prompt.length() < 20) {                                         ← case 3 (MỚI)
    r.reply = "Tell me a bit more — what subject...";
} else {
    r.reply = "Great question! Here's the short version:\n\n"
            + "I'm a mock reply right now. Once you wire the real backend "
            + "(see HANDOFF.md → Section 4) I'll come from the model.\n\n"
            + "Your prompt was: \"" + req.message + "\"";                  ← default (vẫn còn)
}
```

- Câu hỏi < 20 ký tự bây giờ nhận được "Tell me a bit more" thay vì thẳng vào default.
- Default vẫn có chuỗi **"I'm a mock reply right now"** nhưng chỉ khi prompt ≥ 20 ký tự và không khớp case 1/2.

### commonMistakes branch (dòng 94–110) — ĐÃ MỞ RỘNG từ 2 → 4 case

```java
// MockAiService.java : 95–110
if (prompt.contains("2x") || ... || prompt.contains("equation") || prompt.contains("algebra")) {
    // math tips (2 gợi ý)
} else if (prompt.contains("photosynthesis") || prompt.contains("biology")
        || prompt.contains("chemistry") || prompt.contains("physics")) {
    // science tips (2 gợi ý)
} else if (prompt.contains("code") || prompt.contains("program") || prompt.contains("function")
        || prompt.contains("bug") || prompt.contains("algorithm")) {
    // code tips (2 gợi ý)  ← MỚI
} else {
    // generic tips (2 gợi ý) ← MỚI (trước là không có case này)
}
```

Phase 4 đã bổ sung: case math mở rộng thêm `"equation"/"algebra"`, thêm case `"code"` hoàn toàn mới,
và thêm fallback generic thay vì để `commonMistakes` rỗng.

---

## 2. MockOcrService.recognize() — bỏ qua imageUri

**VẪN MOCK — dòng 59–66.**

```java
// MockOcrService.java : 59–66
public static void recognize(final Uri imageUri, final Listener listener) {
    final Random rng = new Random();
    final long delayMs = 900 + rng.nextInt(700);   // 0.9–1.6s

    new Thread(() -> {
        ...
        String text = SAMPLES[rng.nextInt(SAMPLES.length)];  // ← imageUri hoàn toàn bị bỏ qua
```

`imageUri` được nhận vào nhưng không được dùng ở bất kỳ dòng nào trong method.
Kết quả luôn là một trong 6 chuỗi cố định trong mảng `SAMPLES` (dòng 45–52):

```java
// MockOcrService.java : 45–52
private static final String[] SAMPLES = {
    "Giải phương trình bậc hai:\n2x² + 5x − 3 = 0\n...",
    "Find the derivative of:\nf(x) = 3x³ − 2x² + 5x − 7\n...",
    "What is a Python decorator?\nGive an example using @staticmethod.",
    "Solve for x:\n4(x − 3) + 7 = 2x + 11",
    "Explain Newton's third law of motion with two real-world examples.",
    "Tính tích phân:\n∫(2x + 3) dx từ 0 đến 4",
};
```

Confidence score cũng fake: `85 + rng.nextInt(11)` → luôn trong khoảng 85–95 (dòng 77).

---

## 3. build.gradle buildTypes — giá trị USE_MOCK_AI và API_BASE_URL

```groovy
// app/build.gradle : 21–30
debug {
    buildConfigField "boolean", "USE_MOCK_AI", "true"
    buildConfigField "String",  "API_BASE_URL", "\"https://api.studymentor.example.com/\""
}
release {
    minifyEnabled true
    proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
    buildConfigField "boolean", "USE_MOCK_AI", "false"
    buildConfigField "String",  "API_BASE_URL", "\"https://api.studymentor.example.com/\""
}
```

| BuildType | USE_MOCK_AI | API_BASE_URL |
|-----------|-------------|--------------|
| **debug** | `true` — dùng MockAiService | `https://api.studymentor.example.com/` (placeholder) |
| **release** | `false` — gọi Retrofit thật | `https://api.studymentor.example.com/` (placeholder) |

**⚠️ Cảnh báo:** Cả 2 buildType đều trỏ về `api.studymentor.example.com` — một domain placeholder
không tồn tại. Release build có `USE_MOCK_AI=false` nên sẽ khởi tạo Retrofit và gọi thật, nhưng
mọi request sẽ fail (DNS không resolve). Cần cập nhật `API_BASE_URL` thành endpoint thật trước khi
assembleRelease.

---

## 4. ApiClient.java — cơ chế switch Mock ↔ Real

```java
// ApiClient.java : 37–54
private static AiService build() {
    if (BuildConfig.USE_MOCK_AI) {
        return new MockAiService();          // ← debug path
    }
    // real Retrofit path:
    HttpLoggingInterceptor log = new HttpLoggingInterceptor();
    log.setLevel(BuildConfig.DEBUG
            ? HttpLoggingInterceptor.Level.BODY
            : HttpLoggingInterceptor.Level.NONE);
    ...
    return new Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            ...
            .create(AiService.class);
}
```

Singleton với double-checked locking (dòng 26–34). Switch hoạt động đúng:
debug → MockAiService; release → Retrofit với `API_BASE_URL`. Không cần sửa code Java khi deploy,
chỉ cần đổi `API_BASE_URL` trong `build.gradle`.

---

## Tổng kết

| Mục kiểm tra | Trạng thái | Ghi chú |
|-------------|-----------|---------|
| `MockAiService.fakeReply()` chỉ 2 case | **ĐÃ SỬA (mở rộng)** | 3 case reply (thêm short-prompt); 4 case commonMistakes (thêm code + generic) |
| Default vẫn có "I'm a mock reply" | **VẪN CÒN** | dòng 82–86, chỉ trigger khi prompt ≥ 20 ký tự và không khớp case 1/2 |
| `MockOcrService.recognize()` bỏ qua imageUri | **VẪN MOCK** | dòng 66 — random từ SAMPLES[6], imageUri không được dùng |
| `USE_MOCK_AI` debug | `true` | MockAiService |
| `USE_MOCK_AI` release | `false` | Retrofit thật |
| `API_BASE_URL` cả 2 buildType | `https://api.studymentor.example.com/` | **⚠️ Placeholder — cần thay trước khi release** |
