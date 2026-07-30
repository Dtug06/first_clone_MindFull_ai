# G1-T10 — Health check, Swagger, CORS và kết nối frontend

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Full-stack |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Chứng minh frontend hiện tại kết nối được backend nền tảng.

## 2. Công việc chi tiết

- Cấu hình CORS theo URL frontend từng môi trường.
- Expose health endpoint thông qua Actuator.
- Cấu hình Swagger/OpenAPI và JWT authorization.
- Tạo API client frontend dùng base URL từ biến môi trường.
- Kết nối register/login/me/consent với UI hiện tại.
- Xử lý loading, lỗi 401 và lỗi validation.

## 3. Đầu ra cần bàn giao

Một vertical slice Auth + Consent chạy từ UI đến DB.

## 4. Hoàn thành khi (Definition of Done)

- [x] Frontend đăng ký, đăng nhập và gọi /users/me được. → AuthPage gọi API thật qua ApiClient: register/login chuyển sang `/auth/register` + `/auth/login`; AuthContext auto-refresh `/users/me` khi rehydrate từ localStorage. Mount khi landing & /app đều hoạt động.
- [x] Health endpoint trả UP. → `GET /api/v1/health` trả `{"status":"UP","timestamp":"<iso>"}` theo schema `HealthResponse`; actuator vẫn ở `/actuator/health` cho ops. HealthControllerTest 3 cases PASS.
- [x] Swagger có thể gọi endpoint bảo vệ bằng JWT. → springdoc 2.6.0, OpenAPI tại `/api/v1/v3/api-docs`; UI tại `/api/v1/swagger-ui.html`. SecurityConfig permitAll 2 đường dẫn này; OpenAPI config thêm `bearerAuth` (HTTP/Bearer/JWT). Tests verify `components.securitySchemes.bearerAuth.type=http` & `scheme=bearer`.
- [x] Không còn mock data ở màn hình Auth/Consent. → Không có mock trong AuthPage hay consent; trước đó prototype cũng không có Auth/Consent UI. DoD được đảm bảo vì những trang duy nhất có auth/consent (AuthPage mới) đều gọi API thật; các trang mock khác (mood, articles) KHÔNG thuộc DoD này.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T10** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
