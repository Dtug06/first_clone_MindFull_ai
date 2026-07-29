# MindBridge AI — Safety and CBT Rules

## 1. Purpose

File này định nghĩa các quy tắc nghiệp vụ bắt buộc đối với:

- AI Safety.
- Risk Classification.
- Safety Event.
- Program Matching Safety Gate.
- CBT Content Versioning.
- CBT Program State Machine.
- Exercise và Assessment.
- Expert Review.

Cursor và developer không được tự suy luận, tự đặt threshold hoặc tự thay đổi các quy tắc trong file này.

Khi một giá trị chưa được xác nhận bởi chuyên gia, sử dụng:

```text
TODO_EXPERT_REVIEW
CONFIG_PLACEHOLDER
DEMO_ONLY
```

Không tự thay bằng một giá trị có vẻ hợp lý.

---

# 2. General Safety Principles

MindBridge AI:

- Không phải công cụ chẩn đoán.
- Không thay thế bác sĩ hoặc chuyên gia tâm lý.
- Không tự động điều trị.
- Không tự động liên hệ dịch vụ khẩn cấp trong MVP.
- Không được khẳng định chắc chắn tình trạng tâm lý của người dùng.
- Chỉ mô tả tín hiệu, xu hướng hoặc mức rủi ro do hệ thống phát hiện.
- Không được để LLM tự quyết định toàn bộ Safety Flow.
- Mọi quyết định Safety quan trọng phải có khả năng audit.

---

# 3. Risk Levels

## 3.1. Risk Level 1 — Normal

Ý nghĩa:

- Không phát hiện tín hiệu rủi ro đáng kể.
- Người dùng có thể tiếp tục flow thông thường.

Hành động cho phép:

- Tiếp tục chat.
- Tiếp tục Daily Check-in.
- Tạo Behavior Feature.
- Cập nhật Behavior Profile.
- Chạy Program Matching nếu đủ dữ liệu.
- Tiếp tục chương trình CBT đang hoạt động.

Hành động bắt buộc:

- Lưu Risk State nếu Safety Pipeline đã được chạy.
- Lưu confidence và source.

---

## 3.2. Risk Level 2 — Needs Follow-up

Ý nghĩa:

- Có tín hiệu cần tiếp tục theo dõi.
- Chưa đủ điều kiện để kết luận High Risk.

Hành động cho phép:

- Tiếp tục chat.
- Hỏi thêm câu hỏi làm rõ đã được duyệt.
- Khuyến nghị Daily Check-in.
- Tiếp tục Program Matching nếu không có rule chặn khác.
- Tiếp tục CBT nếu không có dấu hiệu xấu đi.

Hành động bắt buộc:

- Ghi Risk State History.
- Lưu reason code.
- Theo dõi trend trong các ngày tiếp theo.
- Không được nâng hoặc hạ risk chỉ dựa trên một từ khóa đơn lẻ.

---

## 3.3. Risk Level 3 — High Risk

Ý nghĩa:

- Có tín hiệu rủi ro cao.
- Cần chuyển sang Safety Flow.
- Không được tự động bắt đầu liệu trình mới.

Hành động bắt buộc:

- Tạo `safety_events`.
- Tạo `safety_event_sources`.
- Ghi `risk_state_history`.
- Chặn automated Program Matching.
- Không tự động tạo `user_programs`.
- Đánh dấu chương trình đang hoạt động để review nếu cần.
- Hiển thị nội dung hỗ trợ đã được duyệt.
- Cho phép tạo Expert Review khi chức năng Expert được triển khai.

Hành động bị cấm:

- Không để LLM tự do tạo hướng dẫn khẩn cấp.
- Không tự động kết luận bệnh.
- Không tự động chuyển user sang một chương trình CBT khác.
- Không giảm Risk Level nếu chưa có dữ liệu hoặc rule hợp lệ.
- Không bỏ qua Safety Event vì AI confidence thấp mà không có fallback.

