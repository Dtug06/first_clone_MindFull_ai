# G7-T11 — Loading, Empty, Error và Accessibility States

| Field | Value |
|---|---|
| Group | G7 — Kết nối Frontend, Dashboard và Admin |
| Priority | MUST |
| Tags | Frontend/Quality |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Hoàn thiện trải nghiệm lỗi thay vì chỉ happy path.

## 2. Công việc chi tiết

- Chuẩn hóa spinner/skeleton/loading button.
- Tạo empty state cho session, profile, matching, program.
- Tạo error boundary hoặc error page.
- Bảo đảm label, keyboard focus và contrast cơ bản.
- Không lộ lỗi kỹ thuật/provider trực tiếp cho user.

## 3. Đầu ra cần bàn giao

UI state components dùng chung.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Các màn hình chính có loading/empty/error.
- [ ] Form sử dụng được bằng bàn phím cơ bản.
- [ ] Lỗi backend được chuyển thành message thân thiện.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G7-T11** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
