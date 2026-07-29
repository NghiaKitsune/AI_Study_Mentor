# Kế hoạch bổ sung kiến trúc hội thoại nhiều lượt cho Milo

> Trạng thái: Bản phác thảo để review, chưa triển khai source code  
> Ngày lập: 2026-07-30  
> Nhánh dự kiến triển khai: `refactor/final-refactor`  
> AI provider bắt buộc: Gemini, không sử dụng Groq

## 1. Mục tiêu

Chuyển Chat của Milo từ mô hình “mỗi câu hỏi là một đoạn chat độc lập” sang hội thoại nhiều lượt có ngữ cảnh, nhưng vẫn giữ mỗi câu hỏi là một đơn vị học tập riêng để không phá vỡ:

- Answer Detail và nút `View details` theo từng câu trả lời.
- Practice lấy câu hỏi từ lịch sử đã hoàn thành.
- Bộ đếm giờ của Practice.
- Câu hỏi trắc nghiệm, nhập đáp án ngắn và điền khuyết.
- Cơ chế chấm điểm, XP, achievement và chống cộng XP trùng.
- Cache câu trả lời, offline queue và WorkManager retry.
- Dữ liệu Room hiện có của người dùng.

## 2. Hiện trạng đã khảo sát

Database hiện tại là Room version 2 và đã có migration tường minh `MIGRATION_1_2`.

Luồng Chat hiện tại:

1. Mỗi lần gửi, `ChatRepository.send()` tạo một `Question` mới.
2. Tin nhắn user và assistant được liên kết bằng `messages.question_id`.
3. `MessageDao.forQuestion()` chỉ tải tin nhắn của một `questionId`.
4. `ChatViewModel` chuyển sang `questionId` vừa tạo và reload riêng câu hỏi đó.
5. `ChatRepository.buildRequest()` chỉ đọc lịch sử của chính câu hỏi hiện tại nên Gemini gần như không có ngữ cảnh từ câu trước.

Hệ quả: giao diện giống chatbot nhưng dữ liệu thực tế là nhiều cặp hỏi–đáp độc lập.

## 3. Kiến trúc mục tiêu

```mermaid
erDiagram
    USER ||--o{ CONVERSATION : owns
    CONVERSATION ||--o{ QUESTION : groups
    QUESTION ||--o{ MESSAGE : contains
    QUESTION ||--o| ANSWER_DETAIL : has
    QUESTION ||--o{ QUIZ_QUESTION : sources
    QUESTION ||--o{ XP_EVENT : awards

    CONVERSATION {
        long id PK
        long user_id FK
        string title
        long created_at
        long updated_at
        boolean archived
    }

    QUESTION {
        long id PK
        long user_id FK
        long conversation_id FK_nullable
        string prompt
        string answer
        string status
    }

    MESSAGE {
        long id PK
        long question_id FK
        string role
        string text
        long sent_at
    }
```

Quyết định thiết kế quan trọng:

- `Conversation` chỉ nhóm các câu hỏi thành một phiên hội thoại.
- `Question` vẫn là đơn vị dùng cho Practice, XP, bookmark, cache và Answer Detail.
- `Message` tiếp tục liên kết với `Question`; không lưu thêm `conversation_id` để tránh dữ liệu quan hệ bị lặp.
- Tin nhắn của một conversation được tải bằng phép JOIN `messages → questions → conversations`.
- Nút `View details` trên từng assistant message vẫn truyền đúng `questionId`.

## 4. Thiết kế dữ liệu

### 4.1 Entity mới: `Conversation`

Các trường dự kiến:

| Trường | Kiểu | Quy tắc |
|---|---|---|
| `id` | `long` | Primary key, auto-generate |
| `user_id` | `long` | Foreign key tới `users.id`, cascade theo user |
| `title` | `String` | Lấy từ câu hỏi đầu tiên, giới hạn khoảng 60 ký tự |
| `created_at` | `long` | Thời điểm tạo |
| `updated_at` | `long` | Cập nhật sau mỗi lần gửi hoặc nhận câu trả lời |
| `archived` | `boolean` | Ẩn conversation mà không xóa dữ liệu học tập |

