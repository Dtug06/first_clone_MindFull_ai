# G5-T15 — CBT Frontend Integration và Runtime Test

| Field | Value |
|---|---|
| Group | G5 — Danh mục và Vận hành liệu trình CBT |
| Priority | MUST |
| Tags | Full-stack/CBT |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Cho user hoàn thành vertical slice CBT bằng UI hiện có.

## 2. Công việc chi tiết

- Kết nối program detail, module list, exercise form và progress.
- Kết nối baseline/final assessment.
- Hiển thị version, trạng thái và lỗi transition phù hợp.
- Tạo kịch bản gán program thủ công trước khi G6 hoàn thành.
- Viết integration test từ ACCEPTED đến một submission hoàn thành.

## 3. Đầu ra cần bàn giao

CBT UI/API chạy end-to-end.

## 4. Hoàn thành khi (Definition of Done)

- [ ] User test đi từ PROPOSED đến ACTIVE và hoàn thành bài đầu tiên.
- [ ] Reload trang không mất progress.
- [ ] Program V1 không bị ảnh hưởng khi tạo V2.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G5-T15** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