---

## 3.4. Risk Level 4 — Emergency

Ý nghĩa:

- Có tín hiệu nguy cơ khẩn cấp theo rule đã được phê duyệt.
- Flow thông thường phải dừng tại Safety Gate.

Hành động bắt buộc:

- Tạo `safety_events`.
- Tạo `safety_event_sources`.
- Ghi `risk_state_history`.
- Chặn automated Program Matching.
- Chặn tự động bắt đầu CBT.
- Không sử dụng free-form LLM response.
- Sử dụng fixed approved Safety Response.
- Ghi audit cho quyết định Level 4.
- Hiển thị thông tin hỗ trợ theo cấu hình đã được phê duyệt.

Hành động bị cấm:

- Không để LLM tự sáng tác crisis advice.
- Không tạo nội dung gây hoảng sợ.
- Không khẳng định người dùng chắc chắn sẽ gây hại.
- Không tự động gọi cơ quan bên ngoài trong MVP.
- Không tự động chia sẻ dữ liệu cho Expert khi chưa có quyền phù hợp.
- Không tự động đóng Safety Event.

---

# 4. Safety Input Sources

Safety Event có thể bắt nguồn từ:

```text
CHAT_ANALYSIS
DAILY_ANSWER
EXERCISE_SUBMISSION
PROGRAM_ASSESSMENT
PROGRAM_REVIEW
MANUAL_EXPERT_REVIEW
```

Mỗi Safety Event phải có ít nhất một source.

Không được chỉ lưu:

```text
risk_level = 4
```

mà không lưu nguồn và lý do.

---

# 5. Safety Decision Components

Final Risk Level có thể được tổng hợp từ:

```text
Keyword/Regex Signal
+ LLM Risk Signal
+ Deterministic Safety Rule
+ Current Risk State
+ Recent Risk History
```

Phải phân biệt:

- `model_risk_level`
- `rule_risk_level`
- `final_risk_level`

Không được ghi đè ba giá trị thành một giá trị duy nhất nếu làm mất khả năng audit.

---

# 6. Keyword and Regex Pre-filter

Keyword hoặc Regex chỉ dùng để:

- Phát hiện tín hiệu ban đầu.
- Kích hoạt Safety Classification.
- Tăng mức ưu tiên xử lý.
- Tạo fallback khi LLM không hoạt động.

Keyword không được là cơ sở duy nhất để kết luận Level 4 nếu chưa có rule được phê duyệt.

Danh sách keyword thực tế:

```text
TODO_EXPERT_REVIEW
```

Cursor không được tự tạo danh sách keyword production.

---

# 7. LLM Safety Classification

LLM Safety Output phải là Structured JSON.

Ví dụ schema:

```json
{
  "riskLevel": 2,
  "confidence": 0.84,
  "reasonCodes": [
    "DISTRESS_SIGNAL",
    "SLEEP_DISRUPTION"
  ],
  "evidenceSpans": [
    {
      "start": 10,
      "end": 35,
      "textHash": "hash"
    }
  ]
}
```

Quy tắc:

- Output phải được validate.
- JSON sai schema không được coi là thành công.
- Confidence phải nằm trong khoảng `0..1`.
- Evidence phải gắn với source.
- Không lưu raw content không cần thiết trong log.
- Reprocess phải tạo AI Analysis Run mới.
- LLM không được tự quyết định action cuối cùng.

---

# 8. Safety Event Status

Status đề xuất:

```text
OPEN
UNDER_REVIEW
RESOLVED
DISMISSED
```

Quy tắc:

- Event mới có trạng thái `OPEN`.
- Chỉ role hoặc service được phép mới có thể chuyển status.
- Mỗi lần thay đổi status phải được audit.
- `RESOLVED` không có nghĩa user đã khỏi hoặc không còn rủi ro.
- `DISMISSED` phải có reason code.
- Không xóa Safety Event chỉ vì đã xử lý xong.

