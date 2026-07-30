# G8-T11 — Cấu hình môi trường và quản lý secret

| Field | Value |
|---|---|
| Group | G8 — Kiểm thử, Bảo mật, Triển khai và Tài liệu |
| Priority | MUST |
| Tags | DevOps/Security |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tách local/test/prod và tránh lỗi cấu hình khi deploy.

## 2. Công việc chi tiết

- Tạo .env.example cho frontend/backend.
- Định nghĩa biến DB, JWT, LLM provider, CORS và feature flags.
- Không dùng default secret yếu ở prod.
- Tạo mock AI mode cho dev/test.
- Ghi hướng dẫn rotate/revoke API key nếu lộ.

## 3. Đầu ra cần bàn giao

Environment configuration guide.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Project chạy local theo hướng dẫn mới.
- [ ] Production config không dùng secret trong Git.
- [ ] Mock/real provider được chọn rõ ràng.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G8-T11** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
