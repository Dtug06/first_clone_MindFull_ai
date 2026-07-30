# G4-T07 — Trend, streak và thay đổi so với baseline

| Field | Value |
|---|---|
| Group | G4 — Phân tích hành vi và Hồ sơ người dùng |
| Priority | MUST |
| Tags | Backend/Analysis |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Phát hiện xu hướng đơn giản mà không cần machine learning.

## 2. Công việc chi tiết

- Định nghĩa trend UP/DOWN/STABLE bằng công thức/version cụ thể.
- Tính streak check-in và streak high-stress nếu cần.
- So sánh cửa sổ gần với baseline hoặc cửa sổ trước.
- Đặt minimum data coverage trước khi kết luận trend.
- Lưu reason/data quality khi trend UNKNOWN.

## 3. Đầu ra cần bàn giao

Trend and streak calculator.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Trend được test với dữ liệu tăng, giảm, ổn định và thiếu.
- [ ] Không kết luận trend khi coverage thấp.
- [ ] Công thức có version.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G4-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
