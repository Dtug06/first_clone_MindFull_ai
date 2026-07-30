# G3-T02 — Định nghĩa JSON Schema và từ điển tín hiệu

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | AI/Data |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giới hạn output AI vào bộ trường ổn định cho MVP.

## 2. Công việc chi tiết

- Chốt topic, emotion, intent, behavior signal, risk và confidence.
- Quy định miền giá trị 0-1 và enum hợp lệ.
- Định nghĩa evidence span dạng start/end/message hash nếu dùng.
- Tạo schema_version và tài liệu giải thích từng field.
- Không đưa chẩn đoán lâm sàng vào schema.

## 3. Đầu ra cần bàn giao

JSON Schema v1 và Java DTO tương ứng.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Schema validate được output mẫu đúng/sai.
- [ ] Frontend/backend dùng cùng tên field.
- [ ] Mọi field có định nghĩa và đơn vị rõ ràng.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T02** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