---

# 9. Expert Review Rules

Expert Review chỉ được thực hiện bởi user có role phù hợp.

Mỗi review cần lưu:

- Safety Event.
- Reviewer.
- Decision.
- Note.
- Timestamp.
- Previous status.
- Resulting status.

Decision MVP:

```text
CONTINUE_MONITORING
REQUEST_FOLLOWUP
ESCALATE
DISMISS
```

Các decision chưa được phê duyệt chính thức phải đánh dấu:

```text
DEMO_ONLY
```

Expert Review không được sửa raw message hoặc AI Analysis Result.

---

# 10. Safety and Program Matching

Safety Gate phải chạy trước Program Matching.

Rule bắt buộc:

```text
final_risk_level >= 3
→ automated matching blocked
```

Matching Decision khi bị chặn:

```text
decision_type = SAFETY_BLOCKED
```

Reason code:

```text
ACTIVE_HIGH_RISK
SAFETY_EVENT_OPEN
INSUFFICIENT_SAFETY_CLEARANCE
```

Không được bỏ Safety Gate chỉ vì:

- User yêu cầu bắt đầu chương trình.
- Candidate score cao.
- Dữ liệu profile đầy đủ.
- LLM đề xuất một chương trình cụ thể.

---

# 11. Safety and Active CBT Program

Khi user đang ở trạng thái `ACTIVE` và phát sinh Risk Level 3–4:

Hệ thống phải:

1. Tạo Safety Event.
2. Ghi Risk State History.
3. Ghi Program State Transition nếu state thay đổi.
4. Tạm dừng hoặc escalate chương trình theo rule được phê duyệt.
5. Không tự động giao bài tập mới khi bị block.
6. Không xóa progress đã có.

Transition có thể sử dụng:

```text
ACTIVE → ESCALATED
ACTIVE → PAUSED
```

Rule cụ thể:

```text
TODO_EXPERT_REVIEW
```

Cursor không được tự quyết định khi nào dùng `PAUSED` hay `ESCALATED`.

---

# 12. Approved Safety Response

Risk Level 4 phải sử dụng fixed response.

Safety Response phải được lưu dưới dạng nội dung được quản lý version.

MVP có thể sử dụng cấu hình hoặc seed:

```text
SAFETY_LEVEL_4_RESPONSE_V1
```

Nội dung thực tế:

```text
TODO_EXPERT_REVIEW
```

Không hard-code nội dung dài trực tiếp trong service Java nếu có thể quản lý bằng configuration hoặc database.

---

# 13. CBT General Principles

CBT trong MindBridge AI là chương trình tự hỗ trợ có cấu trúc.

CBT không được sử dụng để:

- Chẩn đoán.
- Cam kết điều trị khỏi.
- Thay thế chuyên gia.
- Tự động xử lý tình huống khẩn cấp.
- Tự động thay đổi nội dung dựa trên text do LLM sáng tác.

Nội dung CBT phải:

- Được định nghĩa trước.
- Có version.
- Có trạng thái duyệt.
- Có source hoặc tài liệu tham chiếu.
- Không bị sửa khi user đang sử dụng version đó.

---

# 14. CBT Versioning Rules

Các entity logical:

```text
intervention_programs
program_modules
exercise_templates
```

Các entity version:

```text
intervention_program_versions
program_module_versions
exercise_template_versions
```

Quy tắc:

- Logical entity giữ identity ổn định.
- Version entity giữ nội dung bất biến.
- Không update nội dung của version đã approved.
- Khi thay đổi nội dung phải tạo version mới.
- User Program phải trỏ vào chính xác Program Version.
- Exercise Assignment phải trỏ vào chính xác Exercise Version.
- Version cũ không bị ảnh hưởng khi có version mới.

Ví dụ:

```text
CBT-STRESS V1
CBT-STRESS V2
```

