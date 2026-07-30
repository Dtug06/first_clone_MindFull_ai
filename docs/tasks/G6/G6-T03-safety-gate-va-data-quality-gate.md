# G6-T03 — Safety Gate và Data Quality Gate

| Field | Value |
|---|---|
| Group | G6 — Program Matching và Recommendation |
| Priority | MUST |
| Tags | Backend/Matching |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Chặn matching khi risk hoặc dữ liệu không cho phép.

## 2. Công việc chi tiết

- Nếu final risk >= 3, tạo decision SAFETY_BLOCKED hoặc không tạo candidates theo policy.
- Nếu coverage/confidence dưới ngưỡng, decision DEFERRED/INSUFFICIENT_DATA.
- Lưu reason code và threshold/rule version.
- Không cho user bắt đầu program tự động khi bị chặn.
- Viết test ưu tiên Safety trước scoring.

## 3. Đầu ra cần bàn giao

Pre-matching gates.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Risk Level 3-4 luôn chặn.
- [ ] Coverage thấp không tạo kết luận giả.
- [ ] Decision giải thích được lý do chặn.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G6-T03** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
