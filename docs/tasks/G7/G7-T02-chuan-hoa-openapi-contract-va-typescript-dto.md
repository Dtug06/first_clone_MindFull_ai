# G7-T02 — Chuẩn hóa OpenAPI Contract và TypeScript DTO

| Field | Value |
|---|---|
| Group | G7 — Kết nối Frontend, Dashboard và Admin |
| Priority | MUST |
| Tags | Full-stack/Contract |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Ngăn frontend/backend tự đổi tên field và format.

## 2. Công việc chi tiết

- Tạo/hoàn thiện openapi.yaml hoặc Swagger source of truth.
- Định nghĩa DTO Auth, Chat, Check-in, Profile, Matching và CBT.
- Sinh hoặc viết TypeScript types thống nhất.
- Quy định date/time ISO-8601, enum và nullability.
- Thay contract trước khi thay implementation.

## 3. Đầu ra cần bàn giao

API contract và frontend types.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Frontend compile khi DTO khớp.
- [ ] Không còn duplicate interface khác tên cùng nghĩa.
- [ ] API change được review bởi cả hai dev.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G7-T02** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