User đã bắt đầu V1 vẫn tiếp tục V1.

---

# 15. CBT Content Status

Status đề xuất:

```text
DRAFT
PENDING_REVIEW
APPROVED
RETIRED
```

Quy tắc:

- `DRAFT`: được phép sửa.
- `PENDING_REVIEW`: chờ duyệt, không dùng cho user thật.
- `APPROVED`: bất biến.
- `RETIRED`: không gán cho user mới.
- User đang sử dụng version retired vẫn phải xem được nội dung lịch sử cần thiết.

---

# 16. CBT Program State Machine

State:

```text
PROPOSED
ACCEPTED
BASELINE
ACTIVE
PAUSED
ESCALATED
FINAL_ASSESSMENT
COMPLETED
MAINTENANCE
WITHDRAWN
```

Flow chính:

```text
PROPOSED
→ ACCEPTED
→ BASELINE
→ ACTIVE
→ FINAL_ASSESSMENT
→ COMPLETED
→ MAINTENANCE
```

Flow phụ:

```text
PROPOSED → WITHDRAWN
ACCEPTED → WITHDRAWN
BASELINE → WITHDRAWN
ACTIVE → PAUSED
PAUSED → ACTIVE
ACTIVE → ESCALATED
PAUSED → ESCALATED
ACTIVE → WITHDRAWN
ESCALATED → WITHDRAWN
COMPLETED → MAINTENANCE
```

Mọi transition không nằm trong danh sách phải bị từ chối.

---

# 17. Program State Transition Rules

Mỗi transition phải lưu:

- User Program.
- From State.
- To State.
- Trigger Type.
- Trigger ID.
- Actor Type.
- Actor ID.
- Reason Code.
- Occurred At.

Không update state mà không tạo transition history.

Quy trình:

```text
Validate current state
→ Validate requested transition
→ Update user_programs.state
→ Insert program_state_transitions
→ Commit cùng transaction
```

---

# 18. Program Acceptance Rules

User chỉ được accept chương trình khi:

- Program đang ở trạng thái `PROPOSED`.
- Program Version tồn tại.
- Program Version được phép sử dụng.
- User là owner của User Program.
- Không có active Safety block.
- Matching Decision hợp lệ nếu chương trình đến từ Matching.

Khi accept:

```text
PROPOSED → ACCEPTED
```

Phải tạo Behavioral Event:

```text
PROGRAM_ACCEPTED
```

---

# 19. Baseline Rules

User Program phải hoàn thành Baseline trước khi chuyển sang `ACTIVE`.

Flow:

```text
ACCEPTED
→ BASELINE
→ ACTIVE
```

Baseline Assessment phải:

- Gắn với đúng User Program.
- Có assessment version hoặc schema version.
- Có timestamp.
- Không bị overwrite.
- Không được coi là chẩn đoán.

---

# 20. Module Unlock Rules

Module đầu tiên có thể được mở khi chương trình chuyển sang `ACTIVE`.

Module tiếp theo chỉ được mở khi:

- Module trước hoàn thành.
- Unlock condition được đáp ứng.
- Không có Safety block.
- Program đang ở state cho phép.

Unlock conditions thực tế:

```text
TODO_EXPERT_REVIEW
```

Không để frontend tự quyết định module nào được mở.

---

# 21. Exercise Assignment Rules

Exercise Assignment phải:

- Thuộc User Program.
- Thuộc đúng Module Version.
- Trỏ tới đúng Exercise Template Version.
- Không giao trùng không chủ đích.
- Có assigned time.
- Có status.
- Có due date khi cần.

Status đề xuất:

```text
ASSIGNED
STARTED
COMPLETED
SKIPPED
EXPIRED
```

Không giao Exercise Version đang ở trạng thái `DRAFT` hoặc `PENDING_REVIEW`.

---

# 22. Exercise Submission Rules

Submission phải:

