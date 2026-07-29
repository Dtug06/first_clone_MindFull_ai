# MindBridge AI — Architecture

## 1. Architecture Style

MindBridge AI sử dụng kiến trúc Modular Monolith.

Lý do:

- Nhóm chỉ có hai developer.
- Dễ phát triển và triển khai.
- Giảm chi phí vận hành.
- Giảm độ phức tạp của distributed system.
- Vẫn giữ được ranh giới domain rõ ràng.
- Có thể tách service trong tương lai nếu cần.

Không triển khai Microservices trong MVP.

---

## 2. Repository Structure

```text
mindbridge-ai/
├── backend/
├── frontend/
├── database/
├── docs/
│   ├── tasks/
│   └── ...
├── seed-data/
└── README.md
```

---

## 3. Backend Module Structure

```text
backend/src/main/java/.../
├── auth/
├── user/
├── consent/
├── chat/
├── checkin/
├── analysis/
├── safety/
├── behavior/
├── cbt/
├── matching/
├── recommendation/
├── knowledge/
├── audit/
└── common/
```

## Module Responsibilities

| Module | Responsibility |
|---|---|
| auth | Login, JWT, authentication filter |
| user | User identity, role, status |
| consent | Consent event và consent hiện tại |
| chat | Chat session và conversation message |
| checkin | Daily question, assignment, answer |
| analysis | LLM provider và structured analysis |
| safety | Risk resolution và safety event |
| behavior | Daily feature, profile, snapshot |
| cbt | CBT catalog và runtime |
| matching | Program candidate và decision |
| recommendation | Recommendation và feedback |
| knowledge | Approved knowledge và RAG |
| audit | Audit các quyết định quan trọng |
| common | Error, validation, time và tiện ích chung |

---

## 4. Layering Convention

Mỗi module có thể sử dụng cấu trúc:

```text
module/
├── controller/
├── dto/
├── service/
├── domain/
├── repository/
├── mapper/
└── exception/
```

Nguyên tắc:

- Controller chỉ xử lý HTTP.
- Controller không chứa business logic.
- Service xử lý business logic.
- Service quản lý transaction.
- Repository chỉ truy cập database.
- Entity không được trả trực tiếp từ REST API.
- Request và Response sử dụng DTO.
- Mapper chỉ chuyển đổi dữ liệu.
- Mapper không chứa business rule.
- Exception sử dụng error code ổn định.

---

## 5. Standard Request Flow

```text
HTTP Request
     ↓
Security Filter
     ↓
Controller
     ↓
Request Validation
     ↓
Application Service
     ↓
Domain Rules
     ↓
Repository / External Provider
     ↓
DTO Mapping
     ↓
HTTP Response
```

---

## 6. Authentication and Ownership

Authentication sử dụng JWT.

Quy tắc:

- User identity lấy từ authenticated principal.
- Không tin tưởng `userId` do frontend gửi lên.
- User-scoped resource phải kiểm tra ownership.
- Role check không thay thế ownership check.
- USER không được đọc dữ liệu của USER khác.
- EXPERT không mặc định được đọc toàn bộ dữ liệu.
- ADMIN action quan trọng phải được audit.

---

## 7. Chat Architecture

```text
Chat Session
     ↓
Conversation Message
     ↓
Behavioral Event
     ↓
Optional AI Analysis
```

Quy tắc:

- Chat Session là entity riêng.
- Conversation Message thuộc một Chat Session.
- Raw message tách khỏi AI result.
- Không dùng message đầu tiên để đại diện session.
- Message pagination phải theo session và thời gian.
- AI failure không được làm mất raw message.

---

## 8. AI Analysis Architecture

```text
Conversation Message
        ↓
Consent Check
        ↓
Create AI Analysis Run
        ↓
Keyword / Regex Pre-filter
        ↓
ChatAnalysisProvider
  ├── MockChatAnalysisProvider
  └── RealLlmChatAnalysisProvider
        ↓
Structured Output Validation
        ↓
Save Chat Analysis Result
        ↓
Safety Resolver
        ↓
Final Risk State
```

Interface đề xuất:

```java
public interface ChatAnalysisProvider {

    ChatAnalysisOutput analyze(ChatAnalysisInput input);
}
```

Quy tắc:

- Có Mock Provider cho local và test.
- Có Real Provider cho môi trường được cấu hình.
- External call có timeout.
- Retry có giới hạn.
- JSON phải được validate.
- JSON sai schema không được lưu là thành công.
- Không overwrite kết quả phân tích cũ.
- Khi reprocess, tạo run mới.
- Lưu model, prompt và schema version.
- LLM không chọn chương trình CBT cuối cùng.

---

## 9. Safety Architecture

```text
Raw Message
    ↓
Keyword / Regex Signal
    ↓
LLM Risk Signal
    ↓
Safety Rule Resolver
    ↓
Final Risk Level
    ↓
Safety Action
```

Quy tắc:

- Safety tách khỏi response generation.
- Level 3–4 chặn automated Program Matching.
- Level 4 dùng fixed approved response.
- Safety Event phải lưu source.
- Không tự nghĩ crisis wording.
- Không hard-code threshold chưa được phê duyệt.
- Không cho LLM tự quyết định toàn bộ Safety Flow.

---

## 10. Behavior Analysis Architecture

```text
Daily Answers
+ Chat Analysis
+ Behavioral Events
+ Exercise Activity
        ↓
Daily Feature Aggregation
        ↓
User Daily Features
        ↓
7-day and 30-day Aggregation
        ↓
Current Behavior Profile
        ↓
Behavior Profile Snapshot
```

