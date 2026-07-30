# G7-T07 — Dashboard hành vi

| Field | Value |
|---|---|
| Group | G7 — Kết nối Frontend, Dashboard và Admin |
| Priority | MUST |
| Tags | Frontend/Data |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giúp user nhìn thấy xu hướng mà không diễn giải như chẩn đoán.

## 2. Công việc chi tiết

- Hiển thị stress, mood, sleep, energy 7 ngày.
- Hiển thị avg/trend/coverage/confidence.
- Xử lý ngày thiếu dữ liệu bằng gap hoặc trạng thái unknown.
- Thêm thông báo đây là dữ liệu tự theo dõi, không phải chẩn đoán.
- Tối ưu responsive.

## 3. Đầu ra cần bàn giao

Behavior Dashboard.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Biểu đồ dùng data thật.
- [ ] Không nối giả các điểm thiếu dữ liệu gây hiểu sai.
- [ ] Coverage thấp có thông báo.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G7-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
