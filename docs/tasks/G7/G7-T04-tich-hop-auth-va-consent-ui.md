# G7-T04 — Tích hợp Auth và Consent UI

| Field | Value |
|---|---|
| Group | G7 — Kết nối Frontend, Dashboard và Admin |
| Priority | MUST |
| Tags | Frontend |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Hoàn thiện vertical slice đầu tiên.

## 2. Công việc chi tiết

- Kết nối register/login/me.
- Bảo vệ route theo auth state.
- Kết nối cấp/thu hồi consent.
- Hiển thị policy version và trạng thái consent.
- Xóa mock user/auth.

## 3. Đầu ra cần bàn giao

Auth/Consent UI dùng API thật.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Reload vẫn giữ/khôi phục auth đúng policy.
- [ ] Consent ảnh hưởng flow AI.
- [ ] Không hiển thị màn hình protected khi chưa auth.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G7-T04** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
