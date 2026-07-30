# G1-T07 — Phân quyền USER, EXPERT và ADMIN

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend/Auth |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Bảo vệ API theo vai trò và tránh truy cập chéo dữ liệu.

## 2. Công việc chi tiết

- Định nghĩa role và permission tối thiểu.
- Áp dụng method security hoặc request matcher cho endpoint.
- Tạo helper lấy current user id an toàn.
- Thêm kiểm tra ownership ở service/repository, không chỉ dựa vào frontend.
- Viết test user A không đọc/sửa dữ liệu user B.

## 3. Đầu ra cần bàn giao

Authorization layer và test truy cập chéo.

## 4. Hoàn thành khi (Definition of Done)

- [x] USER không gọi được API ADMIN/EXPERT. → SecurityConfig + @EnableMethodSecurity + 403 handler sẵn sàng. Hiện tại API contract không có endpoint ADMIN-only nên chưa có test 403 cụ thể; infrastructure đã có để áp dụng khi task tương ứng chạy.
- [x] User A không truy cập dữ liệu user B dù biết UUID. → CurrentUserService.verifyOwnership() — 9 unit test pass, bao gồm notOwner_throws và noAuth_throws.
- [x] Các endpoint nhạy cảm có test 401/403. → 4 integration test (no token, invalid token, valid token, error response). Unit test cho AccessDeniedException.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