Index cần có:

- `index_conversations_user_id`
- `index_conversations_updated_at`
- `index_conversations_user_id_archived_updated_at`

### 4.2 Mở rộng `Question`

Thêm trường nullable:

```text
conversation_id INTEGER NULL
```

Lý do tạm để nullable:

- Cho phép migration dữ liệu version 2 an toàn.
- Không chặn các bản ghi cũ hoặc tác vụ WorkManager đang chờ.
- Source mới phải luôn gán `conversationId` cho câu hỏi được tạo từ Chat.

Quan hệ đề xuất là `ON DELETE SET NULL`, không cascade xóa `Question`. Xóa hoặc archive một conversation không được vô tình xóa nguồn Practice, XP và lịch sử câu hỏi.

### 4.3 DAO mới và query mới

`ConversationDao` dự kiến có:

- `insert(Conversation)`
- `byId(conversationId, userId)`
- `recentForUser(userId)`
- `touch(conversationId, userId, updatedAt)`
- `rename(conversationId, userId, title)`
- `archive(conversationId, userId)`

`MessageDao` bổ sung:

- `forConversation(conversationId, userId)` để render toàn bộ hội thoại.
- `historyBeforeQuestion(conversationId, questionCreatedAt, userId, limit)` để dựng context Gemini.

Mọi query phải JOIN qua `questions.user_id` hoặc `conversations.user_id` để không rò dữ liệu giữa các tài khoản local.

## 5. Migration Room 2 → 3

Tuyệt đối không dùng `fallbackToDestructiveMigration()`.

Các bước trong `MIGRATION_2_3`:

1. Tạo bảng `conversations` và các index.
2. Thêm cột nullable `conversation_id` vào `questions`.
3. Tạo một conversation tương ứng cho mỗi `Question` cũ.
4. Dùng `Question.id` làm `Conversation.id` trong migration để ánh xạ ổn định, không cần bảng tạm.
5. Gán `questions.conversation_id = questions.id` cho dữ liệu cũ.
6. Tạo index cho `questions.conversation_id`.
7. Không sửa hoặc xóa `messages`, `answer_details`, `quiz_questions`, `xp_events` và `quiz_attempts`.
8. Bump `AppDatabase` từ version 2 lên version 3.
9. Đăng ký cả `MIGRATION_1_2` và `MIGRATION_2_3` trong `StudyMentorApp`.
10. Export schema version 3 vào `app/schemas/`.

Sau migration, mỗi câu hỏi cũ vẫn xuất hiện như một conversation riêng. Người dùng không mất lịch sử và có thể tiếp tục hỏi trong conversation đó.

Kiểm tra bắt buộc sau migration:

- Số lượng `questions`, `messages`, `answer_details`, `xp_events` không giảm.
- Mọi question cũ có `conversation_id` hợp lệ.
- Answer Detail, bookmark, Practice và XP cũ vẫn mở được.
- Foreign key check không trả lỗi.

## 6. Thay đổi Repository và ViewModel

### 6.1 `ChatRepository`

API mới dự kiến:

```text
createConversation(userId, firstPrompt)
loadConversation(conversationId, userId)
send(userId, conversationId, prompt, subject, callback)
retry(questionId, userId, callback)
```

Luồng gửi mới:

1. Nếu chưa có `conversationId`, tạo conversation trong transaction.
2. Tạo `Question` mới và gán `conversationId`.
3. Insert user `Message` gắn với `questionId`.
4. Render ngay user message trong conversation hiện tại.
5. Dựng request Gemini từ tối đa 8 tin nhắn trước câu hỏi hiện tại.
6. Không đưa chính prompt hiện tại vào `history`, vì prompt đã nằm trong `request.message`.
7. Khi Gemini hoàn thành, insert assistant `Message`, `AnswerDetail`, XP và cập nhật `Conversation.updatedAt` trong transaction.
8. Nếu dùng cache, vẫn tạo assistant message và Answer Detail cho question mới trong conversation.

