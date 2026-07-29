# MindBridge AI — Project Scope

## 1. Product Overview

MindBridge AI là nền tảng tự hỗ trợ sức khỏe tinh thần dành cho người dùng Việt Nam.

Hệ thống thu thập dữ liệu từ:

- Chat.
- Daily Check-in.
- Quá trình thực hiện bài tập.
- Assessment.
- Feedback của người dùng.

Từ những dữ liệu này, hệ thống:

1. Trích xuất các tín hiệu có cấu trúc.
2. Tổng hợp đặc trưng hành vi theo ngày.
3. Xây dựng hồ sơ hành vi theo thời gian.
4. Đề xuất chương trình tự hỗ trợ CBT bằng Rule Engine.
5. Theo dõi tiến độ và kết quả chương trình.
6. Phát hiện tín hiệu rủi ro và chuyển sang Safety Flow.

MindBridge AI không thay thế chuyên gia tâm lý, bác sĩ hoặc dịch vụ khẩn cấp.

---

## 2. Main Actors

### USER

Người dùng có thể:

- Đăng ký và đăng nhập.
- Quản lý consent.
- Chat với hệ thống.
- Trả lời Daily Check-in.
- Xem dashboard hành vi.
- Nhận đề xuất chương trình tự hỗ trợ.
- Chấp nhận hoặc từ chối chương trình.
- Thực hiện bài tập.
- Nộp bài và đánh giá mức hữu ích.
- Xem tiến độ chương trình.

### EXPERT

Chuyên gia có thể:

- Duyệt nội dung CBT.
- Duyệt Daily Question.
- Duyệt Safety Template.
- Duyệt tài liệu Knowledge Base.
- Review Safety Event được phân công.
- Ghi nhận quyết định review.

Expert không mặc định được xem toàn bộ dữ liệu của mọi user.

### ADMIN

Admin có thể:

- Quản lý tài khoản và role.
- Quản lý trạng thái nội dung.
- Theo dõi audit.
- Theo dõi tình trạng hệ thống.
- Quản lý dữ liệu cấu hình được cho phép.

Admin không được tự đặt clinical threshold hoặc nội dung chuyên môn chưa được phê duyệt.

---

## 3. MVP Scope

## 3.1. Identity and Consent

MVP bao gồm:

- Đăng ký.
- Đăng nhập.
- JWT Authentication.
- Role:
  - USER
  - EXPERT
  - ADMIN
- Cấp consent.
- Thu hồi consent.
- Kiểm tra consent hiện tại.
- Kiểm tra quyền sở hữu dữ liệu.

Các loại consent ban đầu:

- CHAT_ANALYSIS
- PERSONALIZATION
- EXPERT_SHARING

---

## 3.2. Chat

MVP bao gồm:

- Tạo Chat Session.
- Xem danh sách Chat Session.
- Xem chi tiết Chat Session.
- Gửi Conversation Message.
- Xem lịch sử Message.
- Phân trang Message.
- Lưu message gốc tách biệt với kết quả AI.

---

## 3.3. Daily Check-in

MVP bao gồm:

- Daily Question Template.
- Daily Question Option.
- Daily Question Assignment.
- Daily Question Answer.
- Trả lời các chỉ số:
  - Stress.
  - Mood.
  - Sleep.
  - Energy.
- Xem lịch sử check-in.
- Không cho trả lời trùng cùng assignment.

---

## 3.4. Behavioral Event

Hệ thống ghi nhận các hành động quan trọng dưới dạng event.

Event MVP:

- CHAT_SESSION_STARTED
- CHAT_MESSAGE_SENT
- DAILY_CHECKIN_COMPLETED
- DAILY_CHECKIN_SKIPPED
- EXERCISE_ASSIGNED
- EXERCISE_STARTED
- EXERCISE_COMPLETED
- EXERCISE_SKIPPED
- PROGRAM_ACCEPTED
- PROGRAM_PAUSED
- RECOMMENDATION_OPENED
- RECOMMENDATION_HELPFUL

Behavioral Event không thay thế các bảng nghiệp vụ gốc.

---

## 3.5. AI Analysis

MVP sử dụng hosted pretrained LLM API.

Không train hoặc fine-tune model.

