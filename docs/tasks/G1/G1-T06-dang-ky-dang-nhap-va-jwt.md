# G1-T06 — Đăng ký, đăng nhập và JWT

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend/Auth |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Cho phép user xác thực an toàn trước khi truy cập dữ liệu cá nhân.

## 2. Công việc chi tiết

- Tạo API đăng ký và kiểm tra email không trùng.
- Hash password bằng thuật toán phù hợp của Spring Security.
- Tạo API đăng nhập và phát access token JWT.
- Tạo filter đọc JWT và thiết lập SecurityContext.
- Tạo API GET /users/me.
- Quy định thời hạn token và cách frontend xử lý khi token hết hạn.

## 3. Đầu ra cần bàn giao

Authentication API hoạt động với frontend.

## 4. Hoàn thành khi (Definition of Done)

- [x] User đăng ký và đăng nhập thành công. → 13 integration tests pass (register, login, protected endpoint, error paths, security).
- [x] Password không bao giờ xuất hiện trong response/log. → 3 dedicated security tests verify passwordHash absent in all responses.
- [x] API bảo vệ trả 401 khi thiếu hoặc sai token. → Tested: no token → 401, invalid token → 401.
- [x] User chỉ nhận được thông tin của chính mình. → GET /users/me reads userId from JWT principal, not from request body.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T06** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
