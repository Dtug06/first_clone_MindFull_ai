# G6-T12 — Explainability, audit và test matching

| Field | Value |
|---|---|
| Group | G6 — Program Matching và Recommendation |
| Priority | MUST |
| Tags | Backend/Test |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Chứng minh quyết định đúng với dữ liệu seed và có thể debug.

## 2. Công việc chi tiết

- Tạo response gồm selected program, candidate phụ, component score và reason code.
- Không expose internal clinical wording không phù hợp cho user.
- Viết unit test gate, eligibility, scoring, tie-breaker.
- Viết integration test snapshot -> decision -> user_program.
- Tạo bộ 10-20 kịch bản matching giả.

## 3. Đầu ra cần bàn giao

Matching test suite và admin explanation.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Các fixture cho kết quả xác định.
- [ ] Safety test luôn ưu tiên.
- [ ] Có thể truy vết rule/threshold/snapshot cho mọi decision.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G6-T12** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
