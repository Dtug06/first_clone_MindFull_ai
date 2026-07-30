# G3-T12 — Phản hồi cố định cho Level 4

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/Content |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Không để LLM sinh phản hồi tự do trong tình huống khẩn cấp.

## 2. Công việc chi tiết

- Tạo bảng hoặc catalog safety_response_templates có version và approval.
- Chọn template theo locale và risk reason.
- Không lưu hotline giả; nội dung demo phải được đánh dấu chờ chuyên gia nếu chưa duyệt.
- Bảo đảm template được trả ngay cả khi LLM provider lỗi.
- Ghi template version vào safety event/action.

## 3. Đầu ra cần bàn giao

Fixed Level 4 response flow.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Level 4 không gọi free-form generation.
- [ ] Response dùng đúng template approved/configured.
- [ ] Test khi provider AI unavailable vẫn trả được Safety response.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T12** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