Phép tính MVP:

- Average.
- Count.
- Completion rate.
- Streak.
- Trend.
- Coverage.
- Confidence.
- Dominant topic frequency.

Quy tắc:

- Explicit data ưu tiên hơn inferred data.
- Mỗi feature có calculation version.
- Profile có profile version.
- Snapshot dùng trong matching phải bất biến.
- Không kết luận chẩn đoán.
- Không thêm ML model khi chưa có task riêng.

---

## 11. CBT Architecture

```text
Intervention Program
        ↓
Program Version
        ↓
Module Version
        ↓
Exercise Version
        ↓
User Program
        ↓
Module Progress
        ↓
Exercise Assignment
        ↓
Exercise Submission
```

Version Rule:

- Program Version bất biến.
- Module Version bất biến.
- Exercise Version bất biến.
- User Program trỏ vào chính xác Program Version.
- Assignment trỏ vào chính xác Exercise Version.
- Khi nội dung đổi phải tạo version mới.

---

## 12. CBT State Machine

```text
PROPOSED
→ ACCEPTED
→ BASELINE
→ ACTIVE
→ FINAL_ASSESSMENT
→ COMPLETED
→ MAINTENANCE
```

Các nhánh khác:

```text
ACTIVE → PAUSED
PAUSED → ACTIVE
ACTIVE → ESCALATED
PROPOSED → WITHDRAWN
ACCEPTED → WITHDRAWN
ACTIVE → WITHDRAWN
```

Mỗi transition phải được kiểm tra và ghi vào:

```text
program_state_transitions
```

Không sử dụng `program_reviews` thay thế lịch sử state transition.

---

## 13. Program Matching Architecture

```text
Profile Snapshot
        ↓
Safety Gate
        ↓
Eligibility Rules
        ↓
Exclusion Rules
        ↓
Candidate Scoring
        ↓
Matching Decision
        ↓
User Confirmation
        ↓
User Program
```

Mỗi Matching Run phải lưu:

- User.
- Profile Snapshot.
- Trigger.
- Rule version.
- Threshold bundle version.
- Candidate.
- Eligibility result.
- Exclusion result.
- Score component.
- Final score.
- Reason code.
- Rejection reason.
- Decision.

Matching phải deterministic trong MVP.

---

## 14. Frontend Architecture

Frontend hiện tại là baseline.

Nguyên tắc:

- Không redesign khi task chỉ yêu cầu integration.
- Giữ route và component đang hoạt động.
- Dùng API client chung.
- Dùng DTO TypeScript.
- DTO phải khớp OpenAPI.
- Có auth interceptor.
- Không hard-code backend URL trong component.
- Mock data phải dễ thay thế.
- Không trộn mock và real data âm thầm.

Mỗi trang dùng dữ liệu phải có:

- Loading state.
- Empty state.
- Error state.
- Validation state.
- Disabled state khi submit.
- Retry khi phù hợp.

---

## 15. Database and Flyway

Quy tắc:

- PostgreSQL là source of truth.
- Mọi schema change đi qua Flyway.
- Không sửa migration đã merge hoặc apply.
- Không tạo toàn bộ database trong một migration.
- Timestamp lưu UTC bằng `timestamptz`.
- Primary key mặc định UUID.
- JSONB dành cho dữ liệu linh hoạt.
- Metric thường xuyên query phải là typed column.
- Foreign key và unique constraint phải rõ ràng.
- Index chỉ tạo cho query thực tế.

---

## 16. Transaction Boundaries

Một transaction bao phủ một hành động local hoàn chỉnh.

Ví dụ:

- Tạo message và behavioral event.
- Lưu answer và check-in event.
- Lưu analysis result và risk state.
- Chấp nhận program và tạo state transition.
- Nộp exercise và cập nhật progress.

Không giữ database transaction trong lúc chờ external LLM call lâu.

Flow phù hợp:

```text
Create AI Run PENDING
→ Commit
→ Call External Provider
→ Validate
→ Save SUCCEEDED or FAILED
```

---

## 17. Testing Strategy

### Unit Test

Dùng cho:

- Feature calculation.
- Trend calculation.
- Safety rule.
- Program state transition.
- Matching score.
- Eligibility.
- Exclusion.

### Integration Test

Dùng cho:

- Repository.
- Database constraint.
- REST API.
- Authentication.
- Ownership.
- Flyway migration.
- Transaction.

### End-to-End Test

Flow chính:

```text
Register
→ Login
→ Consent
→ Chat
→ Analysis
→ Daily Features
→ Profile
→ Matching
→ CBT
→ Exercise Submission
```

### AI Test

- Automated test dùng Mock Provider.
- Real Provider test chỉ chạy khi có environment flag.
- Không phụ thuộc API thật trong CI thông thường.

---

## 18. Environment Profiles

Các profile:

- local
- test
- staging
- production

### local

- Database local.
- Mock hoặc Real LLM tùy cấu hình.
- Debug vừa đủ.

### test

- Database test riêng.
- Mock LLM.
- Seed cố định.
- Kết quả deterministic.

### staging

- Gần production.
- Dùng để integration và demo.

### production

- Không debug secret.
- Không log sensitive content.
- Cấu hình nghiêm ngặt hơn.

---

## 19. Architecture Constraints

Không triển khai trong MVP:

- Microservices.
- Event Sourcing toàn hệ thống.
- Kafka bắt buộc.
- Kubernetes.
- CQRS phức tạp.
- Generic framework tự xây.
- Reflection-heavy abstraction.
- AI-generated business rules.
- AI-generated clinical thresholds.