### 6.2 Quy tắc context Gemini

- Chỉ gửi tối đa 8 tin nhắn gần nhất trước câu hỏi hiện tại.
- Giới hạn thêm tổng số ký tự context, dự kiến 12.000–16.000 ký tự.
- Giữ đúng thứ tự thời gian.
- Chỉ lấy context trong cùng conversation và cùng user.
- Không gửi câu hỏi hoặc câu trả lời của conversation khác.
- Không gửi tin nhắn được tạo sau câu hỏi đang retry.
- Gemini vẫn là provider duy nhất.

### 6.3 Offline và WorkManager

- Mỗi `Question` vẫn có status `PENDING/PROCESSING/COMPLETED/FAILED/CANCELLED`.
- Worker tiếp tục retry theo `questionId`.
- Khi dựng context cho câu hỏi offline, chỉ lấy message có thời gian trước `question.createdAt`.
- Không để câu hỏi gửi sau chen vào context của câu hỏi đang retry.
- Ưu tiên xử lý các câu pending trong cùng conversation theo `createdAt ASC`.

### 6.4 `ChatViewModel`

State mới cần giữ đồng thời:

- `conversationId`
- `activeQuestionId`
- Toàn bộ `messages` của conversation
- Trạng thái gửi hiện tại
- Error/retry của câu hỏi đang active

Khi gửi câu tiếp theo, không xóa adapter và không thay toàn bộ lịch sử bằng cặp hỏi–đáp mới.

## 7. Thay đổi giao diện

### 7.1 `ChatActivity`

- Thêm `EXTRA_CONVERSATION_ID`.
- Không sử dụng `EXTRA_QUESTION_ID` làm định danh chính của toàn màn hình Chat.
- Khi mở từ một câu hỏi cũ, repository tìm conversation chứa question đó rồi tải toàn bộ thread.
- Giữ danh sách tin nhắn khi gửi câu mới.
- Auto-scroll tới tin nhắn mới nhất.
- Disable Send trong lúc câu hiện tại `PROCESSING` để tránh gửi chồng không kiểm soát.
- Với `PENDING` offline, hiển thị trạng thái rõ ràng và cho phép retry.

### 7.2 Nút `View details`

- Giữ vị trí ngay dưới từng bong bóng assistant.
- Mỗi nút lấy `message.questionId`, không lấy một `questionId` toàn cục từ Activity.
- Chỉ hiển thị khi question tương ứng đã `COMPLETED` và có Answer Detail.
- Nhấn nút mở `AnswerActivity.EXTRA_QUESTION_ID`.
- Không hiển thị trên user message, loading hoặc error placeholder.

### 7.3 Quản lý conversation

MVP:

- Nút `New chat` trong toolbar overflow.
- History vẫn có thể hiển thị theo từng question để không phá luồng hiện tại.

Giai đoạn hoàn thiện:

- Danh sách conversation gần đây.
- Tiêu đề lấy từ prompt đầu tiên và cho phép đổi tên.
- Archive thay vì hard delete.
- Khi mở một question từ History, đưa người dùng tới đúng vị trí trong conversation.

## 8. Các cơ chế bắt buộc giữ nguyên

| Cơ chế | Quyết định |
|---|---|
| Gemini | Giữ nguyên, không thay bằng Groq |
| Practice timer | Không thay đổi cách tính deadline hoặc thời gian còn lại |
| Hiện đáp án sau khi chọn | Giữ nguyên |
| MULTIPLE_CHOICE | Giữ nguyên |
| SHORT_ANSWER | Giữ nguyên |
| FILL_BLANK | Giữ nguyên |
| Quiz score | Vẫn tính từ `QuizAnswer`/`QuizAttempt` |
| XP câu hỏi | Vẫn award một lần theo `questionId` |
| XP quiz | Không thay đổi |
| Answer Detail | Vẫn định danh bằng `questionId` |
| Cache | Vẫn cache theo normalized prompt và preference |
| Offline queue | Vẫn retry bằng WorkManager |
| User isolation | Mọi query bắt buộc lọc `userId` |

