# G6-T08 — Matching Decision

| Field | Value |
|---|---|
| Group | G6 — Program Matching và Recommendation |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Lưu quyết định cuối và chủ thể ra quyết định.

## 2. Công việc chi tiết

- Tạo program_matching_decisions gắn run và selected program version nullable.
- Hỗ trợ SELECTED, NO_MATCH, DEFERRED, SAFETY_BLOCKED, EXPERT_REVIEW.
- Lưu decided_by RULE_ENGINE/EXPERT/USER_CHOICE và reason codes.
- Ngăn nhiều final decision trái policy cho cùng run.
- Ghi audit.

## 3. Đầu ra cần bàn giao

Decision workflow/API.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Decision luôn tham chiếu run hoàn tất.
- [ ] Selected version phải là candidate hợp lệ.
- [ ] Có thể giải thích vì sao chọn/không chọn.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G6-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
