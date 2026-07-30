# G8-T02 — Migration và Database Integrity Test

| Field | Value |
|---|---|
| Group | G8 — Kiểm thử, Bảo mật, Triển khai và Tài liệu |
| Priority | MUST |
| Tags | Backend/DB/Test |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Bảo đảm schema có thể dựng lại và constraint hoạt động.

## 2. Công việc chi tiết

- Test Flyway từ database trắng.
- Test unique, FK, CHECK và cascade/restrict quan trọng.
- Không chỉnh migration đã applied.
- Test rollback strategy bằng backup/restore hoặc migration sửa lỗi mới.
- Kiểm tra index cho query chính.

## 3. Đầu ra cần bàn giao

Migration test suite/report.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Database trắng lên latest version không lỗi.
- [ ] Constraint ngăn dữ liệu sai quan trọng.
- [ ] Có hướng dẫn xử lý migration fail.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G8-T02** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