AI Analysis thực hiện:

- Topic extraction.
- Emotion extraction.
- Intent extraction.
- Behavioral signal extraction.
- Risk classification sơ bộ.
- Confidence estimation.

Output phải là Structured JSON.

Output tối thiểu:

```json
{
  "topics": [
    "workload",
    "sleep"
  ],
  "emotions": {
    "stress": 0.82,
    "anxiety": 0.64
  },
  "signals": {
    "sleepDifficulty": 0.71,
    "avoidance": 0.44
  },
  "intent": "SEEKING_SUPPORT",
  "riskLevel": 1,
  "confidence": 0.84
}
```

Hệ thống phải có:

- Mock AI Provider.
- Real LLM Provider.
- JSON Schema Validation.
- Timeout.
- Retry giới hạn.
- Lưu provider.
- Lưu model.
- Lưu prompt version.
- Lưu schema version.
- Lưu trạng thái lần chạy.

LLM không đưa ra quyết định cuối cùng về chương trình CBT.

---

## 3.6. Safety

Safety Pipeline:

```text
Message
→ Keyword/Regex Pre-filter
→ LLM Risk Classification
→ Rule Resolver
→ Final Risk Level
```

Risk Level:

- Level 1: Normal.
- Level 2: Needs Follow-up.
- Level 3: High Risk.
- Level 4: Emergency.

Quy tắc MVP:

- Level 1–2 có thể tiếp tục flow thông thường.
- Level 3 tạo Safety Event.
- Level 3 chặn Program Matching tự động.
- Level 4 tạo Safety Event.
- Level 4 chặn Program Matching tự động.
- Level 4 sử dụng fixed approved response.
- Level 4 không dùng LLM để tạo phản hồi khẩn cấp tự do.

Hệ thống không được tự động liên hệ dịch vụ khẩn cấp trong MVP.

---

## 3.7. Behavior Analysis

MVP theo dõi tám feature chính:

- stress_score
- mood_score
- energy_score
- sleep_hours
- anxiety_signal
- engagement_score
- exercise_completion_rate
- max_risk_level

Phân tích tối thiểu:

- Trung bình 7 ngày.
- Trung bình 30 ngày.
- Trend tăng.
- Trend giảm.
- Trend ổn định.
- Số ngày check-in.
- Streak.
- Data coverage.
- Confidence.
- Dominant topics.

Explicit data do user cung cấp có độ ưu tiên cao hơn inferred data từ AI.

Ví dụ:

```text
Daily Check-in stress score
>
AI inferred stress score
```

Kết quả phân tích chỉ thể hiện tín hiệu hoặc xu hướng.

Không sử dụng kết quả để chẩn đoán bệnh.

---

## 3.8. CBT Program

MVP gồm bốn chương trình:

- CBT-STRESS
- CBT-MOOD
- CBT-WORRY
- CBT-SLEEP

Mỗi chương trình demo có thể gồm:

- 3 module.
- 1–2 exercise trong mỗi module.
- Khoảng 5–6 exercise cho mỗi chương trình.

Hệ thống hỗ trợ:

- Program Version.
- Module Version.
- Exercise Version.
- User Program.
- Baseline Assessment.
- Weekly Assessment.
- Final Assessment.
- Exercise Assignment.
- Exercise Submission.
- Module Progress.
- Program State Transition.

Program, Module và Exercise Version là bất biến.

Khi nội dung thay đổi phải tạo version mới.

---

## 3.9. Program Matching

Program Matching sử dụng Rule Engine.

Flow:

```text
Profile Snapshot
→ Safety Gate
→ Eligibility Check
→ Exclusion Check
→ Candidate Scoring
→ Matching Decision
→ User Confirmation
→ User Program
```

Mỗi Matching Run phải lưu:

- Profile Snapshot.
- Rule version.
- Threshold bundle version.
- Tất cả candidate.
- Score từng candidate.
- Eligibility result.
- Exclusion result.
- Reason code.
- Rejection reason.
- Chương trình được chọn.
- Thời điểm quyết định.

LLM không được chọn chương trình cuối cùng.

---

## 3.10. Frontend Integration

Frontend cơ bản hiện đã tồn tại.

Nguyên tắc:

