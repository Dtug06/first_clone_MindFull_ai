# G3-T05 — Lưu Chat Analysis Result dạng versioned

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tách kết quả suy luận khỏi message và cho phép chạy lại.

## 2. Công việc chi tiết

- Tạo chat_analysis_results gắn run_id, message_id và user_id.
- Lưu topics, emotions, signals, intent, confidence và risk sơ bộ.
- Thêm analysis_status, supersedes_id hoặc version để rerun không ghi đè lịch sử.
- Tách model risk và final risk nếu cần.
- Tạo index phục vụ tổng hợp theo user/date.

## 3. Đầu ra cần bàn giao

Analysis result schema và repository.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Chạy lại cùng message tạo result mới có liên kết lịch sử.
- [ ] Message không bị sửa khi model thay đổi.
- [ ] Query lấy result hiệu lực mới nhất hoạt động đúng.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T05** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
