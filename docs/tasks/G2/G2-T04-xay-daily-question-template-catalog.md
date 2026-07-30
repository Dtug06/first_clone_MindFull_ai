# G2-T04 — Xây Daily Question Template Catalog

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Quản lý câu hỏi hằng ngày theo version và loại câu trả lời.

## 2. Công việc chi tiết

- Tạo template cho stress, mood, sleep, energy và câu hỏi mở ngắn.
- Hỗ trợ answer type: scale, number, single choice, text.
- Tạo bảng option cho câu hỏi lựa chọn.
- Thêm status draft/approved/retired và version.
- Tạo API admin đọc/tạo/cập nhật bằng version mới thay vì sửa nội dung đã dùng.

## 3. Đầu ra cần bàn giao

Daily Question Catalog và seed cơ bản.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Có ít nhất 4 câu hỏi MVP được seed.
- [ ] Template đã được giao cho user không bị sửa ngược lịch sử.
- [ ] Option đúng thứ tự và thuộc đúng template.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T04** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