- Thuộc đúng Assignment.
- Thuộc đúng User.
- Có answer data phù hợp response schema.
- Có submitted timestamp.
- Có validation.

Các trường có thể gồm:

- Pre-intensity.
- Post-intensity.
- Difficulty.
- Helpfulness.
- Duration.
- Completion status.

Không dùng nội dung submission để tự động chẩn đoán.

Nếu submission tạo Safety Signal:

```text
Exercise Submission
→ Safety Pipeline
→ Safety Event nếu cần
```

---

# 23. Program Assessment Rules

Assessment Type:

```text
BASELINE
WEEKLY
FINAL
```

Quy tắc:

- Baseline hoàn thành trước Active.
- Weekly dùng để theo dõi tiến độ.
- Final hoàn thành trước Completed.
- Assessment data phải có schema version.
- Không overwrite assessment cũ.
- Reassessment tạo record mới nếu được cho phép.

---

# 24. Program Completion Rules

Program chỉ chuyển sang `COMPLETED` khi:

- Program đang ở `FINAL_ASSESSMENT`.
- Final Assessment đã hoàn thành.
- Không có transition block.
- Completion condition được đáp ứng.

Flow:

```text
ACTIVE
→ FINAL_ASSESSMENT
→ COMPLETED
```

Không coi `COMPLETED` là đã điều trị khỏi.

---

# 25. Maintenance Rules

Maintenance là giai đoạn sau khi hoàn thành chương trình.

Flow:

```text
COMPLETED → MAINTENANCE
```

MVP có thể chỉ lưu state và một số recommendation cơ bản.

Chi tiết Maintenance Plan:

```text
DEFERRED
```

---

# 26. LLM Usage in CBT

LLM được phép:

- Giải thích bài tập bằng ngôn ngữ dễ hiểu.
- Tóm tắt nội dung đã được duyệt.
- Hỗ trợ diễn đạt phản hồi không khẩn cấp.
- Trả lời dựa trên approved knowledge.

LLM không được:

- Tự tạo chương trình CBT mới.
- Tự sửa cấu trúc chương trình.
- Tự tạo Exercise chưa duyệt.
- Tự thay clinical threshold.
- Tự chuyển Program State.
- Tự chọn chương trình cuối cùng.
- Tự tạo emergency response.

---

# 27. Demo-only Rules

Nội dung hoặc threshold chưa được chuyên gia duyệt phải ghi:

```text
DEMO_ONLY
```

Ví dụ:

```text
CBT-STRESS-DEMO-V1
MATCHING_WEIGHT_DEMO_V1
SAFETY_TEMPLATE_DEMO_V1
```

Không trình bày dữ liệu demo như dữ liệu lâm sàng đã xác nhận.

---

# 28. Required Tests

## Safety Tests

Phải test:

- Level 1 cho phép flow bình thường.
- Level 2 ghi follow-up signal.
- Level 3 tạo Safety Event.
- Level 3 chặn Matching.
- Level 4 tạo Safety Event.
- Level 4 chặn Matching.
- Level 4 dùng fixed response.
- AI timeout có fallback.
- JSON sai schema không được lưu thành success.
- Safety Event giữ đúng source.

## CBT Tests

Phải test:

- Không sửa approved version.
- User V1 không bị ảnh hưởng bởi V2.
- Invalid state transition bị từ chối.
- Transition hợp lệ được ghi history.
- User không truy cập User Program của người khác.
- User không nộp bài của người khác.
- Module chưa unlock không được truy cập.
- Final Assessment cần thiết trước Completed.
- Safety block ngăn hành động không phù hợp.

---

# 29. Source-of-Truth Rule

Khi file này mâu thuẫn với task:

1. Dừng triển khai.
2. Báo rõ phần mâu thuẫn.
3. Không tự chọn một phương án.
4. Chờ cập nhật tài liệu hoặc task.

Không được lặng lẽ sửa Safety hoặc CBT logic.