# G5-T07 — User Program và State Machine

| Field | Value |
|---|---|
| Group | G5 — Danh mục và Vận hành liệu trình CBT |
| Priority | MUST |
| Tags | Backend/CBT |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Quản lý instance liệu trình của từng user bằng transition hợp lệ.

## 2. Công việc chi tiết

- Tạo user_programs trỏ program_version_id và matching_decision_id nullable.
- Định nghĩa state: PROPOSED, ACCEPTED, BASELINE, ACTIVE, PAUSED, ESCALATED, FINAL_ASSESSMENT, COMPLETED, MAINTENANCE, WITHDRAWN.
- Tạo StateMachineGuard/service kiểm tra transition.
- Dùng optimistic lock/version column.
- Lưu timestamps theo state quan trọng.

## 3. Đầu ra cần bàn giao

User program state machine.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Transition sai bị từ chối.
- [ ] Hai request đồng thời không làm mất trạng thái.
- [ ] Có unit test toàn bộ transition chính.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G5-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
