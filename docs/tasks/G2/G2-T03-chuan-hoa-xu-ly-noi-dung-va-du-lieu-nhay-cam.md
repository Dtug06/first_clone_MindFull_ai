# G2-T03 — Chuẩn hóa xử lý nội dung và dữ liệu nhạy cảm

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Backend/Privacy |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giảm rủi ro lộ thông tin trước khi chuyển nội dung sang AI hoặc log.

## 2. Công việc chi tiết

- Đặt giới hạn độ dài message và validate nội dung rỗng.
- Tạo bước redaction tối thiểu cho email, số điện thoại hoặc identifier không cần thiết.
- Lưu cờ redacted và quy định raw/redacted content dùng ở đâu.
- Không ghi full message trong exception/log.
- Xử lý Unicode và newline ổn định.

## 3. Đầu ra cần bàn giao

Message preprocessing component.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Message vượt giới hạn bị từ chối có lỗi rõ ràng.
- [ ] Log không chứa raw message.
- [ ] Đầu vào gửi AI là phiên bản đã qua preprocessing theo quy định.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T03** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
