# G4-T01 — Chốt Feature Dictionary MVP

| Field | Value |
|---|---|
| Group | G4 — Phân tích hành vi và Hồ sơ người dùng |
| Priority | MUST |
| Tags | Data/Analysis |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giới hạn phạm vi phân tích vào các chỉ số rõ ràng, có nguồn và cách tính.

## 2. Công việc chi tiết

- Chốt 8 feature: stress, mood, energy, sleep, anxiety signal, engagement, exercise completion, max risk.
- Ghi nguồn dữ liệu, đơn vị, miền giá trị và trường hợp thiếu dữ liệu.
- Định nghĩa feature_version và calculation_version.
- Phân biệt explicit, inferred và behavioral.
- Không dùng tên gọi mang tính chẩn đoán.

## 3. Đầu ra cần bàn giao

Tài liệu Feature Dictionary v1.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Mỗi feature có công thức và nguồn rõ ràng.
- [ ] Hai dev hiểu giống nhau về null/unknown/zero.
- [ ] Feature có test case mẫu.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G4-T01** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
