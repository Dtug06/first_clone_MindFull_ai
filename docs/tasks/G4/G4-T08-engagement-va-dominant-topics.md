# G4-T08 — Engagement và Dominant Topics

| Field | Value |
|---|---|
| Group | G4 — Phân tích hành vi và Hồ sơ người dùng |
| Priority | SHOULD |
| Tags | Backend/Analysis |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tổng hợp mức tham gia và chủ đề xuất hiện nhiều để phục vụ matching.

## 2. Công việc chi tiết

- Tính engagement từ check-in, chat session, exercise started/completed.
- Chuẩn hóa trọng số event trong Feature Dictionary.
- Tổng hợp topic từ analysis result mới nhất, có confidence threshold.
- Không lưu full text trong profile.
- Hỗ trợ top N topic và frequency.

## 3. Đầu ra cần bàn giao

Engagement score và topic summary.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Engagement nằm trong miền giá trị định nghĩa.
- [ ] Topic rerun không bị đếm trùng.
- [ ] Profile chỉ chứa summary cần thiết.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G4-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
