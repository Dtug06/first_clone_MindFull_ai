# G5-T08 — Program State Transition History

| Field | Value |
|---|---|
| Group | G5 — Danh mục và Vận hành liệu trình CBT |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Lưu lịch sử trạng thái append-only thay vì dùng review thay thế.

## 2. Công việc chi tiết

- Tạo program_state_transitions với from/to, trigger, actor, reason và occurred_at.
- Ghi transition trong cùng transaction với cập nhật user_program.
- Hỗ trợ trigger từ user, system, safety và expert.
- Không sửa/xóa transition trong nghiệp vụ thông thường.
- Tạo API timeline.

## 3. Đầu ra cần bàn giao

Transition history và timeline API.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Mỗi đổi state có đúng một history row.
- [ ] Timeline sắp xếp đúng và truy vết actor/trigger.
- [ ] Review không bị dùng lẫn làm state log.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G5-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
