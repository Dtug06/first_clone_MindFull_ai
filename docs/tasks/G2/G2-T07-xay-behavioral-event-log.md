# G2-T07 — Xây Behavioral Event Log

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Backend/Data |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Chuẩn hóa các hành động quan trọng thành event phục vụ phân tích.

## 2. Công việc chi tiết

- Tạo bảng behavioral_events với event_type, source_type, source_id, occurred_at, local_date, timezone và properties.
- Định nghĩa enum event ban đầu: session started, message sent, check-in completed/skipped.
- Tạo service ghi event dùng chung và bảo đảm ghi trong transaction phù hợp.
- Không nhét raw content vào properties.
- Index theo user_id, occurred_at và event_type.

## 3. Đầu ra cần bàn giao

Behavioral Event service và schema.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Mỗi hành động nghiệp vụ tạo đúng một event cần thiết.
- [ ] Event có source_id để truy ngược bản ghi gốc.
- [ ] Retry request không tạo event trùng ngoài dự kiến.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
