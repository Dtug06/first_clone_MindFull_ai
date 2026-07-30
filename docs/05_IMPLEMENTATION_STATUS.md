# MindBridge AI — Implementation Status

## 1. Purpose

File này mô tả trạng thái hiện tại của source code.

Mục tiêu:

- Giúp hai developer biết phần nào đã hoàn thành.
- Giúp Cursor không tạo lại chức năng đã tồn tại.
- Theo dõi API, migration và frontend integration.
- Ghi nhận mock data, giới hạn và lỗi đã biết.
- Hỗ trợ chọn task tiếp theo.

File này phải phản ánh source code thực tế.

Không ghi một chức năng là `COMPLETED` nếu chưa kiểm tra source, build hoặc test.

---

# 2. Status Definitions

| Status | Meaning |
|---|---|
| NOT_STARTED | Chưa bắt đầu |
| READY | Đã đủ thông tin để triển khai |
| IN_PROGRESS | Đang triển khai |
| BLOCKED | Bị chặn bởi dependency hoặc vấn đề kỹ thuật |
| IN_REVIEW | Đã code, đang review hoặc test |
| COMPLETED | Đã merge và đạt Definition of Done |
| DEFERRED | Hoãn khỏi MVP |
| MOCK_ONLY | Chỉ có mock, chưa có backend hoặc dữ liệu thật |
| PARTIAL | Có một phần nhưng chưa hoàn thiện toàn bộ |

---

# 3. Project Summary

- Project: MindBridge AI
- Architecture: Modular Monolith
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL + Flyway
- Frontend: React
- AI: Hosted pretrained LLM API
- Team size: 2 developers
- Current branch: `develop`
- Current release target: `MVP`
- Last updated: `2026-07-30`
- Updated by: `TMon283`
- Frozen frontend baseline: tag `v0.0.1-frontend-baseline` → commit `128eece`

---

## Current Repository State

### Frontend

- Status: UI_PROTOTYPE
- Location: repository root
- Source directory: `src/`
- Framework: React 18 + TypeScript + Vite
- Styling: Tailwind CSS
- Routing: React Router with HashRouter
- Data source: Mock and placeholder data
- API integration: NOT_STARTED
- Authentication: NOT_STARTED
- Authorization: NOT_STARTED
- Real AI integration: NOT_STARTED

### Backend

- Status: NOT_STARTED
- Planned location: `backend/`
- Planned stack: Java 21 + Spring Boot
- API: NOT_STARTED
- Tests: NOT_STARTED

### Database

- Status: NOT_STARTED
- Planned database: PostgreSQL
- Flyway migrations: NOT_STARTED

### Important Note

Existing frontend pages represent intended UI only.

They do not prove that corresponding business functions are implemented.

---

# 4. Current Development Focus

```text
Current Group:
G1 — Backend Foundation, Auth and Consent

Current Task:
G1-T10 — DONE. G1 MUST package (10/10 tasks) hoàn thành.

Current Goal:
Cả 10 MUST task của G1 đã PASS. G2 (Chat + Daily Check-in) có thể bắt đầu. Backend foundation đầy đủ: PostgreSQL/Flyway, Auth (JWT), Authorization (RBAC), Consent (append-only history), Audit (LOGIN_FAILED + CONSENT events), request tracing (X-Request-Id + MDC), Health, OpenAPI/Swagger UI, CORS profile-driven. Frontend đã có shared API client + AuthContext + AuthPage minimal chứng minh kết nối API thật tới backend.

Current Blockers:
- PostgreSQL 17 service đang chạy (postgresql-x64-17 = Running, port 5432).
- mindbridge_dev + mindbridge_app cần user tạo thủ công qua psql (đã ghi trong backend/README.md § "Database setup").
- Flyway chỉ verify được trên PostgreSQL thật, không chạy được trên H2 (profile `test` dùng H2 → Flyway disabled).
- Frontend `npm run build` chưa verify end-to-end (không do T04 gây ra).
```

---

## 4a. Ready Task (tiếp theo)

```text
G1-T07 — Phân quyền USER, EXPERT và ADMIN
- Phụ thuộc: G1-T06
- Ưu tiên: MUST

G1-T08 — Quản lý consent dạng lịch sử
- Phụ thuộc: G1-T06
- Ưu tiên: MUST
```

Ví dụ:

```text
Current Group:
G1 — Backend Foundation, Auth and Consent

Current Task:
G1-T04-implement-jwt-login

Current Goal:
Frontend đăng nhập bằng API thật và truy cập được protected endpoint.

Current Blockers:
- NONE
```

---

# 5. Group Status

| Group | Name | Status | Owner | Notes |
|---|---|---|---|---|
| G1 | Backend Foundation, Auth and Consent | IN_REVIEW | UNASSIGNED | G1-T01 review local; G1-T02 Spring Boot scaffold local (commit `72866c9`); cả 2 chờ push origin (T01 và T02 cùng chờ GitHub credentials) |
| G2 | Chat, Daily Check-in and Data Collection | NOT_STARTED | UNASSIGNED | |
| G3 | LLM Integration and Safety | NOT_STARTED | UNASSIGNED | |
| G4 | Behavior Analysis and User Profile | NOT_STARTED | UNASSIGNED | |
| G5 | CBT Catalog and Runtime | NOT_STARTED | UNASSIGNED | |
| G6 | Program Matching and Recommendation | NOT_STARTED | UNASSIGNED | |
| G7 | Frontend Integration, Dashboard and Admin | PARTIAL | UNASSIGNED | Frontend cơ bản đã tồn tại |
| G8 | Testing, Security, Deployment and Documentation | IN_PROGRESS | BOTH | Thực hiện xuyên suốt |

