# G1-T09 — Audit cơ bản và request tracing

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | SHOULD |
| Tags | Backend/Security |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo khả năng truy vết thao tác nhạy cảm mà không ghi raw data không cần thiết.

## 2. Công việc chi tiết

- Sinh requestId/traceId cho mỗi request.
- Ghi audit cho đăng nhập thất bại, thay đổi consent, thay đổi role và thao tác admin.
- Dùng user pseudonym hoặc ID phù hợp trong log.
- Không ghi password, token, raw chat hoặc bài tập nhạy cảm vào application log.
- Quy định retention log cho môi trường demo.

## 3. Đầu ra cần bàn giao

Audit log nền tảng và quy tắc logging.

## 4. Hoàn thành khi (Definition of Done)

- [x] Mỗi sự kiện audit có actor, action, resource, result và timestamp. → AuditService.record() ghi row với AuditCategory + AuditActions code, actor UUID + type, subject type + UUID, requestId, createdAt = @PrePersist timestamp. Test verify login fail row có đủ các trường.
- [x] Kiểm tra log không chứa token/password/raw chat. → Audit row lưu emailHash (SHA-256) thay vì plain email; AuthService.login() không log password; consent audit metadata chỉ chứa consentType + policyVersion (không raw user data); logback pattern chỉ emit MDC requestId, không có field nào liên quan tới PII. LOGGING.md document quy tắc này.
- [x] Có thể tìm audit theo requestId. → LoggingRequestContextFilter sinh/đọc X-Request-Id → MDC → response header; AuditService.record() đọc RequestContext.getRequestId() rồi lưu vào audit_logs.request_id; repo `findByRequestId` + index mới (`idx_audit_logs_request_id`). Test findByRequestId verify cả login fail và consent event đều tìm được qua cùng requestId.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T09** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
