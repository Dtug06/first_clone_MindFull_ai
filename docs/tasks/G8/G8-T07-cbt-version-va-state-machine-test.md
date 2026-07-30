# G8-T07 — CBT Version và State Machine Test

| Field | Value |
|---|---|
| Group | G8 — Kiểm thử, Bảo mật, Triển khai và Tài liệu |
| Priority | MUST |
| Tags | Backend/CBT/Test |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Bảo vệ lịch sử liệu trình và transition.

## 2. Công việc chi tiết

- Test program/module/exercise V1 không đổi khi tạo V2.
- Test state transition hợp lệ/không hợp lệ.
- Test unlock module, assignment và submission ownership.
- Test pause/resume/escalate/withdraw.
- Test transaction khi submit/update progress.

## 3. Đầu ra cần bàn giao

CBT regression suite.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Version cũ không bị ảnh hưởng.
- [ ] Transition sai bị chặn.
- [ ] Không nộp bài hoặc mở khóa sai user/module.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G8-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