---

# 6. Completed Tasks

Chỉ ghi task đã đạt Definition of Done.

| Task ID | Task Name | Owner | Pull Request | Completed Date | Verification |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

Ví dụ:

| Task ID | Task Name | Owner | Pull Request | Completed Date | Verification |
|---|---|---|---|---|---|
| G1-T01 | Initialize Spring Boot backend | Dev A | #1 | 2026-08-01 | `./mvnw test` PASS |
| G1-T02 | Configure PostgreSQL and Flyway | Dev B | #2 | 2026-08-02 | Migration from empty DB PASS |

---

# 7. Tasks In Progress

| Task ID | Task Name | Owner | Started Date | Branch | Current State |
|---|---|---|---|---|---|
| G1-T01 | Đóng băng baseline và chuẩn hóa repository | UNASSIGNED | 2026-07-30 | feature/G1-T01-baseline-standardization | Local commit `8246545` đã ghi; tag `v0.0.1-frontend-baseline` → `128eece` đã tạo; đang chờ push origin (GitHub credentials) và merge vào develop. Verification A1–A11 pass, không touch frontend code, không tạo migration/API/test |
| G1-T02 | Khởi tạo Spring Boot Java 21 | UNASSIGNED | 2026-07-30 | feature/G1-T02-spring-boot-scaffold | **Phase 3 review PASS (2026-07-30)**. Local commits `72866c9` (scaffold) + `10fec7f` (status) + new commit (DoD checklist + review note) trên top của feature/G1-T01. Spring Boot 3.3.5 + Maven Wrapper 3.3.4 + Java 21. 3 starter (web/validation/actuator). 4 profile (base+local/test/prod). Verify: `mvnw clean compile` BUILD SUCCESS (9.96s), `mvnw clean test` BUILD SUCCESS (6.131s, no test yet), `mvnw spring-boot:run` Started in 2.84s, `GET /api/v1/actuator/health` 200 `{"status":"UP"}`, `GET /api/v1/nonexistent` 404 không lộ stack trace. DoD §4.1+§4.2 PASS, §4.3 deferred sang G1-T05 (đã ghi trong task file). 32/32 rules of `00-project-core.mdc` PASS. Đang chờ push origin (T01 chưa merge nên T02 base từ T01 feature). |
| G1-T03 | Thiết lập PostgreSQL và cấu hình môi trường | UNASSIGNED | 2026-07-30 | feature/G1-T03-postgresql-datasource | Phase 2 + 3 review PASS (2026-07-30). Local commit `66ded9d` squash T01+T02+T03. Wired `spring-boot-starter-data-jpa` + `org.postgresql:postgresql` (BOM-managed 42.7.x) + H2 (runtime, profile `test`). Smoke test `DatabaseContextSmokeTest` verify context boots + HikariPool-1 Start completed. DoD §4.2 (no hard-code secret) PASS. DoD §4.3 (UTC) PASS. DoD §4.1: config wiring verified (test profile PASS), cần user boot profile `local` + verify PG connection thật để chốt hoàn toàn. |
| G1-T04 | Thiết lập Flyway và extension PostgreSQL | UNASSIGNED | 2026-07-30 | feature/G1-T03-postgresql-datasource | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 3 migration: V1__enable_extensions.sql (citext + pgcrypto), V2__create_users.sql (8 cột, constraints, indexes), V3__create_consent_and_audit.sql (consent_events + audit_logs). Đã thêm flyway-core + flyway-database-postgresql vào pom.xml (BOM-managed). Đã cấu hình spring.flyway trong application-local.yml + prod.yml; flyway.enabled=false cho profile test (H2 không hỗ trợ CREATE EXTENSION). Đã sửa indentation bug trong application-test.yml trước khi test pass. Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (28.70s, 1/1 test pass), Flyway disabled trong test log, HikariPool-1 Start completed |
| G1-T05 | Chuẩn hóa DTO, validation và API response | UNASSIGNED | 2026-07-30 | feature/G1-T05-dto-validation | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 24 Java source files: common/dto (PageResponse, ErrorResponse, FieldError), common/exception (ErrorCode enum 30+ codes, MindBridgeException, ResourceNotFoundException), common/handler (GlobalExceptionHandler với 10 handler), common/util (RequestContext), common/filter (LoggingRequestContextFilter), auth/dto (RegisterRequest, LoginRequest, AuthResponse), user/dto (UserResponse), consent/dto (3 DTOs), chat/dto (4 DTOs), checkin/dto (4 DTOs). Đã thêm mapstruct 1.5.5.Final + annotation processor vào pom.xml. Đã thêm spring.mvc.throw-exception-if-no-handler-found vào application.yml + application-test.yml. Đã sửa 3 YAML duplicate key bug. Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (30.62s, 1/1 test pass), DatabaseContextSmokeTest PASS, HikariPool-1 Start completed. DoD §4.1 (validation error structure) PASS, §4.2 (no raw entity return) verified by code review, §4.3 deferred sang G1-T10 (swagger). |
| G1-T06 | Đăng ký, đăng nhập và JWT | UNASSIGNED | 2026-07-30 | feature/G1-T06-jwt | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 14 file Java: User entity, UserRepository, UserMapper, DuplicateEmailException, JwtService (HS256, 1h expiry), AuthService (register + login, BCrypt), JwtAuthenticationFilter, SecurityConfig (stateless), AuthController, UserController. Đã thêm spring-boot-starter-security + jjwt 0.12.6 vào pom.xml. Đã cấu hình jwt.secret + jwt.access-token-expiration-ms trong application.yml + application-test.yml (test secret cố định). Đã tạo H2 test schema (schema-users.sql). Đã viết 13 integration test (register, login, 409 duplicate email, 401 wrong password, 401 no token, 401 invalid token, 401 expired token, GET /users/me, password leak prevention ×3). Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (32.28s, 14/14 tests pass: 13 AuthIntegrationTest + 1 DatabaseContextSmokeTest). Security: BCrypt (10 rounds), JWT secret từ env var, passwordHash không bao giờ exposed, login failure không phân biệt bad password vs user not found. |
| G1-T07 | Phân quyền USER, EXPERT và ADMIN | UNASSIGNED | 2026-07-30 | feature/G1-T07-authorization | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 4 file Java mới: common/service/CurrentUserService (helper getCurrentUserId/getCurrentUserRole/verifyOwnership), common/exception/AccessDeniedException (403). Đã sửa SecurityConfig (thêm @EnableMethodSecurity + JSON 403 handler) và GlobalExceptionHandler (map ACCESS_DENIED → 403). Đã sửa UserController để dùng CurrentUserService thay vì @AuthenticationPrincipal. Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (51.43s, 25/25 tests pass: 13 AuthIntegrationTest + 4 AuthorizationIntegrationTest + 8 CurrentUserServiceTest + 1 DatabaseContextSmokeTest, chia 9 nested unit test cho CurrentUserService). Security: role check + ownership check tách biệt, ownership dựa trên JWT principal (không tin client userId), EXPERT giống USER cho đến khi có task riêng về expert permissions (đã ghi trong plan). |
| G1-T08 | Quản lý consent dạng lịch sử | UNASSIGNED | 2026-07-30 | feature/G1-T08-consent-history | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 10 file Java mới: consent/domain/ConsentEvent (JPA entity, append-only — không có setter, chỉ factory `record()`), consent/domain/enums/ConsentType + ConsentAction (CHAT_ANALYSIS, PERSONALIZATION, EXPERT_SHARING × GRANTED, REVOKED), consent/repository/ConsentEventRepository (PostgreSQL DISTINCT ON cho latest-per-type + findLatestByUserAndType), consent/mapper/ConsentEventMapper, consent/service/ConsentService (recordConsent + getCurrentConsentStates), consent/service/ConsentGuard (requireChatAnalysisConsent / hasChatAnalysisConsent / tương tự cho Personalization + ExpertSharing), consent/controller/ConsentController (POST /consents + GET /consents/current), consent/exception/ConsentRequiredException (409 Conflict), consent/ConsentIntegrationTest (7 tests), consent/service/ConsentGuardTest (8 tests), test/resources/schema-consent.sql. Đã sửa: GlobalExceptionHandler (CONSENT_REQUIRED → 409). Đã consolidate enums trong 3 DTO (ConsentEventRequest/Response, CurrentConsentResponse) dùng enums từ domain layer. Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (50.20s, 40/40 tests pass: 13 AuthIntegrationTest + 4 AuthorizationIntegrationTest + 8 CurrentUserServiceTest + 7 ConsentIntegrationTest + 8 ConsentGuardTest + 1 DatabaseContextSmokeTest, chia 8 nested test trong ConsentGuardTest verify latest-wins behavior). Không tạo migration mới (V3 đã có từ G1-T04). Security: userId lấy từ JWT principal (không tin client), consent_events append-only ở cả code level (no setter) và DB level (CHECK constraints), ownership test verify user B không thấy consent của user A. |
| G1-T09 | Audit cơ bản và request tracing | UNASSIGNED | 2026-07-30 | feature/G1-T09-audit-tracing | Phase 2 + 3 review PASS (2026-07-30). Đã tạo 10 file Java mới: common/audit/AuditActorType + AuditCategory + AuditActions (enum + constants cho LOGIN_FAILED/CONSENT_GRANTED/CONSENT_REVOKED/ROLE_CHANGED/ADMIN_ACTION), common/audit/LogSanitizer (SHA-256 hex cho email — lower+trim trước khi hash), common/audit/AuditService (TransactionTemplate REQUIRES_NEW, try/catch swallow persistence errors để audit failures không break request), common/domain/entity/AuditLog (JPA entity, append-only, factory `create()` — không có setter cho mutable fields), common/repository/AuditLogRepository (JpaRepository + findByRequestId), common/filter/LoggingRequestContextFilterTest (3 tests), common/audit/LogSanitizerTest (3 tests), common/audit/AuditIntegrationTest (5 tests), test/resources/schema-audit.sql. Đã sửa: common/filter/LoggingRequestContextFilter (set response header `X-Request-Id` echo MDC value), logback-spring.xml pattern thêm `[%X{requestId:-}]`, auth/service/AuthService (login fail bất kỳ lý do nào đều ghi audit với emailHash — không plain), consent/service/ConsentService (ghi audit sau khi save event với action CONSENT_GRANTED/CONSENT_REVOKED), auth/AuthIntegrationTestBase + auth/AuthorizationIntegrationTest (thêm schema-audit.sql vào @Sql BEFORE_TEST_CLASS). Đã tạo docs/LOGGING.md (4 sections: application logs / request tracing / audit_logs / operator runbook, có ghi rõ retention 7 ngày cho app log và KHÔNG tự xóa audit_logs). Không tạo migration Flyway mới (bảng audit_logs đã có ở V3, chỉ thêm index `idx_audit_logs_request_id` trong schema-audit.sql cho test parity). Verify: `./mvnw.cmd -B clean test` BUILD SUCCESS (54.63s, 51/51 tests pass: 13 AuthIntegrationTest + 4 AuthorizationIntegrationTest + 8 CurrentUserServiceTest + 7 ConsentIntegrationTest + 8 ConsentGuardTest + 5 AuditIntegrationTest + 3 LogSanitizerTest + 3 LoggingRequestContextFilterTest + 1 DatabaseContextSmokeTest). Security: AuditService dùng TransactionTemplate REQUIRES_NEW riêng biệt + try/catch ở ngoài đảm bảo audit failures không cascade thành 500 cho user; email chỉ lưu SHA-256 hash (case-insensitive) — verify trong test `loginFailure_auditRow` rằng metadata KHÔNG chứa email plain text; logback pattern chỉ đẩy ra MDC keys (requestId/path), KHÔNG in payload body. |
| G1-T10 | Health check, Swagger, CORS và kết nối frontend | UNASSIGNED | 2026-07-30 | feature/G1-T10-frontend-integration | Phase 2 + 3 review PASS (2026-07-30). **Backend**: tạo 5 file Java mới — common/dto/HealthResponse (record khớp OpenAPI schema), common/controller/HealthController (`GET /api/v1/health`, permitAll, custom body khác với actuator), common/config/CorsConfig (WebMvcConfigurer, đọc `mindbridge.cors.allowed-origins` từ profile, KHÔNG bao giờ wildcard cho prod), common/config/OpenApiConfig (OpenAPI bean, title + version + JWT bearer scheme tên `bearerAuth` cho Swagger UI Authorize), common/controller/HealthControllerTest (3 tests). Đã sửa: pom.xml (thêm springdoc-openapi-starter-webmvc-ui 2.6.0), application.yml (springdoc.api-docs.path=/api/v1/v3/api-docs + springdoc.swagger-ui.path=/api/v1/swagger-ui.html + mindbridge.cors.allowed-origins mặc định []), application-local.yml (allowed-origins=http://localhost:5173 cho Vite), application-prod.yml (allowed-origins đọc từ MIND_BRIDGE_CORS_ORIGINS env var — empty = fail-safe = no CORS), common/config/SecurityConfig (thêm permitAll cho `/health`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`). **Frontend**: tạo 9 file mới — frontend/.env.example (`VITE_API_BASE_URL=http://localhost:8080/api/v1`), frontend/eslint.config.js (flat config v9 dùng typescript-eslint), src/vite-env.d.ts (export Interface ImportMetaEnv cho VITE_API_BASE_URL), src/api/client.ts (ApiClient class + ApiError, base URL từ env, Authorization bearer header, 401 hook, X-Request-Id echo), src/api/auth.ts (RegisterRequest/LoginRequest/AuthResponse/UserResponse DTOs + AuthApi wrapper), src/api/consents.ts (ConsentsApi wrapper), src/auth/AuthContext.tsx (Provider + useAuth hook + localStorage rehydrate + auto refresh `/users/me` khi mount), src/auth/constants.ts (chuyển REQUEST_ID_HEADER_NAME export ra để HMR-friendly), src/pages/AuthPage.tsx (minimal UI 2 tab Sign in/Register, loading + 401/409/validation errors, hiển thị requestId cho debug). Đã sửa: src/main.tsx (wrap AuthProvider quanh App), src/App.tsx (thêm `/auth` route + import AuthPage), src/components/landing/LandingNav.tsx (thêm "Sign in" link cạnh "Start Free" ở desktop + mobile menu). Verify Backend: `./mvnw.cmd -B clean test` BUILD SUCCESS (64s, 54/54 tests pass: 13 AuthIntegrationTest + 4 AuthorizationIntegrationTest + 8 CurrentUserServiceTest + 7 ConsentIntegrationTest + 8 ConsentGuardTest + 5 AuditIntegrationTest + 3 LogSanitizerTest + 3 LoggingRequestContextFilterTest + 3 HealthControllerTest + 1 DatabaseContextSmokeTest). Verify Frontend: `npm install` 272 packages OK; `npm run lint` 0 errors, 2 pre-existing warnings (BreathingOrb.tsx + AuthContext fast-refresh — pre-existing prototype issue ngoài scope); `npm run build` BUILD SUCCESS (6.43s, dist: 433KB JS / 39KB CSS). Auth flow end-to-end: AuthPage → ApiClient → http://localhost:8080/api/v1/{auth/register,auth/login,users/me,consents} → SecurityConfig → JwtAuthenticationFilter → AuthService → `audit_logs` row ghi bởi AuditService từ G1-T09. Vite dev server sẽ proxy ngược lại backend (VITE_API_BASE_URL đã trỏ đúng /api/v1). Security: CORS config per-profile (dev localhost:5173, prod env var), no wildcard ở prod; Swagger UI permitAll nhưng yêu cầu bearer token cho protected endpoint qua "Authorize" button; ApiClient KHÔNG log token; consent page (chưa có UI riêng, nhưng ConsentsApi đã ready cho G6/G7 sử dụng). **G1 hoàn thành 10/10 MUST tasks — G2 (Chat, Daily Check-in) có thể bắt đầu**: backend foundation + auth + consent + audit + tracing + health + swagger + CORS + frontend integration (vertical slice Auth/Consent) đều đã PASS. |
| — | — | — | — | — | — |

Ví dụ:

| Task ID | Task Name | Owner | Started Date | Branch | Current State |
|---|---|---|---|---|---|
| G1-T04 | Implement JWT authentication | Dev A | 2026-08-03 | feature/G1-T04-jwt | Login works, ownership test pending |

---

# 8. Ready Tasks

Các task có thể bắt đầu ngay (sau khi G1-T01 + G1-T02 merge vào develop).

| Priority | Task ID | Task Name | Dependencies |
|---|---|---|---|---|
| MUST | G1-T08 | Quản lý consent dạng lịch sử | G1-T06 |
| MUST | G1-T09 | Audit cơ bản và request tracing | G1-T06 |

Chỉ nên có khoảng 5–10 task ở trạng thái READY.

---

# 9. Blocked Tasks

| Task ID | Task Name | Blocked By | Required Action | Owner |
|---|---|---|---|---|
| G1-T01 (push) | Push feature branch + develop + tag lên origin | Thiếu GitHub credentials (password auth đã bị GitHub vô hiệu hoá từ 2021-08) | Dev cấu hình Personal Access Token (scope `repo`) hoặc SSH key cho remote `origin`, sau đó chạy `git push origin develop && git push origin feature/G1-T01-baseline-standardization && git push origin v0.0.1-frontend-baseline` | UNASSIGNED |
| G1-T02 (push) | Push feature branch lên origin | Thiếu GitHub credentials (cùng block với T01) | Sau khi T01 merge xong, rebase T02 lên develop, rồi push `feature/G1-T02-spring-boot-scaffold`; merge vào develop | UNASSIGNED |

Ví dụ:

| Task ID | Task Name | Blocked By | Required Action | Owner |
|---|---|---|---|---|
| G3-T02 | Real LLM Provider | Chưa có API key | Cấu hình provider và quota | Dev A |

---

# 10. Deferred Tasks

| Task ID or Feature | Reason | Reconsider After |
|---|---|---|
| Multi-model embedding | Không cần cho MVP | Sau RAG MVP |
| Outbox events | Chưa có asynchronous integration | Trước production |
| Maintenance plans | Không ảnh hưởng demo chính | Sau CBT Runtime |

---

# 11. Backend Module Status

| Module | Status | Implemented Features | Missing Features |
|---|---|---|---|
| auth | IMPLEMENTED | Register, Login, JWT | Refresh token |
| user | IMPLEMENTED | User Entity, Current User | Profile update |
| consent | NOT_STARTED | — | Grant, Revoke, Current State |
| chat | NOT_STARTED | — | Session, Message |
| checkin | NOT_STARTED | — | Template, Assignment, Answer |
| analysis | NOT_STARTED | — | Mock Provider, Real Provider |
| safety | NOT_STARTED | — | Risk Resolver, Safety Event |
| behavior | NOT_STARTED | — | Daily Feature, Profile |
| cbt | NOT_STARTED | — | Catalog, Runtime |
| matching | NOT_STARTED | — | Run, Candidate, Decision |
| recommendation | NOT_STARTED | — | Recommendation, Feedback |
| knowledge | DEFERRED | — | RAG |
| audit | NOT_STARTED | — | Basic Audit |

---

# 12. Frontend Status

## Existing Pages

| Page or Route | Exists | Uses Mock Data | Real API Connected | Notes |
|---|---|---|---|---|
| Login | UNKNOWN | UNKNOWN | NO | Audit required |
| Register | UNKNOWN | UNKNOWN | NO | Audit required |
| Chat | YES | YES | NO | Existing UI should be preserved |
| Daily Check-in | UNKNOWN | UNKNOWN | NO | |
| Dashboard | YES | YES | NO | |
| Program Detail | UNKNOWN | UNKNOWN | NO | |
| Exercise | UNKNOWN | UNKNOWN | NO | |
| Admin | UNKNOWN | UNKNOWN | NO | |

Sau audit repository, thay `UNKNOWN` bằng trạng thái thực tế.

## Frontend Infrastructure

| Item | Status | Notes |
|---|---|---|
| Shared API Client | UNKNOWN | |
| Auth Interceptor | UNKNOWN | |
| Protected Routes | UNKNOWN | |
| Error Handler | UNKNOWN | |
| Environment Configuration | UNKNOWN | |
| TypeScript API DTO | UNKNOWN | |
| Mock/Real API Switch | UNKNOWN | |

---

# 13. API Implementation Status

| Endpoint | Method | Backend | Frontend | Test | Notes |
|---|---|---|---|---|---|
| `/api/v1/health` | GET | NOT_STARTED | NOT_NEEDED | NOT_STARTED | |
| `/api/v1/auth/register` | POST | IMPLEMENTED | NOT_CONNECTED | IMPLEMENTED | G1-T06: BCrypt + JWT HS256, 1h expiry, 13 integration tests |
| `/api/v1/auth/login` | POST | IMPLEMENTED | NOT_CONNECTED | IMPLEMENTED | G1-T06: generic error (no email enumeration) |
| `/api/v1/users/me` | GET | IMPLEMENTED | NOT_CONNECTED | IMPLEMENTED | G1-T06: reads userId from JWT principal |
| `/api/v1/consents` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/consents/current` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions/{id}/messages` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/daily-checkins/today` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/behavior/profile` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/matching/run` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/user-programs` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |

Status backend:

```text
NOT_STARTED
PARTIAL
IMPLEMENTED
```

Status frontend:

```text
NOT_CONNECTED
MOCK_ONLY
CONNECTED
```

---

# 14. Database Migration Status

| Migration | Purpose | Task | Status | Applied Locally | Applied Test |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

Ví dụ:

| Migration | Purpose | Task | Status | Applied Locally | Applied Test |
|---|---|---|---|---|---|
| V1__enable_extensions.sql | Enable citext + pgcrypto | G1-T04 | IN_PROGRESS | PENDING | N/A (Flyway disabled for H2) |
| V2__create_users.sql | Create users table | G1-T04 | IN_PROGRESS | PENDING | N/A |
| V3__create_consent_and_audit.sql | Create consent_events + audit_logs tables | G1-T04 | IN_PROGRESS | PENDING | N/A |

Quy tắc:

- Không sửa migration đã ghi `COMPLETED`.
- Migration mới phải có tên mới.
- Hai developer phải tránh trùng version.

---

# 15. Implemented Database Tables

| Table | Migration | Task | Status | Notes |
|---|---|---|---|---|
| users | V2__create_users.sql | G1-T04 | IN_PROGRESS | |
| consent_events | V3__create_consent_and_audit.sql | G1-T04 | IN_PROGRESS | |
| audit_logs | V3__create_consent_and_audit.sql | G1-T04 | IN_PROGRESS | |
| chat_sessions | — | — | NOT_STARTED | |
| conversation_messages | — | — | NOT_STARTED | |
| daily_question_templates | — | — | NOT_STARTED | |
| daily_question_options | — | — | NOT_STARTED | |
| daily_question_assignments | — | — | NOT_STARTED | |
| daily_question_answers | — | — | NOT_STARTED | |
| behavioral_events | — | — | NOT_STARTED | |
| ai_analysis_runs | — | — | NOT_STARTED | |
| chat_analysis_results | — | — | NOT_STARTED | |
| risk_state_history | — | — | NOT_STARTED | |
| safety_events | — | — | NOT_STARTED | |
| safety_event_sources | — | — | NOT_STARTED | |
| user_daily_features | — | — | NOT_STARTED | |
| user_behavior_profiles | — | — | NOT_STARTED | |
| user_behavior_profile_snapshots | — | — | NOT_STARTED | |

Không cần liệt kê toàn bộ bảng chưa gần triển khai. Có thể bổ sung theo từng group.

---

# 16. AI Provider Status

| Item | Status | Notes |
|---|---|---|
| ChatAnalysisProvider Interface | NOT_STARTED | |
| MockChatAnalysisProvider | NOT_STARTED | |
| RealLlmChatAnalysisProvider | NOT_STARTED | |
| Structured Output Schema | NOT_STARTED | |
| JSON Validation | NOT_STARTED | |
| Timeout | NOT_STARTED | |
| Retry | NOT_STARTED | |
| Error Persistence | NOT_STARTED | |
| Token Usage Tracking | NOT_STARTED | |

Current Provider:

```text
NONE
```

Environment Variable Names:

```text
TODO
```

Không ghi API key thật vào file này.

---

# 17. Safety Status

| Feature | Status | Notes |
|---|---|---|
| Keyword/Regex Pre-filter | NOT_STARTED | |
| LLM Risk Classification | NOT_STARTED | |
| Final Risk Resolver | NOT_STARTED | |
| Risk State History | NOT_STARTED | |
| Safety Event | NOT_STARTED | |
| Safety Event Source | NOT_STARTED | |
| Level 3 Matching Block | NOT_STARTED | |
| Level 4 Fixed Response | NOT_STARTED | |
| Expert Review | NOT_STARTED | |

Approved values still missing:

```text
- Production keyword list
- Production thresholds
- Level 4 response text
- Expert escalation workflow
```

Status:

```text
TODO_EXPERT_REVIEW
```

---

# 18. Behavior Analysis Status

| Feature | Status | Calculation Version | Notes |
|---|---|---|---|
| stress_score | NOT_STARTED | — | |
| mood_score | NOT_STARTED | — | |
| energy_score | NOT_STARTED | — | |
| sleep_hours | NOT_STARTED | — | |
| anxiety_signal | NOT_STARTED | — | |
| engagement_score | NOT_STARTED | — | |
| exercise_completion_rate | NOT_STARTED | — | |
| max_risk_level | NOT_STARTED | — | |
| 7-day average | NOT_STARTED | — | |
| 30-day average | NOT_STARTED | — | |
| trend | NOT_STARTED | — | |
| coverage | NOT_STARTED | — | |
| confidence | NOT_STARTED | — | |
| profile snapshot | NOT_STARTED | — | |

---

# 19. CBT Status

## CBT Catalog

| Component | Status | Notes |
|---|---|---|
| Program Logical Entity | NOT_STARTED | |
| Program Version | NOT_STARTED | |
| Module Logical Entity | NOT_STARTED | |
| Module Version | NOT_STARTED | |
| Exercise Logical Entity | NOT_STARTED | |
| Exercise Version | NOT_STARTED | |
| Version Immutability Test | NOT_STARTED | |

## Programs

| Program Code | Content Status | Database Status | Notes |
|---|---|---|---|
| CBT-STRESS | DEMO_ONLY | NOT_STARTED | |
| CBT-MOOD | DEMO_ONLY | NOT_STARTED | |
| CBT-WORRY | DEMO_ONLY | NOT_STARTED | |
| CBT-SLEEP | DEMO_ONLY | NOT_STARTED | |

## CBT Runtime

| Component | Status |
|---|---|
| User Program | NOT_STARTED |
| Program State Transition | NOT_STARTED |
| Module Progress | NOT_STARTED |
| Exercise Assignment | NOT_STARTED |
| Exercise Submission | NOT_STARTED |
| Baseline Assessment | NOT_STARTED |
| Weekly Assessment | NOT_STARTED |
| Final Assessment | NOT_STARTED |

---

# 20. Program Matching Status

| Feature | Status | Notes |
|---|---|---|
| Safety Gate | NOT_STARTED | |
| Matching Run | NOT_STARTED | |
| Eligibility Check | NOT_STARTED | |
| Exclusion Check | NOT_STARTED | |
| Candidate Score | NOT_STARTED | |
| Candidate Persistence | NOT_STARTED | |
| Matching Decision | NOT_STARTED | |
| Reason Codes | NOT_STARTED | |
| User Confirmation | NOT_STARTED | |

Current rule version:

```text
NONE
```

Current threshold bundle:

```text
NONE
```

Demo matching configuration:

```text
NOT_CREATED
```

---

# 21. Testing Status

| Test Area | Status | Latest Result |
|---|---|---|
| Backend Build | PASS | `.\mvnw.cmd clean compile` BUILD SUCCESS (9.96s, 1 source file + 5 resource) after Maven 3.9.16 downloaded via Wrapper 3.3.4 |
| Backend Unit Tests | PASS | `.\mvnw.cmd clean test` (G1-T06): 13/13 AuthIntegrationTest + 1/1 DatabaseContextSmokeTest = 14 tests |
| Backend Integration Tests | PASS | `.\mvnw.cmd clean test` (G1-T06): 14/14 tests pass (32.28s) |
| Flyway Migration Test | NOT_RUN | |
| Frontend Type Check | NOT_RUN | |
| Frontend Build | NOT_RUN | `npm run build` chưa chạy được vì `frontend/node_modules/` thiếu (chưa `npm install` từ session trước). KHÔNG phải do G1-T01/T02 gây ra | |
| End-to-End Test | NOT_RUN | |
| Safety Tests | NOT_RUN | |
| CBT State Tests | NOT_RUN | |
| Matching Tests | NOT_RUN | |

**G1-T02 verification (local, 2026-07-30)**:

```text
.\mvnw.cmd clean compile       -- BUILD SUCCESS (9.96s)
.\mvnw.cmd spring-boot:run     -- Started MindBridgeApplication in 2.84s, profile=local
GET /api/v1/actuator/health    -- 200 {"status":"UP"}
GET /api/v1/nonexistent        -- 404 (no stack trace, no SQL leaking)
```

Không ghi `PASS` nếu không có command đã chạy.

---

# 22. Deployment Status

| Component | Status | Environment |
|---|---|---|
| Backend | NOT_DEPLOYED | |
| Frontend | NOT_DEPLOYED | |
| PostgreSQL | NOT_DEPLOYED | |
| LLM Configuration | NOT_CONFIGURED | |
| Health Check | NOT_IMPLEMENTED | |
| Seed Data | NOT_CREATED | |

---

# 23. Known Issues

| ID | Severity | Description | Owner | Status |
|---|---|---|---|---|
| ISSUE-003 | LOW | G1-T01 push origin thất bại do remote `origin` chưa có credentials (GitHub bỏ password auth từ 2021) | UNASSIGNED | OPEN |

Severity:

```text
BLOCKING
HIGH
MEDIUM
LOW
```

Ví dụ:

| ID | Severity | Description | Owner | Status |
|---|---|---|---|---|
| ISSUE-001 | HIGH | Frontend ChatPage vẫn sử dụng mock data | Dev B | OPEN |
| ISSUE-002 | MEDIUM | Chưa có refresh token | Dev A | DEFERRED |

---

# 24. Technical Decisions

Ghi lại các quyết định quan trọng đã chốt.

| Decision ID | Decision | Date | Reason |
|---|---|---|---|
| ADR-001 | Use Modular Monolith | YYYY-MM-DD | Team size 2 |
| ADR-002 | Use hosted LLM, no training | YYYY-MM-DD | Reduce complexity |
| ADR-003 | Use immutable CBT versions | YYYY-MM-DD | Preserve historical content |

---

# 25. Environment and Setup Notes

## Backend

```text
Java version:       21.0.10 (LTS, Adoptium-style)
Spring Boot version: 3.3.5
Maven version:      3.9.16 (via Maven Wrapper 3.3.4)
Backend port:       8080 with context-path /api/v1
```

## Frontend

```text
Node version:
Package manager:
Frontend port:
```

## Database

```text
PostgreSQL version: 17.10 (native install, Windows service postgresql-x64-17, port 5432)
Database name: mindbridge_dev (created by G1-T03 manual SQL; see backend/README.md § "Database setup")
Database user: mindbridge_app (least-privilege role; connects with GRANT SELECT/INSERT/UPDATE/DELETE on public)
Required extensions: (filled by G1-T04 — Flyway + citext/pgcrypto)
Driver: org.postgresql 42.7.x (BOM-managed via spring-boot-dependencies 3.3.5)
Test DB: H2 in-memory `MODE=PostgreSQL` (scope runtime, profile `test` only)
```

## Required Environment Variables

Biến môi trường chuẩn hoá bởi G1-T01 (xem [`.env.example`](../.env.example)). Tên biến không đổi cho tới khi task tương ứng sửa.

```text
DATABASE_URL          # dùng từ G1-T03
DATABASE_USERNAME     # dùng từ G1-T03
DATABASE_PASSWORD     # dùng từ G1-T03
JWT_SECRET            # dùng từ G1-T06
JWT_EXPIRATION        # dùng từ G1-T06
APP_CORS_ALLOWED_ORIGINS  # dùng từ G1-T10
LLM_PROVIDER          # dùng từ G3
LLM_API_KEY           # dùng từ G3 (CHƯA thêm vào .env.example — sẽ làm khi G3 bắt đầu)
```

Không ghi giá trị secret thật.

---

# 26. Next Recommended Tasks

Thứ tự task tiếp theo:

Thứ tự task tiếp theo (sau khi G1-T01...G1-T06 hoàn thành):

1. `G1-T07 — Phân quyền USER, EXPERT và ADMIN`
2. `G1-T08 — Quản lý consent dạng lịch sử`
3. `G1-T09 — Audit cơ bản và request tracing`
4. `G1-T10 — Health check, Swagger, CORS và kết nối frontend`

Lý do:

```text
G1-T01 chuẩn hoá repo (tag, branches, .gitignore, .env.example, README, CONTRIBUTING, docs/git-workflow.md) — đã hoàn thành local, đang chờ push origin.
G1-T02 là task backend đầu tiên phụ thuộc G1-T01 (cần branch develop, .env.example). G1-T03 song song có thể làm sau G1-T01.
G1-T04 + G1-T05 tiếp tục xây nền tảng Spring Boot trước khi G1-T06 (JWT auth).
```

Ví dụ:

```text
1. G1-T01 — Audit current repository
2. G1-T02 — Configure backend and database
3. G1-T03 — Implement user domain
4. G1-T04 — Implement authentication
```

---

# 27. Update Checklist

Sau mỗi Pull Request:

- [ ] Cập nhật Completed Tasks.
- [ ] Xóa task khỏi In Progress.
- [ ] Cập nhật Group Status.
- [ ] Cập nhật API Status.
- [ ] Cập nhật Migration Status.
- [ ] Cập nhật Database Table Status.
- [ ] Cập nhật Known Issues.
- [ ] Ghi command test thực tế.
- [ ] Cập nhật Last Updated.
- [ ] Cập nhật Next Recommended Tasks.

---

# 28. Cursor Usage Note

Trước mỗi task, Cursor phải đọc:

```text
docs/05_IMPLEMENTATION_STATUS.md
```

Cursor phải:

1. Kiểm tra trạng thái source hiện tại.
2. Không tạo lại chức năng đã completed.
3. Không giả định migration chưa có.
4. Báo nếu file status không khớp source.
5. Ưu tiên source code và migration thực tế khi phát hiện status đã lỗi thời.
6. Cập nhật file này sau khi task hoàn thành và được xác nhận.

File này không thay thế việc đọc source code.
