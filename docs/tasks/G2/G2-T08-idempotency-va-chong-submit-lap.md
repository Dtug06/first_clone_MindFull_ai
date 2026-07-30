# G2-T08 — Idempotency và chống submit lặp

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | SHOULD |
| Tags | Backend |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giảm lỗi double-click hoặc retry mạng tạo dữ liệu trùng.

## 2. Công việc chi tiết

- Chọn requestId/idempotency key cho gửi message và submit answer.
- Tạo unique constraint hoặc bảng idempotency tối giản.
- Khi request trùng, trả lại kết quả cũ thay vì tạo bản ghi mới.
- Frontend khóa nút trong lúc submit và gửi idempotency key.
- Ghi test retry cùng payload.

## 3. Đầu ra cần bàn giao

Cơ chế chống duplicate cho endpoint quan trọng.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Double-click không tạo hai answer/message ngoài mong muốn.
- [ ] Request trùng trả cùng resource ID.
- [ ] Không gây lỗi race condition cơ bản.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