Không chuyển XP, Practice hoặc Answer Detail sang định danh bằng `conversationId`.

## 9. Kế hoạch triển khai theo phase

### Phase 0 — Checkpoint và baseline (0,5–1 giờ)

- Commit/checkpoint toàn bộ bản đang hoạt động trên `refactor/final-refactor`.
- Lưu snapshot database version 2 từ emulator.
- Chạy `testDebugUnitTest` và `assembleDebug` làm baseline.
- Chụp ảnh Chat, View Details, Practice và Profile XP trước migration.

Điều kiện hoàn thành: baseline xanh và có dữ liệu để đối chiếu sau migration.

### Phase 1 — Data model và migration 2→3 (2–3 giờ)

- Thêm `Conversation`, `ConversationDao`.
- Thêm `conversationId` vào `Question`.
- Bump database version.
- Viết `MIGRATION_2_3` và migration instrumentation test.
- Export schema version 3.

Điều kiện hoàn thành: dữ liệu version 2 được nâng cấp mà không mất bản ghi.

### Phase 2 — DAO, Repository và Gemini context (2,5–4 giờ)

- Thêm query hội thoại.
- Refactor `ChatRepository` theo `conversationId`.
- Dựng history tối đa 8 message và giới hạn ký tự.
- Giữ cache/offline/retry/XP transaction hiện tại.
- Bổ sung unit test cho context ordering và user isolation.

Điều kiện hoàn thành: câu hỏi follow-up gửi được ngữ cảnh đúng cho Gemini.

### Phase 3 — ViewModel và Chat UI (2–3 giờ)

- Refactor `ChatViewModel` để quản lý conversation.
- Chat không mất các tin nhắn trước khi gửi câu mới.
- Giữ `View details` cho từng assistant message.
- Thêm `New chat`.
- Kiểm tra loading, error, pending và cached answer.

Điều kiện hoàn thành: người dùng chat nhiều lượt liên tục trong cùng màn hình.

### Phase 4 — Tích hợp History và điều hướng (1–2 giờ)

- Mở question cũ trong đúng conversation.
- Giữ trang History theo question ở MVP.
- Đảm bảo back stack không tạo vòng lặp.
- Đảm bảo follow-up từ AnswerActivity quay lại đúng conversation hoặc tạo conversation mới theo quy tắc đã chọn.

### Phase 5 — Regression và emulator test (2–4 giờ)

- Chạy toàn bộ unit/instrumentation test.
- Build và cài đè để kích hoạt migration thật.
- Test Gemini nhiều lượt.
- Test View Details cho nhiều assistant message.
- Test Practice, timer, đáp án, score và XP.
- Test offline queue, retry và app restart.
- Kiểm tra logcat không có crash/Room exception.

Tổng thời gian dự kiến:

- MVP: 6–8 giờ.
- Hoàn chỉnh và kiểm thử an toàn: 10–16 giờ, tương đương khoảng 1–2 ngày làm việc.

## 10. Test case chấp nhận

### Migration

- Cài APK version 2 có dữ liệu, sau đó cài đè version 3.
- Không mất question, message, Answer Detail, bookmark, XP hoặc quiz history.
- Mỗi question cũ mở được và có conversation hợp lệ.

### Multi-turn Gemini

1. Hỏi: “What is photosynthesis?”
2. Hỏi tiếp: “Where does it happen?”
3. Gemini phải hiểu “it” là photosynthesis mà không yêu cầu người dùng nhắc lại.
4. Đóng và mở lại app, cả hai lượt vẫn còn.

### View Details

- Hai assistant message có hai nút `View details`.
- Mỗi nút mở đúng prompt, answer, steps và detail của question tương ứng.
- Không mở nhầm detail của câu trả lời mới nhất.

### Practice và XP

- Mỗi question hoàn thành vẫn có thể trở thành nguồn Practice.
- Timer giữ đúng sau rotate/background/restore.
- Đáp án được reveal đúng.
- Score được lưu đúng.
- Một question không nhận XP hai lần khi retry hoặc mở lại conversation.