- Giữ giao diện hiện có.
- Không redesign toàn bộ ứng dụng.
- Ưu tiên thay mock data bằng API thật.
- Giữ component đang hoạt động.
- Có loading state.
- Có empty state.
- Có validation state.
- Có error state.
- Có retry khi phù hợp.
- API DTO phải khớp OpenAPI.

---

## Existing Frontend Prototype

Repository hiện có một frontend prototype dùng để:

- Tham khảo giao diện.
- Tham khảo bố cục trang.
- Tham khảo trải nghiệm người dùng dự kiến.
- Tham khảo theme, component và navigation.

Frontend hiện tại:

- Chưa có backend.
- Chưa có API integration.
- Chưa có authentication thật.
- Chưa có authorization thật.
- Chưa có business logic hoàn chỉnh.
- Đang sử dụng mock và placeholder data.
- AI Chat hiện chỉ mô phỏng phản hồi.
- Các số liệu Dashboard, Admin và Expert chỉ là dữ liệu demo.

Frontend prototype không phải là nguồn xác định:

- Database schema.
- API contract.
- Business rules.
- Safety rules.
- CBT workflow.
- Authentication flow.

Khi triển khai chức năng thật, ưu tiên:

1. Task hiện tại.
2. API Contract.
3. Database Design.
4. Safety và CBT Rules.
5. Architecture.
6. Frontend prototype chỉ dùng làm UI reference.

---

## 4. Out of Scope

Không triển khai trong MVP:

- Train AI model.
- Fine-tune AI model.
- Self-host large language model.
- Chẩn đoán bệnh tâm lý.
- Tự động điều trị.
- Tự động thay thế chuyên gia.
- Microservices.
- Kubernetes.
- Kafka bắt buộc.
- Realtime analytics quy mô lớn.
- Mobile app riêng nếu web responsive đã đủ.
- Billing.
- Subscription.
- Social network.
- Community forum.
- Gamification phức tạp.
- Multi-model embedding phức tạp.
- Notification đa kênh.
- Tự động gọi dịch vụ khẩn cấp.
- Tự động tạo clinical threshold.
- Tự động sáng tác nội dung CBT chưa duyệt.

---

## 5. Technology Baseline

- Java 21.
- Spring Boot.
- Maven.
- PostgreSQL.
- Flyway.
- PostgreSQL JSONB.
- pgvector khi triển khai RAG.
- React frontend hiện có.
- REST API.
- JWT.
- Hosted LLM API.
- Git.
- Modular Monolith.

---

## 6. Main Product Flow

```text
Chat / Daily Check-in / Exercise / Feedback
                    ↓
              Raw Data Storage
                    ↓
             AI Structured Analysis
                    ↓
                 Safety Gate
                    ↓
             User Daily Features
                    ↓
             Behavior Profile
                    ↓
             Profile Snapshot
                    ↓
        Rule-based Program Matching
                    ↓
            Versioned CBT Program
                    ↓
          Exercise and Assessment
                    ↓
             Outcome Feedback Loop
```

---

## 7. Source-of-Truth Priority

Khi có tài liệu mâu thuẫn, áp dụng thứ tự:

1. Task file hiện tại trong `docs/tasks/`.
2. `docs/03_API_CONTRACT.yaml`.
3. `docs/02_DATABASE_MVP.md`.
4. Safety và CBT Rules.
5. `docs/01_ARCHITECTURE.md`.
6. `docs/00_PROJECT_SCOPE.md`.
7. SRS đầy đủ.
8. Database Design đầy đủ.
9. Tài liệu cũ chỉ dùng tham khảo.

Cursor phải báo xung đột trước khi thay đổi hành vi.

---

## 8. Definition of Done

Một task chỉ được coi là Done khi:

- Code đã hoàn thành.
- Build thành công.
- Migration chạy được từ database trắng.
- API khớp contract.
- Có validation.
- Có ownership check nếu liên quan user.
- Test liên quan đã được chạy thực tế.
- Không hard-code secret.
- Không log dữ liệu nhạy cảm không cần thiết.
- Frontend đã tích hợp nếu task yêu cầu.
- OpenAPI đã cập nhật nếu contract thay đổi.
- Người còn lại đã review.
- Không còn lỗi BLOCKING.
