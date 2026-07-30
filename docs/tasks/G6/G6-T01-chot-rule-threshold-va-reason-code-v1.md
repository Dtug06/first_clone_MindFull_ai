# G6-T01 — Chốt Rule, Threshold và Reason Code v1

| Field | Value |
|---|---|
| Group | G6 — Program Matching và Recommendation |
| Priority | MUST |
| Tags | Nghiệp vụ/Backend |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo bộ quy tắc MVP minh bạch, không để LLM tự chọn chương trình.

## 2. Công việc chi tiết

- Liệt kê eligibility/exclusion cho 4 program ở mức demo.
- Tạo clinical_thresholds cho giá trị đơn và rule_set_versions cho logic ghép.
- Định nghĩa reason code thống nhất.
- Đánh dấu threshold chờ chuyên gia nếu chưa duyệt.
- Không hard-code số rải rác trong Java.

## 3. Đầu ra cần bàn giao

Matching rules v1 và seed/config.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Mỗi rule có version và trạng thái active.
- [ ] Reason code có mô tả cho UI.
- [ ] Thay threshold không cần sửa nhiều service.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G6-T01** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