### Offline

- Gửi câu hỏi khi mất mạng tạo `PENDING`.
- Khởi động lại app không mất conversation.
- Khi có mạng, WorkManager hoàn thành đúng question và thêm assistant message vào đúng vị trí.

### Bảo mật dữ liệu local

- User A không đọc được conversation của User B.
- Intent chứa `conversationId` không vượt qua kiểm tra `userId` trong repository/DAO.

## 11. Rủi ro và biện pháp kiểm soát

| Rủi ro | Mức độ | Kiểm soát |
|---|---:|---|
| Migration làm mất dữ liệu | Cao | Migration test, DB snapshot, không destructive migration |
| Context Gemini quá dài | Trung bình | Giới hạn 8 messages và giới hạn ký tự |
| Retry lấy nhầm message tương lai | Cao | Query history trước `question.createdAt` |
| XP bị cộng trùng | Trung bình | Giữ unique source key theo `questionId` |
| View Details mở nhầm câu | Trung bình | Listener luôn nhận `message.questionId` |
| Chat và History tạo back-stack loop | Trung bình | Kiểm tra flags và test điều hướng |
| Conversation xóa làm mất nguồn Practice | Cao | Archive/SET NULL, không cascade xóa Question |
| Gửi nhiều câu đồng thời làm sai thứ tự | Trung bình | Disable Send khi processing, xử lý pending theo thời gian |

## 12. Danh sách file dự kiến tác động

File mới:

- `app/src/main/java/com/studymentor/app/data/Conversation.java`
- `app/src/main/java/com/studymentor/app/data/ConversationDao.java`
- Unit/instrumentation tests cho migration và conversation repository.

File chỉnh sửa chính:

- `AppDatabase.java`
- `DatabaseMigrations.java`
- `StudyMentorApp.java`
- `Question.java`
- `QuestionDao.java`
- `MessageDao.java`
- `ChatRepository.java`
- `ChatViewModel.java`
- `ChatActivity.java`
- `MessageAdapter.java`
- `HistoryActivity.java` hoặc `HistoryViewModel.java`
- Các string/layout liên quan tới New Chat và trạng thái conversation.

Không dự kiến sửa logic timer, evaluator hoặc score của Practice trừ khi regression test phát hiện lỗi tích hợp.

## 13. Tiêu chí hoàn tất toàn bộ liệu trình

Chỉ đánh dấu hoàn tất khi đáp ứng đồng thời:

- Migration 2→3 pass và dữ liệu cũ được bảo toàn.
- Chat nhiều lượt hoạt động sau app restart.
- Gemini nhận đúng context của cùng conversation.
- `View details` mở đúng từng question.
- Practice timer, reveal answer, question types, score và XP đều pass regression.
- Offline queue và retry vẫn hoạt động.
- Toàn bộ unit/instrumentation test pass.
- `assembleDebug` pass, APK cài đè thành công.
- Logcat không có crash, Room main-thread access hoặc migration exception.
- Nhánh `main` không bị chỉnh sửa.

## 14. Quyết định cần review trước khi triển khai

- Có cần màn hình danh sách conversation ngay trong MVP hay để phase sau?
- Khi nhấn follow-up trong AnswerActivity: tiếp tục conversation cũ hay tạo chat mới?
- Archive conversation có ẩn các question của nó khỏi History hay History vẫn hiển thị theo question?
- Giới hạn context nên là 8 hay 12 messages?
- Có cho phép gửi thêm khi một câu đang `PENDING` offline hay buộc chờ hoàn thành?

Khuyến nghị mặc định:

- MVP chưa cần màn hình conversation list riêng.
- Follow-up tiếp tục conversation cũ nếu được mở từ Chat; nếu mở từ History thì mở đúng conversation chứa question.
- Archive chỉ ẩn conversation khỏi danh sách conversation, không ẩn question khỏi History/Practice.
- Context tối đa 8 messages.
- Chỉ cho một câu `PROCESSING` tại một thời điểm trong mỗi conversation.
