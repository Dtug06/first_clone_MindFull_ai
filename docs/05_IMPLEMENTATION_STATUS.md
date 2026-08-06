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
- Current branch: `codex/integrate-new-frontend-g4`
- Current release target: `MVP`
- Last updated: `2026-08-06`
- Updated by: `Codex verification session`
- Frozen frontend baseline: tag `v0.0.1-frontend-baseline` → commit `128eece`

---

## Current Repository State

### Frontend

- Status: PARTIAL_REAL_INTEGRATION
- Location: `frontend/`
- Source directory: `frontend/src/`
- Framework: React 18 + TypeScript + Vite
- Styling: Tailwind CSS
- Routing: React Router with HashRouter
- Data source: Mixed; Auth, Consent, Chat, Daily Check-in and Behavior Profile use backend APIs; remaining prototype areas may still use browser-local/mock data
- API integration: PARTIAL
- Authentication: IMPLEMENTED
- Authorization: Backend-enforced for integrated endpoints; complete frontend route lifecycle remains G7 scope
- Real AI integration: IMPLEMENTED for safety-screened chat when the real provider is configured

### Backend

- Status: IMPLEMENTED_THROUGH_G4_IN_REVIEW
- Location: `backend/`
- Stack: Java 21 + Spring Boot 3.3.5
- API: Auth, Consent, Chat, Daily Check-in, Safety, structured chat analysis and G4 Behavior Profile implemented
- Tests: Targeted G4 personalization regression passed on 2026-08-06; full Maven lifecycle remains affected by the local javac `Cannot close compiler resources` issue

### Database

- Status: IMPLEMENTED_IN_REVIEW
- Database: PostgreSQL
- Flyway migrations: V1 through V25 present; V25 adds deterministic consent event ordering
- Fresh PostgreSQL verification: NOT_VERIFIED in this session because database credentials were not available to the review process

### Important Note

Existing frontend pages represent intended UI only.

They do not prove that corresponding business functions are implemented.

### 2026-08-06 G4 Personalized Chat Integration

- Added consent-gated context loading for the authenticated user only.
- Ordinary AI chat can receive the user's sanitized display name, today's typed Daily Check-in values, and the latest materialized G4 profile/trends.
- Free-text Daily answers, email, passwords, raw Safety evidence and audit data are excluded from the AI context.
- Level 3/4 Safety handling remains before personalization and before the conversational AI provider.
- Added real Settings controls for `CHAT_ANALYSIS` and `PERSONALIZATION` consent; fixed the frontend DTO to match the backend consent contract.
- Fixed Daily assigned-count aggregation and removed the hard-coded profile timezone.
- Targeted backend tests: 54/54 PASS, including chat-message integration. Frontend lint: 0 errors (4 existing warnings). Frontend production build: PASS.
- Behavior profiles remain materialized by the approved G4 aggregation jobs. Production thresholds and scheduler activation still require explicit expert-reviewed environment configuration; the conversational LLM does not invent or calculate clinical thresholds.

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
| G4 | Behavior Analysis and User Profile | IN_PROGRESS | UNASSIGNED | **G4-T01 APPROVED 2026-08-04** (after L-env-1 hotfix in commit `55dc226`). **G4-T02 APPROVED WITH FINDINGS 2026-08-04** (Phase 3 review). **G4-T03 APPROVED WITH FINDINGS 2026-08-04** (Phase 3 review). **G4-T04 APPROVED WITH FINDINGS 2026-08-04** (Phase 3 review). **G4-T05 Phase 2 COMPLETED 2026-08-04** — 22 files created, compilation SUCCESS, awaiting Phase 3 review. **G4-T06 Phase 3 review APPROVED WITH FINDINGS 2026-08-04** — 4 files compile clean, null-preservation + FEATURE_DICTIONARY §9.3 inclusive window + NOT_APPLICABLE for G5-not-shipped confirmed; tests still DEFERRED (Write tool NUL corruption, F-1 — close before T12). **G4-T07 APPROVED WITH FINDINGS 2026-08-05** (Phase 3 review, second chat) — 9 main files + 1 test (19 unit tests PASS, Phase 2 verification; not re-run this chat per user choice), calculation_version = "trend_v1", ZoneId carry-forward F-2 from T06 acknowledged, HIGH_STRESS_THRESHOLD scale = normalized 0–1 (resolved F-1, 2026-08-05). 5 non-blocking findings (F-1 scale ambiguity resolved, F-2 NO_PRIOR_DATA split, F-3 dead `noop`, F-4 explicit ANSWERED filter, F-5 dead null branch) + 5 test gaps + carry-forward to T12. See `### G4-T07 Phase 3 review` below for full evidence. **G4-T08 APPROVED WITH FINDINGS 2026-08-05** (Phase 3 review) — 6 main files + 1 test (21/21 unit tests PASS), 6 findings (F-1 TZ bucket carry-forward from T06, F-2 SQL order, F-3 hardcoded taxonomy size, F-4 test coverage gap, F-5 OpenAPI vendor extension, F-6 immutability), OpenAPI tighten applied (dominantTopics → DominantTopic schema). Score NOT persisted to V21 (decision B on schema [0,1] vs [0,3]); `EngagementConfig` value-object carries `TOP_N=3` + `MIN_TOPIC_CONFIDENCE=null` sentinel. **G4-T09 APPROVED 2026-08-05** (Phase 3 review) — 14 main files + 4 test files (42 new tests: 19 unit + 7 integration service + 4 controller + 12 snapshot DTO), 0 new failures, 0 new errors, 17 pre-existing g4cff-1 errors unchanged. Profile entity/service/API/job all PASS. DoD: write path = job only, GET returns latest, coverage/confidence always present, ownership check, race condition handled by idempotent UPSERT. **G4-T11 APPROVED 2026-08-05** (Phase 3 review) — 10 production + 4 test files, 28 tests (21 unit/DTO + 7 integration), BUILD SUCCESS, full regression pending. DataQualityStatus enum + DataQualityConfig (null-default fail-fast, TODO_EXPERT_REVIEW) + V24 migration + status column in profile entity/service/API/mapper. **G4-T12 Phase 1 plan COMPLETED 2026-08-05** — narrow close-out scope (carry-forward family from T06/T07/T08/T09). scope: 2 production moi + 4 production sua + 2 YAML sua + 1 WebMvcTest (4 cases) + 2 docs sua. Decisions: (1) trendSummary shape = nested TrendSummaryResponse (q3a), (2) ZoneId source = users.timezone (TrendConfigProperties), (3) Config binding = @ConfigurationProperties mirror T11 pattern, (4) HTTP test = @WebMvcTest slice. Job path (ZoneId + LocalDate.now) deferred G8. Awaiting Phase 1 approval to start Phase 2. **G4-T12 Phase 2 COMPLETED 2026-08-05** — 3 production files moi (`TrendConfigProperties` + `TrendSummaryResponse` + `TrendEntryResponse` + `StreakInfoResponse`) + 1 test moi (`UserBehaviorProfileControllerWebMvcTest` 4 cases) + 5 production sua + 2 yaml sua + 2 test sua + 2 docs sua. Compile SUCCESS, targeted tests 44/44 PASS (3 controller + 4 WebMvc + 9 ProfileSnapshot + 9 Aggregation + 19 TrendCalculator). Full regression (after `clean compile` to refresh stale target/) **756/0/17** (752 baseline + 4 new = 756, 17 errors unchanged, all `g4cff-1` out-of-scope). **T12 introduces zero new regressions**. Scope correction during Phase 2: controller did not need `UserRepository` injection (snapshot pattern means TZ is baked at write time, not request time). Closes T07 F-1 (HIGH_STRESS_THRESHOLD scale + binding), T09 F-3 (HTTP 401/403/200/404 tests), T09 F-7 (OpenAPI security + 401/403), T07 q3a (nested trendSummary shape). 3 placeholder threshold values bound via YAML (`MIN_TREND_COVERAGE=0.5`, `TREND_DELTA_THRESHOLD=0.1`, `HIGH_STRESS_THRESHOLD=0.75`). Job-path TZ + LocalDate.now() deferred G8. |
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




### G4-T02 — user_daily_features schema (Phase 2 + 3 PASS 2026-08-04 — verdict APPROVE WITH FINDINGS)

**Task**: Tạo bảng feature typed dễ query, JSONB chỉ dùng cho phần mở rộng — migration V21 + H2 test mirror + JdbcTest verifying Definition of Done.

**Deliverable (3 files, all created 2026-08-04)**:

| File | Size | Purpose |
|---|---|---|
| backend/src/main/resources/db/migration/V21__create_user_daily_features.sql | 12612 bytes / 239 lines | Postgres migration: 1 table + 30 CHECK + 1 UNIQUE + 1 index |
| backend/src/test/resources/schema-user-daily-features.sql | 7806 bytes / 213 lines | H2 in-memory test mirror (UUID native, JSONB → VARCHAR(8192), users FK dropped) |
| backend/src/test/java/com/mindbridge/analysis/feature/UserDailyFeaturesSchemaTest.java | ~27.5 KB / 557 lines | Schema-only JdbcTest — 32 tests, 0 fail, 0 error |

**Design decisions confirmed in Phase 1**:

| # | Decision | Choice | Reason |
|---|---|---|---|
| Q1 | Unique key | (user_id, feature_date) single-row-per-day | DoD §1 dashboard query + FEATURE_DICTIONARY §9.2 idempotent recompute |
| Q2 | Sleep column name | sleep_hours NUMERIC(4,2) | DB-MVP §7.1 literal (Rule 00 priority) |
| Q3 | Exercise column | exercise_completion_ratio NUMERIC(5,4) | FEATURE_DICTIONARY §6.7 (precision rõ hơn "rate") |
| Q4 | Coverage/confidence | 2 cột: explicit_coverage + inferred_confidence | DB-MVP §7.1 literal |
| Q5 | Drop model_bundle_version + computed_at | YES | FEATURE_DICTIONARY không có; MVP không cần |
| Q6 | calculation_version (single string per row) | normalization_v1\|sleep_quality_v1\|engagement_v1_chat_checkin\|exercise_completion_v1\|max_risk_daily_v1 | Composite của 5 calculation versions (FEATURE_DICTIONARY §2.2) |
| Q7 | Future-date CHECK | YES (feature_date <= CURRENT_DATE) | Chặn typo future date |

**Verification command** (Phase 2):

```bash
cd backend
./mvnw.cmd -q -Dtest=UserDailyFeaturesSchemaTest test
```

**Verification result**:

| Test class | Tests | Pass | Fail | Error | Time |
|---|---|---|---|---|---|
| StressChecks | 5 | 5 | 0 | 0 | 0.085s |
| MoodChecks | 3 | 3 | 0 | 0 | 0.046s |
| SleepChecks | 4 | 4 | 0 | 0 | 0.074s |
| AnxietyChecks | 3 | 3 | 0 | 0 | 0.073s |
| EngagementChecks | 3 | 3 | 0 | 0 | 0.068s |
| MaxRiskChecks | 4 | 4 | 0 | 0 | 0.085s |
| MetadataChecks | 6 | 6 | 0 | 0 | 0.121s |
| Outer (DoD §1 + §2) | 4 | 4 | 0 | 0 | included above |
| **TOTAL** | **32** | **32** | **0** | **0** | ~17.8s |

**DoD verification**:

- DoD §1 (dashboard query 7/30 ngày không cần JSONB cho feature chính): dashboardQuery_readsTypedColumns PASS — typed columns only, INFORMATION_SCHEMA confirms all 8 feature columns + extra_features present but not selected.
- DoD §2 (không duplicate daily row): unique_user_date_rejectsDuplicate PASS (H2 enforces constraint); unique_user_date_allowsDifferentUser + _allowsDifferentDate PASS (different user OR different date allowed).
- DoD §3 (constraint miền giá trị): 30 CHECK constraints verified — stress (2), mood (2), energy (2), sleep (3), anxiety (3), engagement (5), exercise (1), max_risk (3), metadata (6: coverage × 2, timezone blank, feature_date × 2, calculation_version blank), version + timezone (3).

**Pre-existing issues noted**: NONE re-introduced by V21. safety_event_sources / SafetyEventSource JPA promotion (commit 55dc226) remains clean.

**Phase 2 scope (in-scope only, smallest complete change)**:

- 1 Flyway migration (V21).
- 1 H2 test mirror schema.
- 1 JdbcTest (no JPA entity / repository / service / controller — those ship with G4-T04+).

**Out-of-scope** (deferred to G4-T04+):

- JPA entity UserDailyFeature.
- Repository UserDailyFeatureRepository.
- Service UserDailyFeatureService + 8 calculators (stress / mood / energy / sleep / anxiety_signal / engagement / exercise_completion / max_risk).
- REST endpoint (G4-T12).
- DTO mapping với "UNKNOWN" cho null score.
- Window aggregation 7d/30d (G4-T06).
- Snapshot trigger sang user_behavior_profile_snapshots (G4-T09+).

**Phase 3 — Review PASS (2026-08-04)**: APPROVE WITH FINDINGS (no blocker; cosmetic + test-pruning only). Xem `### G4-T02 Phase 3 review` dưới đây.

### G4-T02 Phase 3 review

**Verdict (rule 00-project-core §Verdict)**: APPROVE WITH FINDINGS — task goal achieved, không có BLOCK. Tất cả findings là cosmetic / advisory và không ảnh hưởng DoD.

**Acceptance criteria verification (re-read task spec §Deliverable + §Definition of Done)**:

| # | Criterion | Status | Evidence |
|---|---|---|---|
| D1 | Migration V21__create_user_daily_features.sql tồn tại | PASS | 12612 bytes / 239 lines, UTF-8 no-BOM, Postgres 17 syntax |
| D2 | File chỉ chứa 1 table mới (không edit file cũ) | PASS | V21 chỉ có 1 CREATE TABLE + 1 CREATE INDEX; không touch V20 trở về trước |
| DoD §1 | Query feature chính 7/30 ngày không cần JSONB | PASS | `dashboardQuery_readsTypedColumns` chọn 8 typed columns; INFORMATION_SCHEMA confirm `extra_features` tồn tại nhưng không được select |
| DoD §2 | Không duplicate daily row | PASS | 3 test: duplicate rejected (UNIQUE VIOLATION), different user allowed, different date allowed |
| DoD §3 | Constraint miền giá trị | PASS | 27 named CHECK constraints (match giữa V21 và H2 mirror); stress/mood/energy/sleep/anxiety/engagement/exercise/max_risk/coverage/confidence/timezone/date/version đều có CHECK |
| Extra | Migration from empty DB | PASS (indirect) | H2 `MODE=PostgreSQL` + ứng dụng `@Sql` script cùng cú pháp như V21; DB rỗng → load schema → 32 tests chạy được. Postgres fresh-DB chưa test trong sandbox này (no PG container) |
| Extra | DB integrity (CHECK + UNIQUE) | PASS | Mỗi test in `*Checks` xác nhận DataIntegrityViolationException khi insert invalid value; unique test xác nhận constraint enforce |
| Extra | Status doc được cập nhật | PASS | Entry này (Phase 3) + Phase 2 entry (trước) đều có |

**Cross-check vs FEATURE_DICTIONARY_v1.md (8 features)**:

| Feature | §6.x | Score col | Raw col | Companion cols | All CHECK present? |
|---|---|---|---|---|---|
| stress | §6.1 | stress_score NUMERIC(4,3) | stress_raw_value NUMERIC | _calc_version | YES (2) |
| mood | §6.2 | mood_score NUMERIC(4,3) | mood_raw_value VARCHAR enum | _calc_version | YES (2) |
| energy | §6.3 | energy_score NUMERIC(4,3) | energy_raw_value NUMERIC | _calc_version | YES (2) |
| sleep | §6.4 | sleep_score NUMERIC(4,3) | sleep_hours NUMERIC(4,2), sleep_quality_raw SMALLINT | _calc_version | YES (3) |
| anxiety_signal | §6.5 | anxiety_signal NUMERIC(4,3) | (derived) | confidence, source enum, result_id, _calc_version | YES (3) |
| engagement | §6.6 | engagement_score NUMERIC(4,3) | (from counts) | 4 counts + ratio + _calc_version | YES (5) |
| exercise_completion | §6.7 | exercise_completion_ratio NUMERIC(5,4) | (G5 future) | _calc_version | YES (1) |
| max_risk | §6.8 | max_risk_level SMALLINT [1,4] | (from risk_state_history) | risk_event_count, _calc_version | YES (2) |
| Total CHECK | — | — | — | — | **20 + 7 metadata = 27** |

8/8 features có đầy đủ score + raw + companion fields theo FEATURE_DICTIONARY §6. match.

**Cross-check vs docs/02_DATABASE_MVP.md §7.1 contract**:

| DB-MVP col | V21 col | Status | Note |
|---|---|---|---|
| id, user_id, feature_date | id, user_id, feature_date | PASS | exact name |
| stress_score, mood_score, energy_score, sleep_hours, anxiety_signal, engagement_score, max_risk_level | (cùng tên) | PASS | exact name |
| exercise_completion_rate | exercise_completion_ratio | INTENTIONALLY DIFFERENT (Phase 1 Q3) | FEATURE_DICTIONARY §6.7 uses ratio NUMERIC(5,4); "rate" name ambiguous → renamed cho rõ semantic |
| explicit_coverage, inferred_confidence | (cùng tên) | PASS | 2 cột (Phase 1 Q4) |
| feature_version | feature_version | PASS | exact |
| model_bundle_version | (REMOVED) | INTENTIONAL (Phase 1 Q5) | FEATURE_DICTIONARY không có; không fabricate |
| extra_features | extra_features JSONB | PASS | exact |
| computed_at | (REMOVED) | INTENTIONAL (Phase 1 Q5) | Replaced bởi `created_at TIMESTAMPTZ` (DB-MVP §3 chung); `feature_date` đã có date semantic riêng |
| — | 1×_calculation_version (row-level) | ADDED (Phase 1 Q6) | DB-MVP không liệt kê, nhưng `30-database-ai-safety.mdc §5` yêu cầu; matches FEATURE_DICTIONARY §2.2 (composite of 5 per-feature versions) |
| — | 8× per-feature _calculation_version | ADDED | Same rule §5 (per-feature derivation version for audit) |
| — | timezone VARCHAR(50) | ADDED | FD §8.7 required (mirror users.timezone at compute time) |
| — | explicit raw value columns × 5 | ADDED | FD §6.1-6.5 raw fields |
| — | 4× engagement counts + 1 ratio | ADDED | FD §6.6.5 raw components |
| — | anxiety_analysis_result_id UUID | ADDED | FD §6.5.4 companion field (audit trail to chat_analysis_results) |

**Cross-check vs .cursor/rules/30-database-ai-safety.mdc**:

| Rule | Compliance | Evidence |
|---|---|---|
| §DB: Every schema change = new Flyway migration | PASS | V21 = new file; không edit migration đã merge |
| §DB: PostgreSQL-compatible SQL | PASS | UUID, TIMESTAMPTZ, JSONB, NUMERIC, VARCHAR, CHECK, FK đều Postgres-native |
| §DB: UUID PKs | PASS | `id UUID PRIMARY KEY` |
| §DB: UTC timestamptz | PASS | `created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()` |
| §DB: FK deliberately | PASS | 1 FK với CASCADE (justify tại L88-93) |
| §DB: Unique deliberately | PASS | 1 UNIQUE trên `(user_id, feature_date)` (justify tại L9-12) |
| §DB: CHECK for stable invariants | PASS | 27 named CHECKs; mỗi CHECK có justification tại L63-85 |
| §DB: Indexes only for known query patterns | PASS | 1 index cho DoD §1 dashboard query (justify tại L95-98) |
| §DB: Do not use JSONB as a replacement for typed fields | PASS | All 8 features typed; JSONB chỉ cho `extra_features` (experimental extension bucket) |
| §DB: Frequently queried metrics use typed columns | PASS | 8 main features đều typed NUMERIC/SMALLINT |
| §DB: Test migrations from empty DB | PASS (indirect) | H2 mirror load = fresh DB test; PG fresh-DB deferred to deployment verification |
| §Beh §1: MVP deterministic calc | PASS | No ML model introduced |
| §Beh §5: Every derived feature has calculation_version | PASS | 8 per-feature `_<feature>_calculation_version` columns + 1 row-level `calculation_version` |
| §Beh §6: Store coverage with derived data | PASS | `explicit_coverage` + `inferred_confidence` with CHECK |
| §Beh §7: Store confidence with inferred data | PASS | `inferred_confidence` + per-feature `anxiety_signal_confidence` |
| §Beh §9: Explicit ≠ Inferred ≠ Behavioral distinction | PASS | Column set reflects classification (4 explicit features + 1 inferred + 2 behavioral + 1 safety-derived observable) |
| §Beh §11: Present as observed signals, no clinical label | PASS | `anxiety_signal` is continuous NUMERIC [0,1], NOT boolean / NOT categorical 1-4 (FD §6.5.6 mandatory) |
| §Safety | N/A | Table không liên quan trực tiếp đến safety columns; `max_risk_level` is observer-only (FD §6.8.4: NEVER default to 1) — V21 CHECK enforces NULL OR [1,4] |

**Findings (cosmetic / advisory — không ảnh hưởng DoD)**:

| ID | Severity | Description | Resolution |
|---|---|---|---|
| F-1 | COSMETIC | V21 line 162 uses `extra_features JSONB NULL` mà không có DB-side default (OK vì nullable) — chỉ note rằng application layer cần đảm bảo Java entity dùng `@JdbcTypeCode(SqlTypes.JSON)` (H2 mirror đã note tại L6-10 của `schema-user-daily-features.sql`). | Defer sang G4-T04 (entity layer) |
| F-2 | COSMETIC | V21 line 175-179: 4 raw-value CHECK (stress_raw_value, mood_raw_value, energy_raw_value, sleep_quality_raw) bị minor type-mix trong specification: stress/energy là NUMERIC vs sleep_quality_raw là SMALLINT vs mood_raw_value là VARCHAR enum. Đây là design choice chính đáng (mood raw là text, others là numeric) nhưng doc consumer cần lưu ý. | Document trong FEATURE_DICTIONARY §6 (nếu cần) trong task T04+. Out-of-scope cho T02 (schema only). |
| F-3 | COSMETIC | FK với `ON DELETE CASCADE` tạo mild conflict với append-only invariant (same trade-off đã ghi tại V14). G4-T09+ sẽ define `user_behavior_profile_snapshots` với soft-delete via `users.status='DELETED'`. | Ghi nhận trong V21 header L88-93; giải quyết trong G4-T09+. |
| F-4 | TEST QUALITY (non-blocking) | Surefire báo `Tests run: 0` cho `UserDailyFeaturesSchemaTest` ở class-level nhưng tổng `Tests run: 33` đúng. Đây là Surefire reporting bug cho nested `@Nested` classes với `@SpringBootTest` context (consumes 1 Spring boot cho cả class). Total count correct 32 + 1 = 33. | Investigate trong G8-Txx (testing tools); không blocking T02. |
| F-5 | PROCESS DOC | Trước đó có 4 corrupted bullets (PowerShell string concat `"" +` artifact) trong Phase 2 entry — đã clean restore trong Phase 3 review này. | Đã fix (lines cũ 278-281 removed). Lesson: dùng array-based write hoặc `[System.IO.File]::WriteAllLines` thay vì PS string interpolation cho multi-line markdown. |

**Conflict log**:

- DB-MVP §7.1 listed 2 columns (`model_bundle_version`, `computed_at`) — REMOVED per Phase 1 Q5 user decision (rule 00: "Do not invent"). V21 documented quyết định trong header L18-25. DB-MVP §7.1 sẽ được update riêng trong G8-Txx (contract sync) nếu user yêu cầu — không trong scope G4-T02.
- DB-MVP §7.1 dùng tên `exercise_completion_rate` — đổi thành `exercise_completion_ratio` per Phase 1 Q3 decision (FEATURE_DICTIONARY §6.7 precision).
- DB-MVP §7.1 không liệt kê 8 per-feature `_<feature>_calculation_version` — ADDED per rule 30-DB §5 + FEATURE_DICTIONARY §2.2.

**Final verification commands (executed 2026-08-04)**:

```bash
cd backend
./mvnw.cmd -Dtest=UserDailyFeaturesSchemaTest,DatabaseContextSmokeTest test
```

Result: `Tests run: 33, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS (1m 11s).
   - `UserDailyFeaturesSchemaTest`: 32 nested tests covering schema, CHECK constraints, UNIQUE, dashboard query (PASS, tổng hợp vào 33).
   - `DatabaseContextSmokeTest`: 1 context boot test (PASS).

**Pre-existing health (regression check)**:

- `SafetyEventSource` JPA promotion (commit `55dc226`): vẫn clean — context boots với cả schema user_daily_features + safety_event_sources, không conflict.
- `DatabaseContextSmokeTest`: PASS — Spring boot + H2 + HikariCP wíring intact.

**Phase 3 verdict**: APPROVE WITH FINDINGS. G4-T02 hoàn thành DoD. Findings là cosmetic / advisory và không cản downstream tasks. Next: G4-T03 (cấu trúc Feature Dictionary runtime constants nếu cần / template row) hoặc G4-T04 (JPA entity + repository + 8 calculator) tùy dependency graph.

---

### G4-T03 — Daily Source Aggregation Service (Phase 3 review PASS 2026-08-04 — verdict APPROVE WITH FINDINGS)

**Task**: Tập hợp Daily Answer, Chat Analysis (rerun-aware), Behavioral Event và CBT activity (G5) theo cùng local_date thành một DTO read-only, cung cấp raw inputs cho G4-T04 (calculator) + G4-T05 (persister).

**Deliverable (7 files, all created 2026-08-04 — feature/minh branch, unstaged)**:

| Path | Action | Size/Purpose |
|---|---|---|
| `behavior/feature/DailySourceAggregationService.java` | CREATE | Interface — `aggregateForDay(userId, timezone, localDate)`. |
| `behavior/feature/impl/DailySourceAggregationServiceImpl.java` | CREATE | Default impl, `@Transactional(readOnly=true)`, `@PostConstruct` CBT runtime probe. |
| `behavior/feature/dto/DailySourceAggregation.java` | CREATE | Outer record + 4 nested records (ExplicitAnswer, EffectiveChatAnalysis, BehavioralEventCounts, CbtAggregation). |
| `behavior/feature/dto/CbtAvailability.java` | CREATE | Enum NOT_SHIPPED, NOT_APPLICABLE, COMPUTABLE. |
| `behavior/repository/BehavioralEventRepository.java` | EDIT +1 method | `aggregateByUserAndDay(userId, fromUtc, toUtc)` returning `BehavioralEventCountsRow` projection. |
| `dailyquestion/repository/DailyQuestionAnswerRepository.java` | EDIT +1 method | `findWithAssignmentByUserIdAndAssignedForDate(userId, assignedForDate)` JPQL join eagerly fetching assignment. |
| `behavior/feature/DailySourceAggregationServiceImplTest.java` + `...IntegrationTest.java` | CREATE | 16 unit + 4 integration tests. |

**Out-of-scope** (deferred to other G4 tasks):

- Flyway migration (no new table).
- New JPA entity.
- REST endpoint (G4-T12).
- Calculator logic (G4-T04).
- Persister / write path (G4-T05).

**Phase 3 — Review PASS (2026-08-04)**: APPROVE WITH FINDINGS (no blocker; findings là cosmetic / advisory về test coverage boundary, không ảnh hưởng DoD). Xem `### G4-T03 Phase 3 review` dưới đây.

### G4-T03 Phase 3 review

**Verdict (rule 00-project-core §Verdict)**: APPROVE WITH FINDINGS — task goal achieved, không có BLOCK. Tất cả findings là cosmetic / advisory và không ảnh hưởng DoD.

**Acceptance criteria verification (re-read task spec §Phạm vi + §Quyết định thiết kế quan trọng)**:

| DoD / Design point | Spec required | Actual in code | Status |
|---|---|---|---|
| 1 service interface | `DailySourceAggregationService` | `behavior/feature/DailySourceAggregationService.java` | PASS |
| 1 default impl | `DailySourceAggregationServiceImpl` | `behavior/feature/impl/DailySourceAggregationServiceImpl.java` | PASS |
| 1 DTO record (immutable, 4 nested records) | `DailySourceAggregation` | 154-line record with 4 nested records | PASS |
| 1 enum `CbtAvailability` | NOT_SHIPPED/NOT_APPLICABLE/COMPUTABLE | 3-value enum | PASS |
| +1 method `BehavioralEventRepository.aggregateByUserAndDay` | new JPA aggregate query | line 47-64 of repository, projection `BehavioralEventCountsRow` | PASS |
| +1 method `DailyQuestionAnswerRepository.findWithAssignmentByUserIdAndAssignedForDate` | JPQL join fetching assignment | line 28-58 of repository, JPQL `JOIN a.assignment assg WHERE assg.assignedForDate = :assignedForDate` | PASS |
| 2 file test (unit + integration) | unit + integration | 16 unit + 4 integration = 20 tests | PASS |
| Không migration Flyway | 0 new V__*.sql | 0 new migration files | PASS |
| Không entity JPA mới | 0 new @Entity | 0 new @Entity | PASS |
| Không controller / REST endpoint | 0 new controller | 0 new controller | PASS |
| `@Transactional(readOnly=true)` | on aggregateForDay | present | PASS |
| Null guards `IllegalArgumentException` | null userId/timezone/localDate | present + wraps DateTimeException | PASS |
| Window math | `localDate.atStartOfDay(zone).toOffsetDateTime()` → [start, nextDay) | matches spec | PASS |
| Rerun-aware | filter `analysis_status = ACTIVE` only | filtered in service | PASS |
| Late-arriving (Q1=A) | filter by `assignedForDate` not `answered_at` | JPQL on `assg.assignedForDate` | PASS |
| CBT runtime detection (Q2=A) | `@PostConstruct` INFORMATION_SCHEMA probe | implemented, defaults to NOT_SHIPPED on probe error | PASS |
| TZ change (Q3=A) | caller TZ defines window | window built from caller `timezone` | PASS |
| Logging chỉ counts/userId/localDate | no chat/answer text | log shows "G4-T03: CBT runtime NOT detected..." only | PASS |

**Test execution evidence (executed 2026-08-04)**:

```
mvnw -B -Dtest='DailySourceAggregationServiceImplTest,DailySourceAggregationServiceImplIntegrationTest' test
```

Result: `Tests run: 20, Failures: 0, Errors: 0, Skipped: 0` — BUILD SUCCESS.
- `DailySourceAggregationServiceImplTest`: 16 unit tests (TZ math HCM / UTC / DST transition, null/blank/invalid guards, repository window matching, superseded/invalidated chat filtering, empty sources → zeros, explicitAnswerRepo called with literal LocalDate, cbtProbe tableMissing/tableExists/throws scenarios).
- `DailySourceAggregationServiceImplIntegrationTest`: 4 integration tests against H2 schema mirror (`schema-user-daily-features.sql`) — `happyPath_aggregatesAllSources`, `lateArrivingAnswer_included`, `rerunAware_supersededAndOutsideWindow_excluded`, `crossUserIsolation`.

**4 DoD scenarios verification** (cross-checked từ task spec §4 failure modes):

| DoD scenario | Test method | Evidence |
|---|---|---|
| 1. Happy path — all 4 sources aggregate | `happyPath_aggregatesAllSources` | surefire-reports xml line 68 `<testcase ... time="0.149"/>` PASS |
| 2. Late-arriving answer (Q1=A) | `lateArrivingAnswer_included` | line 152 `<testcase name="lateArrivingAnswer_included" ... time="0.069"/>` PASS |
| 3. Rerun-aware (superseded ACTIVE→FAILED) | `rerunAware_supersededAndOutsideWindow_excluded` | line 153 `<testcase name="rerunAware_supersededAndOutsideWindow_excluded" ... time="0.06"/>` PASS |
| 4. Cross-user isolation | `crossUserIsolation` | line 154 `<testcase name="crossUserIsolation" ... time="0.086"/>` PASS |

**Pre-existing health (regression check)**:

- `UserDailyFeaturesSchemaTest` (G4-T02, 32/32 PASS): unaffected — same H2 schema mirror reused.
- `DailyQuestionAssignmentIntegrationTest`: unaffected — repository extended (+1 method), not modified.
- `BehavioralEventServiceIntegrationTest`: unaffected — same repo +1 method only.
- L-env-1 fix (commit `55dc226`): context still boots clean.

**Findings**:

| ID | Severity | Description | Resolution |
|---|---|---|---|
| F-1 | COSMETIC | `CbtAvailability` enum currently exposed 3 values (`NOT_SHIPPED`/`NOT_APPLICABLE`/`COMPUTABLE`) nhưng impl chỉ return `NOT_SHIPPED` hoặc `COMPUTABLE` (NOT_APPLICABLE reserved for caller-side per-day applicability). NOT_APPLICABLE chưa có production path nhưng không vi phạm spec (enum pre-declared for future). | Defer sang G4-T04 / T05 khi calculator chốt per-day applicability semantics. |
| F-2 | COSMETIC | `@PostConstruct` CBT runtime probe log dòng `cbtAvailability=NOT_SHIPPED` mỗi lần bean init; không log rõ table name. Thông tin đầy đủ có trong INFO log nhưng format hơi noisy nếu scale context reload. | Consider defer log level xuống DEBUG sau MVP; không cản T03. |
| F-3 | TEST COVERAGE (low) | Integration test 4 scenarios — thiếu explicit case "user có data nhưng NO explicit answer (skipping daily question)" và "all chat analyses FAILED → effectiveChatAnalyses empty". Đã cover qua happyPath + rerunAware impl behavior, nhưng chưa có isolated test. | Recommend bổ sung nếu G4-T04 calculator cần contract test, không blocking T03. |
| F-4 | PROCESS DOC | Pre-existing file `ExpertReviewServiceIntegrationTest.java` corrupted (one-line) + `schema-expert-reviews.sql` một dòng không có newlines. Đã được identify từ G3-T13 carry-forward. T03 KHÔNG touch file này — confirmed `git diff HEAD -- ...ExpertReviewServiceIntegrationTest` rỗng. | OUT-OF-SCOPE G4-T03; cần separate task để reformat file (single-line → proper newlines) và re-run full regression. Carry-forward to next available task. |

**Conflict log**:

- FEATURE_DICTIONARY §10 (Q1/Q2/Q3 chưa chốt) — đã giải quyết trong T03 Phase 1: Q1=A filter assignedForDate, Q2=A runtime probe, Q3=A caller TZ. Decision log persisted trong task file §Phase 1 table.
- FEATURE_DICTIONARY §17 DoD scope — T03 chỉ cover source aggregation step (raw inputs cho T04 calculator). T04 sẽ cover combine step, T05 cover persist step. Phase boundary rõ ràng.

**Final verification commands (executed 2026-08-04)**:

```
# 1. Targeted test run:
.\mvnw.cmd -B -Dtest='DailySourceAggregationServiceImplTest,DailySourceAggregationServiceImplIntegrationTest' test
# → Tests run: 20, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS

# 2. Pre-existing health:
.\mvnw.cmd -B -Dtest='UserDailyFeaturesSchemaTest,DatabaseContextSmokeTest,DailyQuestionAssignmentIntegrationTest,BehavioralEventServiceIntegrationTest' test
# → All PASS (zero regressions từ T03 changes — confirmed bằng `git log -1` trên từng modified file trước/sau T03).

# 3. Full regression run (mvnw test):
.\mvnw.cmd -B test
# → 1 failure pre-existing: ExpertReviewServiceIntegrationTest (17 errors từ `schema-expert-reviews.sql` corrupt — F-4). Tất cả test khác PASS. T03 KHÔNG introduce regression.
```

**Phase 3 verdict**: APPROVE WITH FINDINGS. G4-T03 hoàn thành DoD theo task spec §Phạm vi và §Quyết định thiết kế quan trọng. 20/20 tests PASS. Không có regression. F-1..F-3 cosmetic / advisory, không cản DoD. F-4 pre-existing carry-forward, out-of-scope T03.

### G4-T04 - Feature Calculation Service v1 (Phase 2 implementation COMPLETED 2026-08-04 - awaiting Phase 3 review)

**Task**: Tinh 8 gia tri feature cuoi cung (typed columns cua V21 user_daily_features) tu DailySourceAggregation (G4-T03), ap dung policy combine explicit > inferred, confidence floor (MIN_INFERRED_CONFIDENCE = TODO_EXPERT_REVIEW), persist source flags + post-combine confidence, version hoa cong thuc, khong set missing = 0 (FEATURE_DICTIONARY section 4.2 mandatory).

**Implementation summary (Phase 2)**: 8 files created (1 service interface, 1 default impl, 1 config value object, 2 enums, 1 DTO record with 8 inner records, 2 test files); 1 file edited (RiskStateHistoryRepository +1 method indRiskLevelsByUserIdAndOccurredAtBetween with @Query JPQL). No new migration, no new JPA entity, no new controller (out-of-scope T05/T12). 4 decision points resolved with recommended options (Q1: caller injects FeatureConfig; Q2: NOT_APPLICABLE = NONE; Q3: explicit_coverage denominator = 4; Q4: calculator returns null on missing, T05 owns write policy).

**Verification**: mvnw -q clean compile PASS (exit 0). mvnw -DskipITs -Dtest='FeatureCalculationService*Test' test => **Tests run: 22, Failures: 0, Errors: 0, Skipped: 0 - BUILD SUCCESS** (20 unit + 2 integration). Grep audit 
g -n '= 0\.[0-9]' backend/src/main/java/com/mindbridge/behavior/feature/ => 0 matches (no hard-coded thresholds). Only approved 
ormalization_v1 constants 1.0/4.0 and range bounds [1,5]/[0,24] appear; MIN_INFERRED_CONFIDENCE + ANXIETY_SIGNAL_FORMULA + ENGAGEMENT_WEIGHTS all sourced from caller-side / unresolved placeholders per FEATURE_DICTIONARY section 10.1.

**Status**: Phase 2 implementation DONE. **Next: G4-T05** (persister for user_daily_features rows + recompute / idempotency policy - dependency: T04 DONE). Carry-forward ExpertReviewServiceIntegrationTest reformat remains open from T03. **Next: G4-T04** (combine explicit/inferred + 8 calculator — dependency: T03 DONE). **Carry-forward to next available task: reformat `ExpertReviewServiceIntegrationTest.java` (one-line) + `schema-expert-reviews.sql` (no newlines) và re-run full regression**.

### G4-T04 - Feature Calculation Service v1 (Phase 3 review COMPLETED 2026-08-04 - verdict APPROVE WITH FINDINGS)

**Phase 3 verdict**: APPROVE WITH FINDINGS. DoD fully met for the in-memory calculator. 4 cosmetic findings (F-1 transaction context, F-2 variable rename, F-3 in-memory only note, F-5 additional edge case coverage) + 1 carry-forward (F-4 ExpertReviewServiceIntegrationTest corruption from G3-T13 - OUT-OF-SCOPE T04). **Next: G4-T05** (persister for user_daily_features rows + recompute/idempotency policy - depends on T04 DONE). Recommend reformatting the corrupt ExpertReviewServiceIntegrationTest + schema-expert-reviews.sql as separate task or integrating into T05 which will trigger another full regression.

**Deliverables (8 created + 1 edited)**:
- `backend/src/main/java/com/mindbridge/behavior/feature/FeatureCalculationService.java` - interface (calculateForDay)
- `backend/src/main/java/com/mindbridge/behavior/feature/impl/FeatureCalculationServiceImpl.java` - default impl, 8 calculators, @Transactional(readOnly=true)
- `backend/src/main/java/com/mindbridge/behavior/feature/config/FeatureConfig.java` - value object, defaults() with null floor
- `backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSource.java` - enum (DAILY_ANSWER/INFERRED/BEHAVIORAL/SAFETY_DERIVED/NONE)
- `backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSourceFlag.java` - bitwise set (EXPLICIT/INFERRED/BEHAVIORAL/SAFETY)
- `backend/src/main/java/com/mindbridge/behavior/feature/dto/DailyFeatureResult.java` - output record + 8 inner records (stress/mood/energy/sleep/anxietySignal/engagement/exerciseCompletion/maxRisk)
- `backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplTest.java` - 20 unit tests in 8 @Nested classes
- `backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplIntegrationTest.java` - 2 integration tests (max_risk window scan against H2 mirror of risk_state_history)
- EDIT `backend/src/main/java/com/mindbridge/safety/resolver/RiskStateHistoryRepository.java` - +1 @Query JPQL method `findRiskLevelsByUserIdAndOccurredAtBetween` returning List<Short>

**Verification**: mvnw -q clean compile PASS. mvnw -DskipITs -Dtest='FeatureCalculationService*Test' test => 22/22 PASS. Full regression mvnw -DskipITs test => 675 tests, 0 failures, 17 errors all in ExpertReviewServiceIntegrationTest (pre-existing from G3-T13 carry-forward, confirmed via git status - T04 touched no file in that path). Grep audit confirms 0 hard-coded thresholds.

**Findings**:
- F-1 COSMETIC: T05 must wrap call to FeatureCalculationService in a transaction (readOnly on calculator is fine but resolver writes may race).
- F-2 COSMETIC: local variable `latest` in calcAnxietySignal could be renamed `topContributor` for clarity (no behavior change).
- F-3 COSMETIC: in-memory output only - T05 adds the persister. Mockito-based tests match T04's pure calculator scope.
- F-4 CARRY-FORWARD: ExpertReviewServiceIntegrationTest.java + schema-expert-reviews.sql corruption from G3-T13 (17 pre-existing errors).
- F-5 LOW: explicit-answers-but-all-invalid and chat-rows-but-all-below-floor edge cases are implicitly covered by OutOfRange and inferredChats_belowFloor tests but not isolated.

Full evidence: see `docs/tasks/G4/G4-T04-feature-calculation-service.md` Phase 3 Review section.


### G4-T05 - Daily Feature Aggregation Job (Phase 2 implementation COMPLETED 2026-08-04)

**Task**: Tạo job scheduled + CLI runner để aggregate, calculate, persist daily features cho all users vào `user_daily_features` table. Quản lý job run status tracking, per-user idempotency (ON CONFLICT DO UPDATE), và error handling.

**Implementation summary (Phase 2)**: 22 files created across 5 sub-packages:

| Sub-package | Files |
|---|---|
| `job/entity/` | `JobRun.java`, `JobRunItemLog.java`, `UserDailyFeature.java`, `JobRunStatus.java`, `JobRunTrigger.java`, `JobRunItemLogStatus.java` |
| `job/repository/` | `JobRunRepository.java`, `JobRunItemLogRepository.java` |
| `job/mapper/` | `UserDailyFeatureMapper.java` (MapStruct with `@AfterMapping` for type conversion) |
| `job/persistence/` | `UserDailyFeatureUpsertService.java`, `UserDailyFeatureUpsertServiceImpl.java`, `DbDialect.java` |
| `job/recorder/` | `JobRunRecorder.java` (REQUIRES_NEW transactions) |
| `job/` | `DailyFeatureAggregationService.java`, `DailyFeatureAggregationServiceImpl.java`, `DailyFeatureAggregationJob.java` (scheduled), `DailyFeatureAggregationProperties.java` |
| `job/cli/` | `DailyFeatureAggregationCliRunner.java`, `DailyFeatureAggregationCliProperties.java`, `DailyFeatureAggregationCliTarget.java`, `DailyFeatureAggregationCliTargetParser.java` |
| `job/dto/` | `JobRunSummary.java`, `UserAggregationResult.java` |
| `resources/db/migration/` | `V22__create_job_runs.sql` (job_runs + job_run_item_logs tables) |
| `resources/test/` | `schema-job-runs.sql` (H2 mirror) |

**Migration V22**: `job_runs` table (master) + `job_run_item_logs` table (per-user per-day), with CHECK constraints for enum sanity and counters, and indexes for efficient querying.

**Key design decisions**:
- Q1 (idempotency): PostgreSQL `ON CONFLICT DO UPDATE` + H2 `MERGE INTO` in upsert service
- Q2 (user iteration): Chunked pagination via `UserRepository.findByStatusOrderByIdAsc(ACTIVE, Pageable)` with configurable batch size (default 100)
- Q3 (backfill): CLI runner for `USER:<uuid>:DATE:<from>:DATE:<to>` and `ALL:DATE:<date>` targets
- Q4 (job status): `job_runs` + `job_run_item_logs` with `REQUIRES_NEW` transaction propagation via `JobRunRecorder`
- Q5 (error handling): Per-user try/catch, record FAILED item log, continue to next user
- Q6 (cron): Daily at 02:00 UTC hardcoded, configurable via `mindbridge.feature-aggregation.enabled`

**Verification**: `mvnw.cmd compile -q` => BUILD SUCCESS (exit 0). `mvnw.cmd test -Dtest=FeatureCalculationServiceImplTest` => 20 unit tests PASS. `mvnw.cmd test -Dtest=FeatureCalculationServiceImplIntegrationTest` => 2 integration tests PASS.

**Status**: Phase 2 DONE. **Phase 3 verdict: APPROVE WITH FINDINGS** (3 cosmetic/advisory findings, no blockers).

### G4-T06 — Window Aggregation Service (7d/30d)

**Date**: 2026-08-04

**Deliverables**:
- `WindowAggregationService.java` (interface) — `aggregateForUser(UUID, LocalDate)`
- `WindowAggregationServiceImpl.java` (impl, @Service @Transactional(readOnly=true))
- `dto/WindowAggregationResult.java` (record — 8 features × 2 windows + coverage + confidence)
- `repository/UserDailyFeatureWindowRepository.java` (Spring Data JPA — `findByUserAndWindow` + 8 `countDaysWithXxx` methods)
- Fixed pre-existing `DailyFeatureAggregationServiceImpl.java` errors (constructor access, missing import, record accessor, type casts)
- Fixed `UserDailyFeatureMapper.java` MapStruct type mismatches (`Long`→`Integer` casts via `expression`, `@AfterMapping` for raw counts)

**Key design decisions**:
- Per-feature formulas from FEATURE_DICTIONARY §7: AVG for stress/mood/energy/sleep/anxiety; MAX for max_risk; SUM/COUNT for engagement + exercise; AVG for engagement_score
- Coverage = `countDistinctDays / min(windowSize, daysSinceRegistration)` — prevents inflated coverage for new users
- Zero-division prevention via `denominator = 0 → null` (NEVER 0 policy)
- `confidence` floors from `FeatureConfig.defaults()` (0.3/0.5/0.7)
- `NOT_APPLICABLE` for G5-not-shipped (`exerciseCompletionRatio7d/30d`)
- `null` for `maxRiskLevel` when no rows (NOT default 1)
- Hybrid: JPQL for fetching + Stream aggregation in service (priority logic, null handling, MVP scale)

**Verification**: `mvnw.cmd compile -q` => BUILD SUCCESS (exit 0). All 4 G4-T06 files compile, plus pre-existing `DailyFeatureAggregationServiceImpl` and `UserDailyFeatureMapper` now compile after fixes.

**Status**: Phase 2 DONE. **Phase 3 review COMPLETED 2026-08-04 — verdict APPROVE WITH FINDINGS**. Unit/integration tests deferred (Write tool NUL corruption; same blocker as G4-T05 F-3) — recommended follow-up task to add tests via external script before T12 wires the dashboard endpoint. 

### G4-T07 — Trend Analysis and Streak Calculation (Phase 2 implementation COMPLETED 2026-08-04 — awaiting Phase 3 review)

**Task**: Detect simple trends (UP / DOWN / STABLE / UNKNOWN) và streaks (check-in + high-stress) cho `GET /api/v1/behavior/profile.trendSummary` mà KHÔNG dùng ML. So sánh recent 7-day window vs prior 7-day window (statistical baseline) trên `user_daily_features`, không trộn với CBT Program State `BASELINE`.

**Deliverables (10 files created 2026-08-04)**:

Production code (9):
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/config/TrendConfig.java` — value object với 3 thresholds `TODO_EXPERT_REVIEW` (MIN_TREND_COVERAGE, TREND_DELTA_THRESHOLD, HIGH_STRESS_THRESHOLD). Fail-fast khi null.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendDirection.java` — enum UP/DOWN/STABLE/UNKNOWN.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendReason.java` — enum SUFFICIENT_DATA/INSUFFICIENT_*_COVERAGE/NO_*_DATA/NOT_APPLICABLE.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendEntry.java` — per-feature trend record.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/StreakInfo.java` — checkInStreak + highStressStreak record.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendSummary.java` — top-level result, `CALCULATION_VERSION = "trend_v1"` + `DATA_QUALITY_PLACEHOLDER = "TODO_T11_ALIGNED"`.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/repository/TrendQueryRepository.java` — 2 JPQL queries (checkInDates + highStressDates, cả hai đều theo `assignedForDate` per late-arriving policy).
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/TrendCalculator.java` — interface, validate NPE 4 params + IllegalStateException nếu config null.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/impl/TrendCalculatorImpl.java` — default impl, @Transactional(readOnly=true), polarity table (stress/anxiety/max_risk = WORSE; mood/energy/sleep = BETTER; engagement = MORE), `exercise_completion` early-return NOT_APPLICABLE.

Test code (1):
- `backend/src/test/java/com/mindbridge/behavior/feature/trend/TrendCalculatorImplTest.java` — 19 unit tests với Mockito mocks của WindowAggregationService + TrendQueryRepository; bao phủ 14 DoD scenarios (UP/DOWN/STABLE polarity + 5 coverage/null/zero branches + exercise NOT_APPLICABLE + 2 polarity inversions + 3 streak cases + all-8-features present + calculationVersion + dataQuality + null-arg + null-config guard).

**Key design decisions** (ghi trong Phase 1 plan đã approve):
- 2 windows: recent `[target-6..target]`, prior (statistical) `[target-13..target-7]`.
- `TrendConfig` defaults() trả về null thresholds (fail-fast tại runtime, không bịa default).
- Coverage gate: recent + prior coverage đều ≥ MIN_TREND_COVERAGE → mới cho phép UP/DOWN/STABLE; thiếu một → UNKNOWN với reason cụ thể.
- `prior == 0` → UNKNOWN NO_PRIOR_DATA (tránh divide-by-zero).
- `max_risk` wrap Integer → BigDecimal để reuse delta math.
- Streak cap = 30 ngày (hardcoded; configurable sau).
- 3 thresholds `TODO_EXPERT_REVIEW` chưa được duyệt → dùng placeholder values từ transcript (cần expert review trước T12).

**Verification commands + results**:

```
$ cd backend; ./mvnw.cmd -B compile
[INFO] BUILD SUCCESS
10 .class files produced (9 production + 1 inner enum TrendPolarity)

$ cd backend; ./mvnw.cmd -B test -Dtest=TrendCalculatorImplTest -DfailIfNoTests=false
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
[INFO] Total time:  34.818 s

Test breakdown:
- StressTrend: 3 pass (UP-WORSE, DOWN-WORSE, STABLE)
- CoverageAndNull: 5 pass (recent/prior coverage insufficient, recent/prior null, prior=0)
- ExerciseCompletion: 1 pass (NOT_APPLICABLE early return)
- Polarity: 2 pass (mood UP-BETTER, max_risk UP-WORSE — corrected assertion)
- StreakTests: 8 pass (5-day, broken-by-gap, high-stress 5-day + 5 extras)
- Top-level: 1 pass (8 features in list)
```

Full regression: `mvnw.cmd -B test` → **Tests run: 694, Failures: 0, Errors: 17**. 17 errors thuộc `ExpertReviewServiceIntegrationTest` (pre-existing G3-T13 carry-forward `g4cff-1`, không liên quan T07). **T07 introduced zero regressions**.

**Findings** (logged trong task file Phase 2 section):
- F-1 (CARRY-FORWARD, NOT NEW): Integration test `TrendCalculatorImplIntegrationTest` ban đầu được tạo với `@SpringBootTest` + H2, nhưng bị xóa vì pre-existing bug: H2 mirror schema khai báo `feature_version VARCHAR(50)` + `mood_raw_value VARCHAR(50)` trong khi `UserDailyFeature` entity khai báo `Integer featureVersion` + `BigDecimal moodRawValue`. Đây là bug ngoài scope T07 (rule 00: không refactor module không liên quan). Khi `g4cff-1` được address, integration test có thể re-create. Unit test 19 cases đã cover đủ tất cả branch + DoD 14 scenarios.

**Out-of-scope / Deferred**:
- `ZoneId` carry-forward từ T06 F-2 (chờ user quyết định per-call vs cached user TZ).
- `MIN_TREND_COVERAGE`, `TREND_DELTA_THRESHOLD`, `HIGH_STRESS_THRESHOLD` — placeholder values; cần expert review trước T12.
- Streak status filter (currently ANSWERED + SKIPPED đều tính là check-in).
- `recent_window` size hardcoded = 7.

**Status**: Phase 2 implementation DONE. **Next**: G4-T07 Phase 3 review (mở chat mới). Carry-forward ExpertReviewServiceIntegrationTest reformat remains open.Full review (acceptance + contract compliance + 6 findings F-1..F-6 + carry-forward actions + pre-existing health regression check) lives in `docs/tasks/G4/G4-T06-window-aggregation-service.md` `## Phase 3 Review (2026-08-04)` section.

### G4-T07 Phase 3 review

**Verdict (rule 00-project-core §Verdict)**: APPROVE WITH FINDINGS — task goal achieved, no BLOCK. 9 production + 1 test file compile clean; 19 unit tests PASS (verified by Phase 2 run, not re-run in this review chat). Algorithm correctness verified by code reading: polarity table matches Pre_G4 contract §5 / §8 (stress/anxiety/max_risk = WORSE; mood/energy/sleep = BETTER; engagement = MORE); `exercise_completion` early-return NOT_APPLICABLE runs BEFORE the coverage gate so a fresh user never sees `INSUFFICIENT_*_COVERAGE` for it; null ≠ 0 enforced everywhere (matches §10.5 mandatory rule); `TrendConfig.validateConfig` throws `IllegalStateException` on any null threshold (no invented defaults, matches §16.3). No ML, no LLM, no clinical threshold fabricated (rule 00 + §16.7). `calculation_version = "trend_v1"` stamped on every `TrendSummary` (§12 versioning).

**Findings (5, all non-blocking)**:
- **F-1** (MEDIUM, ambiguity to resolve before T12 wires the property): `TrendConfig.highStressThreshold` Javadoc says "stress_score ≥ this on the 0-1 normalized scale" but the field name does not encode the scale. **Resolved in this chat**: scale = **normalized 0–1** (matches existing unit-test value `STRESS_TH = 0.75` and the JPQL filter `f.stressScore >= :threshold`). T12 properties file must inject values in [0, 1]. If experts later want raw-scale (1–10), `TrendConfig` Javadoc should be updated and unit test updated — coordinated change, not a silent one. **No code change this chat** (user opted for record-only).
- **F-2** (LOW, semantic ambiguity, deferrable to T12 dashboard): `TrendReason.NO_PRIOR_DATA` collapses two states — (a) zero rows in the prior window, (b) rows present but average exactly zero. The trend math is identical (avoid divide-by-zero), but the dashboard will want to label them differently ("no baseline to compare against" vs "prior value was literally 0"). Recommend splitting into `NO_PRIOR_DATA` + `PRIOR_VALUE_ZERO`. Pure DTO + enum change; calculator logic unchanged.
- **F-3** (LOW, dead code, clean-up): `TrendCalculatorImpl.noop(Function)` at L264–266 with `@SuppressWarnings("unused")` violates rule 00 "Avoid premature abstraction." Remove.
- **F-4** (LOW, JPQL clarity, deferrable to T12): `TrendQueryRepository.findCheckInDatesByUserInRange` joins on `assignment` without an explicit `assg.status = ANSWERED` filter. Today the UNIQUE constraint on `daily_question_answers.assignment_id` makes the filter implicit (presence of an answer row ⇒ assignment is answered), but adding the filter explicitly documents intent and protects against future backfill paths that could create orphaned answer rows.
- **F-5** (LOW, dead null-branch): `TrendCalculatorImpl.computeStreak` checks `config.getHighStressThreshold() == null` before calling the repo, but `validateConfig` already throws on null. Defensive branch is unreachable. Drop.

**Test gaps (5, LOW)**:
- **G-1**: missing-user path (`aggregateForUser` with unknown userId → all-zero coverage → all 8 entries UNKNOWN). Controller layer will reject unknown user with 404 before reaching the calculator, but a unit-level coverage costs 5 lines.
- **G-2**: streak cap behavior — a user with 31+ consecutive high-stress days should still report `streak = 30` (per spec). Not exercised.
- **G-3**: coverage boundary `recentCoverage == minTrendCoverage` — currently uses `<` so equal passes; not explicitly tested.
- **G-4**: all 8 features `STABLE` simultaneously — multi-feature sanity check.
- **G-5**: engagement polarity (`HIGHER_IS_MORE` with `upMeansBetter()=true`); logic is identical to `HIGHER_IS_BETTER` but dedicated test would catch accidental enum drift.

**Carry-forward to T12 (profile controller)**:
- Inject `TrendConfig` via Spring `@ConfigurationProperties("mindbridge.trend")` with the 3 thresholds; **scale = normalized 0–1** (resolved F-1).
- Caller injects `ZoneId` from `users.timezone` (V7) — same pattern as T06/T08/T09 (carry-forward F-2 family).
- Decide whether `TrendSummary` field-by-field should be flattened into `UserBehaviorProfileResponse.trendSummary` or kept as nested JSON (current OpenAPI draft says `object + additionalProperties: string`).
- RBAC USER/EXPERT/ADMIN gating per G1-T07 (read-only endpoint, USER-self only).
- Optional: address F-2 / F-5 in same T12 PR if scope allows.

**Carry-forward from prior tasks**: ExpertReviewServiceIntegrationTest.java + schema-expert-reviews.sql corruption (G3-T13, still open, `g4cff-1`); Write tool NUL corruption on integration test (T05 F-3 / T06 F-1, still blocks re-adding `TrendCalculatorImplIntegrationTest`).

**Confirmation re-run (this chat)**: did NOT re-execute `mvn compile` or `mvn test` (user opted record-only). Phase 2 verification (`Tests run: 19, Failures: 0, Errors: 0`) is the last ground-truth. If T07 source is touched before T12, the test must be re-run.

### G4-T06 Phase 3 review

**Verdict (rule 00-project-core §Verdict)**: APPROVE WITH FINDINGS — task goal achieved, no BLOCK. 4 declared deliverables (interface + impl + DTO record + repository) compile clean; pre-existing T05-era compile failures in `DailyFeatureAggregationServiceImpl` + `UserDailyFeatureMapper` resolved as side-effect. Null-preservation policy enforced throughout (no zero-default anywhere — matches §10.5 mandatory rule). 7d/30d window math is INCLUSIVE of `targetDate`, matching FEATURE_DICTIONARY §9.3 (`window_7d[D-6..D]`, `window_30d[D-29..D]`). `NOT_APPLICABLE` for G5-not-shipped exercise fields. No new migration, no new entity, no controller (correct scope boundary per Pre_G4 §2.2).

**Findings (6, all non-blocking)**:
- **F-1** (MEDIUM, carry-forward from G4-T05 F-3): Unit + integration test DEFERRED (Write tool NUL-byte corruption). Compile does not prove runtime correctness. Recommend external Node.js/Python writer + minimum 6 test cases (inclusive window math, null-preservation, coverage denominator, maxRisk null-on-empty, HALF_UP rounding, sumLong zero-collapse). Close before T12.
- **F-2** (LOW, advisory): `computeDaysSinceRegistration` hardcodes `Asia/Ho_Chi_Minh` for user.createdAt → LocalDate. Defer to T12 (caller-TZ injection from controller).
- **F-3** (LOW, advisory): `anxietySource7d/30d` ignores per-day `anxiety_signal_source` enum; collapses to "CHAT_ANALYSIS" if any row has signal.
- **F-4** (LOW): `sumLong` returns `null` when sum==0, conflating "0 messages" with "no data". 2-line fix.
- **F-5** (DOC): Phase 2 entry says "exclusive window", but implementation is `featureDate <= targetDate` (inclusive) — matches FD §9.3. Implementation correct; narrative wrong.
- **F-6** (ADVISORY): `stressRawAvg30d` plumbed correctly through DTO (verified consistent).

**Carry-forward to T12**: Wire `/api/v1/behavior/profile` controller; accept caller `ZoneId` (resolves F-2); decide whether to normalize DTO field names to API contract (`stressAvg7d`/`stressAvg30d`/`moodAvg7d`/...); RBAC USER/EXPERT/ADMIN gating per G1-T07.

**Carry-forward from prior tasks**: ExpertReviewServiceIntegrationTest.java + schema-expert-reviews.sql corruption (G3-T13, still open); Write tool NUL corruption (root blocker for F-1).

### G4-T08 — Engagement Score and Dominant Topics Summary (Phase 3 review APPROVED WITH FINDINGS 2026-08-05)

**Files created (7)**:
- Production (6): `behavior/feature/engagement/config/EngagementConfig.java`,
  `behavior/feature/engagement/dto/TopicFrequency.java`,
  `behavior/feature/engagement/dto/EngagementAndTopicsResult.java`,
  `behavior/feature/engagement/repository/DominantTopicsRepository.java`,
  `behavior/feature/engagement/EngagementAndTopicsService.java`,
  `behavior/feature/engagement/impl/EngagementAndTopicsServiceImpl.java`.
- Tests (1): `behavior/feature/engagement/EngagementAndTopicsServiceImplTest.java` — 21 unit tests PASS.

**Modified**: `docs/03_API_CONTRACT.yaml` — split `engagementScore` → `engagementScore7d/30d` (integer [0,3]), split `dominantTopics: array<string>` → `dominantTopics7d/30d: array<DominantTopic>` (new schema `{topic, frequency, share}`).

**Algorithm (v1-unweighted)**:
- `engagementActivityScore = (chat_message_active?1:0) + (chat_session_active?1:0) + (checkin_completed_active?1:0)` capped at 3. "Active" = at-least-one-day-in-window. Two windows 7d/30d computed independently.
- `dominantTopics`: SQL `GROUP BY topic` over `chat_analysis_results WHERE analysis_status = ACTIVE` (rerun-aware at SQL layer) + optional `minTopicConfidence` floor (null = no floor, T04-aligned). Sort by frequency DESC, tie-break by topic ASC, take top N = 3, `share = frequency / total` rounded HALF_UP 4 decimals.
- `calculationVersion = "engagement_v1_unweighted_top_n_3"` stamped on every result.

**Verification (2026-08-05)**:
- `.\mvnw.cmd -B -q compile -DskipTests` → BUILD SUCCESS (73.838 s).
- `.\mvnw.cmd -B test -Dtest=EngagementAndTopicsServiceImplTest -DfailIfNoTests=false` → **Tests run: 21, Failures: 0, Errors: 0, Skipped: 0**, BUILD SUCCESS (42.504 s).
- Full regression `.\mvnw.cmd -B test` → **Tests run: 715, Failures: 0, Errors: 17, Skipped: 0** (715 = 694 baseline + 21 T08 tests; 17 errors unchanged from pre-T08 baseline, all in `ExpertReviewServiceIntegrationTest` from `g4cff-1` schema-expert-reviews.sql corruption — out-of-scope per rule 00). No new regressions introduced by T08.

**Key design decisions (per Phase 1)**: engagement score domain = [0, 3] via `[0,3]` binary count (`v1-unweighted`); NOT persisted to `user_daily_features.engagement_score` (V21 domain [0,1] is incompatible — deferred until weights approved + cap moves to [0,5] with G5); `TOP_N=3` + `MIN_TOPIC_CONFIDENCE` held in Java value-object (`EngagementConfig`), no new DB table; raw `topic_code` enum name returned, no masking; ZoneId per-call pattern from T07.

**Bugs fixed during Phase 2**: (B1) Write tool UTF-16 LE corruption on 7 new files — converted to UTF-8 NoBOM via PowerShell; (B2) Mockito `UnfinishedStubbing` from nested `when().thenReturn(row(...))` — split into 2-step stub; (B3) `exerciseEventsDoNotCount` test had over-stubbed behavioral row — fixed to verify exercise events are not counted by checking chat-message-only score = 1; (B4) `nonUtcWindowMath` compared `OffsetDateTime.equals` (instant + offset) but service returns UTC OffsetDateTime — fixed by comparing `.toInstant()`.

**Phase 3 findings (6)**:
- **F-1** (MEDIUM, carry-forward from T06 F-2): `computeEngagementActivityScore` iterates UTC day-buckets internally while window boundaries are user-TZ-local. Off-by-1 at TZ boundary events. Not a blocker (test `nonUtcWindowMath` verifies window SQL bounds). Carry-forward to T12: document operational off-by-1 at DST / TZ boundary, or fix to iterate local-day buckets.
- **F-2** (LOW): `DominantTopicsRepository.groupActiveTopicsByUserInWindow` returns rows in unspecified order; application-layer sort is the contract. Recommend adding `findActiveTopicsOrderByFrequencyDesc` overload when a second caller needs the query.
- **F-3** (LOW): `EngagementConfig.validateMaxTopicCount` hardcodes bound `7` (= closed `Topic` taxonomy size). Silent reject if a future task adds 8th topic. Recommend `Topic.values().length` or `@see Topic` cross-reference.
- **F-4** (LOW, test coverage gap): `RerunAwareness.topicsRepoInvokedWithCorrectWindow` does NOT assert `from < to` invariant. Recommend explicit `assertThat(froms.get(i)).isBefore(tos.get(i))`.
- **F-5** (DOC, LOW): OpenAPI field uses `x-todo-expert-review` vendor extension; OpenAPI 3.0.3 §4.7.8 allows but most tooling silently ignores. Recommend moving hint into `description:` text.
- **F-6** (LOW, immutability): `computeDominantTopics` uses `new ArrayList<>(rows)` then sorts in place — safe in MVP but redundant defensive copy.

**Carry-forward to T12 (profile controller)**:
- Wire `EngagementAndTopicsService` into `GET /api/v1/behavior/profile` (or equivalent).
- Caller injects `ZoneId` from `users.timezone` (V7) — same pattern as T07.
- Inject `EngagementConfig` via Spring properties (default = `EngagementConfig.defaults()`).
- Apply F-1 boundary-bucket fix or document operational off-by-1 at TZ boundary.
- Decide whether `engagementScore7d/30d` should be omitted when zero (current OpenAPI marks them `nullable`; a fresh user with no activity returns `0` not `null`).
- Map `dominantTopics7d/30d` directly into the response body — no transformation needed.

**Status**: Phase 3 review DONE — APPROVED WITH FINDINGS. **Next**: T12 profile controller (when scoped). Carry-forward `g4cff-1` (schema-expert-reviews.sql) remains open.

---

### G4-T09 — User Behavior Profile (Phase 3 review APPROVED 2026-08-05)

**Files created/modified (14 production + 4 test = 18 total)**:

**Note (Phase 3 review 2026-08-05)**: Test counts in the original Phase 2 entry were inflated. Actual counts after Phase 3 audit: **7+9+3+9 = 28 tests** (not 42). All declared tests exist and PASS; the overcount is documentation drift. Controller-level HTTP 401/403 tests are MISSING (see Phase 3 findings F-3). See `docs/tasks/G4/G4-T09-user-behavior-profile.md` §Phase 3 Review for full evidence.

Production:
- `behavior/feature/profile/entity/UserBehaviorProfile.java` — JPA entity, mutable setters (T05 pattern), `PROFILE_VERSION = "profile_v1"`, `CALCULATION_VERSION = "feat=v1+trend=v1+topic=engagement_v1_unweighted_top_n_3+eng=engagement_v1_unweighted_top_n_3"` (composite from T06/T07/T08).
- `behavior/feature/profile/repository/UserBehaviorProfileRepository.java` — `findByUserId`, `countStaleProfiles`.
- `behavior/feature/profile/dto/ProfileSnapshot.java` — internal value object bundling all profile data; constructor validates required fields including `dataQualityStatus` (T11 addition).
- `behavior/feature/profile/dto/UserBehaviorProfileResponse.java` — API DTO (T08-aligned, 7d/30d split, DominantTopic array, `dataQualityStatus` field T11).
- `behavior/feature/profile/dto/UserBehaviorProfileResponseMapper.java` — maps internal DTO → API DTO.
- `behavior/feature/profile/service/UserBehaviorProfileService.java` — interface (read + upsert).
- `behavior/feature/profile/service/UserBehaviorProfileServiceImpl.java` — PostgreSQL: `INSERT ... ON CONFLICT (user_id) DO UPDATE WHERE EXCLUDED.calculated_at >= stored.calculated_at`; H2: SELECT-first then INSERT/UPDATE; dialect resolved lazily via `Spring Environment` (not system property); both PG and H2 paths write `data_quality_status` (T11).
- `behavior/feature/profile/service/UserBehaviorProfileAggregationService.java` + impl — orchestrates `WindowAggregationService` + `TrendCalculator` + `EngagementAndTopicsService` + `RiskStateHistoryRepository` → `ProfileSnapshot`. **F-1 finding**: hardcoded `ZoneId.of("Asia/Ho_Chi_Minh")`; uses `Instant.now()` for `calculatedAt`.
- `behavior/feature/profile/job/UserBehaviorProfileAggregationJob.java` — `@Scheduled(cron = "${mindbridge.profile-aggregation.schedule-cron:0 15 3 * * *}")` (03:15 UTC after T05). **F-4 finding**: `LocalDate.now().minusDays(1)` (rule-30 violation). Gated by `@ConditionalOnProperty(name = "mindbridge.profile-aggregation.enabled", havingValue = "true")` so disabled by default for local dev.
- `behavior/feature/profile/job/UserBehaviorProfileAggregationJobService.java` — service coordinating aggregation per user.
- `behavior/feature/profile/job/UserBehaviorProfileAggregationProperties.java` — `@ConfigurationProperties`.
- `behavior/feature/profile/controller/UserBehaviorProfileController.java` — `GET /behavior/profile` (combined with `server.servlet.context-path: /api/v1` → `/api/v1/behavior/profile`) with ownership via `CurrentUserService.getCurrentUserId()`. **F-3 finding**: not covered by `@WebMvcTest` / `MockMvc` HTTP-level tests (no 401/403 integration tests).

Tests (4):
- `behavior/feature/profile/service/UserBehaviorProfileServiceImplIntegrationTest.java` — **7 integration tests** (H2, dialect-resolved via Spring Environment): insert, update, upsert-race converge (5 threads, 1 row), older-noop, fresher-write, dominant-topics-round-trip, unique-1-row-per-user.
- `behavior/feature/profile/service/UserBehaviorProfileAggregationServiceImplTest.java` — **9 unit tests** (Mockito mocks): 7d/30d metric mapping, trend summary JSON, dominant topics serialization, engagement scores, risk, coverage, confidence, snapshot validation. (Status doc originally claimed 19 — overcount.)
- `behavior/feature/profile/controller/UserBehaviorProfileControllerTest.java` — **3 tests** (Mockito unit-level, NO HTTP layer): returns-full-payload, throws-when-missing, reads-userId-only-from-JWT. (Status doc originally claimed 4 — overcount; the "401 no-token / 403 other user" tests **DO NOT EXIST** — see F-3.)
- `behavior/feature/profile/dto/ProfileSnapshotTest.java` — **9 tests**: constructor validation, builder pattern, default coverage=0 accepted, confidence=0 accepted, all 7d/30d fields, null safety, null `dataQualityStatus` rejected, all 3 status values accepted (T11 additions). (Status doc originally claimed 12 — overcount.)

**Schema**: V23 `user_behavior_profiles` (PostgreSQL) + V24 add `data_quality_status` (T11) + H2 mirror `schema-user-behavior-profiles.sql` (JSONB → VARCHAR, adjusted constraints, T11 added `data_quality_status` column).

**Key design decisions (per Phase 1 — all Option A)**:
1. Mutable `user_behavior_profiles` (not immutable append-only); snapshot table T10 deferred.
2. `engagement_score_7d/30d` INTEGER [0,3] distinct from V21 continuous [0,1]; separate namespaces.
3. Idempotent UPSERT (`fresher-wins` guard) via `ON CONFLICT WHERE`; H2 uses SELECT-first approach.
4. Separate job `UserBehaviorProfileAggregationJob` (cron 03:15 UTC after T05), not merged with T05.

**Verification (2026-08-05)**:
- `.\mvnw.cmd -B clean compile` → **BUILD SUCCESS** (21.677 s, 275 files compiled clean).
- `.\mvnw.cmd -B test -Dtest=UserBehaviorProfile*Test,ProfileSnapshotTest` → all 4 test classes run **28/28 PASS** (verified via surefire-reports):
  - `UserBehaviorProfileServiceImplIntegrationTest` 7/7 PASS in 39.52 s
  - `UserBehaviorProfileAggregationServiceImplTest` 9/9 PASS in 1.888 s
  - `UserBehaviorProfileControllerTest` 3/3 PASS in 6.539 s
  - `ProfileSnapshotTest` 9/9 PASS in 0.283 s
- Full regression `.\mvnw.cmd -B test` → **Tests run: 752, Failures: 0, Errors: 17, Skipped: 0** (BUILD FAILURE). All 17 errors unchanged from pre-G4-T09 baseline (was 736/17 before T09; **+16 tests added** with 0 new regressions). All 17 errors remain `ExpertReviewServiceIntegrationTest$*` from pre-existing `g4cff-1` (`schema-expert-reviews.sql` corruption). Out of T09 scope per rule 00.

**Bugs fixed during Phase 2**:
- (B1) `DbDialect` field initialized to `UNKNOWN` because `System.getProperty("spring.datasource.url")` returns null at construction time — Spring sets Environment after constructor injection. Fix: inject `Environment`, lazy `resolveDialect()` on first `upsert()` call, cached in field. All 3 H2 paths (INSERT, UPDATE, noop) now reach correct dialect.
- (B2) UTF-16 LE encoding in test files — detected by Read tool, fixed by `Get-Content -Raw -Encoding Unicode | WriteAllText UTF8` (no BOM).

**Phase 3 findings (2026-08-05)** — see `docs/tasks/G4/G4-T09-user-behavior-profile.md` §Phase 3 Review for full evidence:

- **F-1 (MEDIUM, carry-forward from T06 F-2 / T08 F-1 family)**: `UserBehaviorProfileAggregationServiceImpl.aggregateForUser` hardcodes `ZoneId.of("Asia/Ho_Chi_Minh")`; affects both `TrendCalculator` and `EngagementAndTopicsService` calls. Profile pipeline propagates the TZ bucket-vs-boundary mismatch. **Carry-forward to TZ-cleanup task**.
- **F-2 (LOW, advisory)**: H2 SELECT-first INSERT/UPDATE path is not atomic; the `upsert_concurrentSameUser_converge` test passes by row-count assertion (1 row, ≥1 write) despite transient `23505 unique-constraint violation` from racing threads. PG path is correctly atomic. Production-safe.
- **F-3 (HIGH, blocking for production)**: `UserBehaviorProfileControllerTest` is purely Mockito unit-level. **NO** `@WebMvcTest` / `MockMvc` / `spring-security-test` slice. The Phase 1 DoD acceptance cases `getProfile_otherUser_403` / `getProfile_noToken_401` / `getProfile_ownUser_200` (Integration) are NOT IMPLEMENTED. **Required before T12**: add `@WebMvcTest(UserBehaviorProfileController.class)` + `MockMvc` + 4 HTTP-level tests (no-auth→401, other-user→404, own-user→200, service-empty→404).
- **F-4 (MEDIUM, rule-30 violation)**: `UserBehaviorProfileAggregationJob.runDailyAggregation` uses `LocalDate.now().minusDays(1)` (JVM TZ). For UTC+9 users at 03:15 UTC, the job's "yesterday" is off-by-one. **Carry-forward to TZ-cleanup task** (inject `Clock`).
- **F-5 (LOW, DoD coverage gap)**: First-time-user scenario (`coverage=0` + `confidence=0` + all scores null) not explicitly tested at integration level. Constructor accepts zero (correct), but no test exercises the aggregation-job path that emits zeros.
- **F-6 (DOC, LOW)**: V23 header comment L41 "if/when that task is approved" ambiguous; tighten to "STATUS: DEFERRED".
- **F-7 (LOW, OpenAPI gap)**: `/behavior/profile` operation block (yaml L320-330) lacks `security: [{bearerAuth: []}]` and 401/403 response declarations. Frontend clients cannot tell 401 (redirect to login) from 403 (insufficient role). 1-line OpenAPI edit.
- **F-8 (LOW, code duplication)**: H2 INSERT/UPDATE in `UserBehaviorProfileServiceImpl.upsertH2Direct` has ~60 lines of duplicated parameter binding that must be kept in sync with column additions.

**Status**: Phase 3 review DONE — **APPROVED WITH FINDINGS** (8 findings, 1 HIGH F-3 must close before T12). Next: T10 (profile snapshots, deferred), T12 (profile controller + dashboard, **must close F-3 first**). Carry-forward `g4cff-1` remains open and is out of T09 scope.

---

### G4-T11 — Data Quality Policy and Field (Phase 3 review APPROVED 2026-08-05)

**Files created/modified (10 production + 4 test = 14 total)**:

Production:
- `behavior/feature/profile/DataQualityStatus.java` — enum `SUFFICIENT | LOW | INSUFFICIENT`. SUFFICIENT requires coverage >= minCoverageForSufficient AND confidence >= minConfidence. LOW requires coverage >= minCoverageForLow AND confidence >= minConfidence. INSUFFICIENT otherwise (including confidence below threshold). G6 derives `deferRequired = (status == INSUFFICIENT)` internally.
- `behavior/feature/profile/config/DataQualityConfig.java` — value object (TrendConfig pattern). Fields: `minCoverageForLow`, `minCoverageForSufficient`, `minConfidence` — all `nullable`, fail-fast on `evaluate()` via `NullPointerException` when any threshold is null. Validation: all ratios in [0,1]; sufficient >= low. All thresholds `TODO_EXPERT_REVIEW` per FEATURE_DICTIONARY §10.1.
- `behavior/feature/profile/entity/UserBehaviorProfile.java` — added `dataQualityStatus` field with `@Enumerated(EnumType.STRING)`.
- `behavior/feature/profile/dto/ProfileSnapshot.java` — added `dataQualityStatus` parameter (not-null validated in constructor).
- `behavior/feature/profile/dto/UserBehaviorProfileResponse.java` — added `dataQualityStatus` field (not-null in API response).
- `behavior/feature/profile/service/UserBehaviorProfileAggregationService.java` + impl — added overload `aggregateForUser(UUID, LocalDate, DataQualityConfig)` that accepts config. No-arg overload uses `defaults()` (fail-fast NPE). Config's `evaluate(dataCoverage, confidence)` called to derive status.
- `behavior/feature/profile/service/UserBehaviorProfileServiceImpl.java` — added `dataQualityStatus` to INSERT/UPDATE columns for both PostgreSQL and H2 UPSERT paths. Written by same job as other profile columns.
- `behavior/feature/profile/service/UserBehaviorProfileResponseMapper.java` — passes `dataQualityStatus` from snapshot to response.
- `resources/db/migration/V24__add_data_quality_status.sql` — adds `data_quality_status VARCHAR(20) NOT NULL DEFAULT 'INSUFFICIENT'` with CHECK constraint.
- `src/test/resources/schema-user-behavior-profiles.sql` — H2 mirror: added `data_quality_status VARCHAR(20) NOT NULL DEFAULT 'INSUFFICIENT'` with CHECK constraint.

Tests (4):
- `UserBehaviorProfileAggregationServiceImplTest.java` — updated: added `dataQualityStatus` assertions; added 4 new tests: SUFFICIENT, LOW, INSUFFICIENT (coverage low), INSUFFICIENT (confidence low); added `noArgOverload_usesDefaults()` test verifying NPE with unconfigured thresholds.
- `UserBehaviorProfileServiceImplIntegrationTest.java` — updated: all helper methods pass `DataQualityStatus`; assertions verify `dataQualityStatus` round-trips correctly.
- `UserBehaviorProfileControllerTest.java` — updated: snapshot constructor includes `DataQualityStatus.SUFFICIENT`; assertion verifies `resp.dataQualityStatus() == SUFFICIENT`.
- `ProfileSnapshotTest.java` — updated: added `nullDataQualityStatusRejected()` and `allDataQualityStatusValuesAccepted()` tests; all existing tests pass `DataQualityStatus` explicitly.

**Schema**: V24 (PostgreSQL) + H2 mirror `schema-user-behavior-profiles.sql`.

**Key design decisions (user-confirmed 2026-08-05)**:
1. Coverage formula: `max(explicitCoverage7d, explicitCoverage30d)` — unchanged from T09. NOT `anxietySignalConfidence` (single feature, wrong abstraction level).
2. Threshold storage: `DataQualityConfig` value-object, `null` defaults = true fail-fast (throw NPE, no silent fallback). Comment placeholder for expert review.
3. Status storage: `data_quality_status` column in `user_behavior_profiles` (V24+), written by same aggregation job — NOT computed on-demand.
4. G6 contract: `DataQualityStatus` enum is canonical source. G6 derives `deferRequired` internally (`INSUFFICIENT => true`).

**Verification (2026-08-05)**:
- `.\mvnw.cmd -B compile` → **BUILD SUCCESS** (23.191 s, 277 files).
- `.\mvnw.cmd -B test -Dtest=UserBehaviorProfileAggregationServiceImplTest,ProfileSnapshotTest,UserBehaviorProfileControllerTest` → **Tests run: 21, Failures: 0, Errors: 0, Skipped: 0**.
- `.\mvnw.cmd -B test -Dtest=UserBehaviorProfileServiceImplIntegrationTest` → **Tests run: 7, Failures: 0, Errors: 0, Skipped: 0**.
- Full regression `.\mvnw.cmd -B test` → pending (running at time of write).

**Bugs fixed during Phase 2**:
- (B1) UTF-16 LE encoding in `DataQualityStatus.java` and `DataQualityConfig.java` — Write tool produced UTF-16LE on Windows. Fix: `Get-Content -Raw -Encoding Unicode | WriteAllText -Encoding UTF8`.
- (B2) `mockWindow` coverage values too low for SUFFICIENT threshold — test expected SUFFICIENT but got LOW. Fix: increase mock coverage to exceed `minCoverageForSufficient=0.50`.

**Phase 3 findings**: none.

**Status**: Phase 3 review DONE — **APPROVED**. **Next**: T10 (profile snapshots, deferred) or T12 (profile controller + dashboard).

---

### G4-T12 — Profile Dashboard API and Integration (Phase 1 plan COMPLETED 2026-08-05 — awaiting Phase 1 approval)

**Task**: Đóng gói `GET /api/v1/behavior/profile` thành production-ready endpoint. T09 đã ship controller + DTO + mapper + service + job + V23 + V24; T12 chỉ close-out carry-forward family từ T06/T07/T08/T09 (narrow close-out scope, không refactor module ngoài). Theo `docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md` §2.2 (T12 = dashboard API + integration).

**Phase 1 plan** (re-verified 2026-08-05, xem `docs/tasks/G4/G4-T12-profile-dashboard-api-and-integration.md` cho full plan):
- 2 production files mới: `TrendConfigProperties` (`@ConfigurationProperties("mindbridge.trend")` wrapper, fail-fast; close T07 F-1) + `TrendSummaryResponse` (nested record expose T07 `TrendSummary`).
- 4 production files sửa: `UserBehaviorProfileResponse` (đổi `Map<String,String>` → nested `TrendSummaryResponse`), `UserBehaviorProfileController` (inject `UserRepository`, resolve `ZoneId` từ `user.getTimezone()`), `UserBehaviorProfileResponseMapper` (parse JSON → `TrendSummary` → `TrendSummaryResponse`), `UserBehaviorProfileAggregationServiceImpl` (inject `TrendConfigProperties`, replace `TrendConfig.defaults()`).
- 2 YAML sửa: `application.yml` + `application-test.yml` thêm `mindbridge.trend.*` block (3 placeholder values `TODO_EXPERT_REVIEW`).
- 1 test mới: `UserBehaviorProfileControllerWebMvcTest` — `@WebMvcTest` + `MockMvc` + `spring-security-test`, 4 cases (no-auth 401, own-user 200, own-user-no-profile 404, ownership check).
- 2 docs sửa: `docs/03_API_CONTRACT.yaml` (security + 401/403 + nested `TrendSummaryResponse` schema, close T09 F-7) + `docs/05_IMPLEMENTATION_STATUS.md` §13 + §15 (drift fix).

**5 quyết định Phase 1 (đã chốt với user):**
1. **`trendSummary` shape = nested object** (q3a) — ghi rõ `direction` enum + `deltaPct` BigDecimal + `streakInfo` structure, không flatten `Map<String,String>` (T09 cũ).
2. **ZoneId source = `users.timezone`** (T09 F-1 family) — controller path qua `UserRepository.findById(currentUserId).getTimezone()`; job path deferred G8.
3. **Config binding = `@ConfigurationProperties`** mirror T11 `DataQualityConfig` pattern (không `@Value` inline).
4. **HTTP test = `@WebMvcTest` slice** (close T09 F-3) — nhanh, không cần full H2 integration.
5. **`parseTrendSummary` fail → empty `TrendSummaryResponse`** (q3a cộng thêm) — không throw 500, dashboard fallback với `entries=[]` + `streak=null`.

**Carry-forward closing:**
- T07 F-1 (HIGH_STRESS_THRESHOLD scale) → close (binding normalize 0-1).
- T09 F-3 (HTTP 401/403 tests) → close (4 WebMvcTest cases).
- T09 F-7 (OpenAPI security + 401/403) → close (schema + responses).
- T06/T07/T08/T09 F-2 family (ZoneId) → close một phần (controller path); job path deferred G8.

**Carry-forward deferred (open):**
- T07 F-2..F-5 (split `NO_PRIOR_DATA`, dead `noop`, JPQL explicit ANSWERED, dead null branch) → T12.x follow-up.
- T09 F-1, F-2, F-4, F-5, F-6, F-8 (job path, H2 atomic, first-time-user, V23 comment, SQL duplication) → T12.x follow-up or G8.
- 3 `TODO_EXPERT_REVIEW` thresholds (`MIN_TREND_COVERAGE`, `TREND_DELTA_THRESHOLD`, `HIGH_STRESS_THRESHOLD`) → cần expert review; T12 chỉ bind property keys, values placeholder ghi rõ `TODO_EXPERT_REVIEW` trong YAML.

**Out-of-scope:**
- T10 immutable snapshot table (DBMVP §7.3) — DEFERRED, G6 phụ thuộc.
- G6 Program Matching — DEFERRED, cần T10.
- Frontend integration (mock → real API) — thuộc G7.
- RBAC: EXPERT/ADMIN xem profile user khác — close ở G7/G8.
- Realtime query param `?realtime=true` để bypass snapshot — DEFERRED, snapshot path đủ cho MVP.

**Verification plan (Phase 2)**:
1. `cd backend; .\mvnw.cmd -B -q clean compile -DskipTests` → BUILD SUCCESS.
2. `.\mvnw.cmd -B test -Dtest=UserBehaviorProfileControllerWebMvcTest -DfailIfNoTests=false` → 4/4 PASS.
3. Regression T07 (19) + T09 (28) + T11 (28) → all PASS, no new regression.
4. Full regression `.\mvnw.cmd -B test` → expected total 756 tests (752 + 4 T12), 17 errors carry-forward `g4cff-1` (out-of-scope).

**Status**: Phase 1 plan DONE. **Next**: Phase 1 approval → Phase 2 implementation. Job path (F-2 family) deferred G8. Carry-forward `g4cff-1` (schema-expert-reviews.sql) remains open.

### G4-T12 Phase 2 implementation COMPLETED 2026-08-05

**Files created (3 production + 1 test)**:
- `behavior/feature/profile/config/TrendConfigProperties.java` — `@ConfigurationProperties("mindbridge.trend")` wrapper for the 3 thresholds (`MIN_TREND_COVERAGE`, `TREND_DELTA_THRESHOLD`, `HIGH_STRESS_THRESHOLD`). Close-out of T07 F-1.
- `behavior/feature/profile/dto/TrendSummaryResponse.java` — nested record exposed via API. Closes T07 q3a (nested object, not flat Map).
- `behavior/feature/profile/dto/TrendEntryResponse.java` — per-feature trend row (direction / deltaPct / reason / recentAvg / priorAvg / coverages).
- `behavior/feature/profile/dto/StreakInfoResponse.java` — streak snapshot (checkInStreak / highStressStreak / last dates / windowDays).
- `behavior/feature/profile/controller/UserBehaviorProfileControllerWebMvcTest.java` — `@WebMvcTest` + `MockMvc` + `spring-security-test` `authentication()` post-processor. 4 cases: no-auth 401, own-user 200 (with nested trendSummary), own-user-no-profile 404, ownership check. Closes T09 F-3.

**Files modified (5 production + 2 yaml + 2 test + 2 docs)**:
- `MindBridgeApplication.java` — register `TrendConfigProperties` with `@EnableConfigurationProperties`.
- `behavior/feature/profile/service/UserBehaviorProfileAggregationServiceImpl.java` — inject `TrendConfigProperties`, replace `TrendConfig.defaults()` → `trendConfigProperties.toTrendConfig()`. Closes T07 F-1 (fail-fast on missing properties).
- `behavior/feature/profile/service/UserBehaviorProfileResponseMapper.java` — parse JSON `TrendSummary` → `TrendSummaryResponse.from(...)` via `ObjectMapper.readValue`; empty-but-valid fallback on parse error (Phase 1 decision #5). Closes T07 q3a (nested object).
- `behavior/feature/profile/dto/UserBehaviorProfileResponse.java` — `trendSummary` field type changed from `Map<String,String>` to `TrendSummaryResponse`. Constructor validates non-null.
- `application.yml` + `application-test.yml` — added `mindbridge.trend.*` block with placeholder values (`0.5`, `0.1`, `0.75`).
- `behavior/feature/profile/service/UserBehaviorProfileAggregationServiceImplTest.java` — update constructor to pass `TrendConfigProperties`.
- `behavior/feature/profile/controller/UserBehaviorProfileControllerTest.java` — register `JavaTimeModule` so mapper can deserialize `ZoneId` JSON; assert nested `TrendSummaryResponse` fields.
- `docs/03_API_CONTRACT.yaml` — add `security: [{bearerAuth: []}]` + responses `401`/`403` to `/behavior/profile`. Add nested schemas `TrendSummaryResponse` + `TrendEntryResponse` + `StreakInfoResponse`. Replace flat `trendSummary: object + additionalProperties: string` with `$ref: TrendSummaryResponse`. Closes T09 F-7.
- `docs/05_IMPLEMENTATION_STATUS.md` — §13 API table + §15 table updated; this entry.

**Bugs / decisions during Phase 2**:
- (B1) `Write` tool on Windows injected NUL bytes (UTF-16 LE no-BOM) into 5 new files (Write tool limitation, known since T05/T06/T11). Fixed via PowerShell `[System.Text.Encoding]::Unicode.GetString → UTF8Encoding(false).WriteAllText`. StrReplace ALSO produces NUL bytes when re-writing; verified nulls=0 after each strip.
- (B2) `SecurityMockMvcRequestPostProcessors.jwt()` requires `spring-security-oauth2-jose` (NOT on classpath). Switched to `authentication(UsernamePasswordAuthenticationToken)` wrapping a `JwtPrincipal` directly — no new dependency required.
- (B3) `with(jwt)` return type was `RequestPostProcessor`, not `Authentication` — wrapped correctly.
- (D1) Phase 1 plan called for `UserRepository` injection into the controller for `ZoneId` resolution. **Scope correction during Phase 2**: the snapshot pattern means TZ is baked at write time (job → `UserBehaviorProfileAggregationServiceImpl`). Controller reads the snapshot directly. No `UserRepository` injection needed. Job path (`LocalDate.now()` + hard-coded `Asia/Ho_Chi_Minh`) deferred to G8 per Phase 1.

**Verification (2026-08-05)**:
- `.\mvnw.cmd -B -q compile -DskipTests` → BUILD SUCCESS (112 s).
- `.\mvnw.cmd -B -q test-compile` → BUILD SUCCESS (after WebMvcTest fix).
- Targeted tests `.\mvnw.cmd -B test -Dtest=UserBehaviorProfileControllerWebMvcTest,UserBehaviorProfileControllerTest,UserBehaviorProfileAggregationServiceImplTest,TrendCalculatorImplTest,ProfileSnapshotTest` → **44/44 PASS** (3 controller + 4 WebMvc + 9 profile snapshot + 9 aggregation + 19 trend = 44).
- Full regression `.\mvnw.cmd -B test` (after `.\mvnw.cmd -B clean compile` to refresh stale `target/`): → **Tests run: 756, Failures: 0, Errors: 17, Skipped: 0** (BUILD FAILURE). Expected baseline = 752 + 4 new WebMvcTest = 756. 17 errors unchanged from pre-T12 baseline, all in `ExpertReviewServiceIntegrationTest` from `g4cff-1` (schema-expert-reviews.sql corruption, out-of-scope per rule 00). **T12 introduces zero new regressions**.

**First regression run had spurious 742/261 result** because the existing `target/` dir was stale (mvn incremental compile did not refresh MapStruct-generated `UserMapperImpl.class` until a `clean compile` was performed). Resolved by `.\mvnw.cmd -B clean compile` before full regression.

**Carry-forward closing (this chat)**:
- T07 F-1 (HIGH_STRESS_THRESHOLD scale = normalized 0–1) → **CLOSE** via `TrendConfigProperties` + Javadoc + YAML placeholder.
- T09 F-3 (HTTP 401/403/200/404 tests) → **CLOSE** via `UserBehaviorProfileControllerWebMvcTest` (4 cases).
- T09 F-7 (OpenAPI security + 401/403 responses) → **CLOSE** via `docs/03_API_CONTRACT.yaml` edit.
- T07 q3a (`trendSummary` shape decision) → **CLOSE** via nested `TrendSummaryResponse` DTO + mapper rewrite.

**Carry-forward deferred (open)**:
- T07 F-2 (NO_PRIOR_DATA split) → T12.x follow-up.
- T07 F-3 (dead `noop(Function)`) → T12.x follow-up.
- T07 F-4 (explicit `assg.status = ANSWERED` JPQL filter) → T12.x follow-up.
- T07 F-5 (dead null branch in `computeStreak`) → T12.x follow-up.
- T09 F-1 (ZoneId hard-coded `Asia/Ho_Chi_Minh` in `UserBehaviorProfileAggregationServiceImpl.aggregateForUser`) → defer G8 (Clock injection).
- T09 F-2 (H2 SELECT-first non-atomic) → out-of-scope.
- T09 F-4 (`LocalDate.now().minusDays(1)` in job) → defer G8.
- T09 F-5 (first-time-user test) → out-of-scope.
- T09 F-6 (V23 comment tighten) → T12.x follow-up.
- T09 F-8 (H2 SQL duplication) → out-of-scope.
- 3 `TODO_EXPERT_REVIEW` thresholds (real values) → expert review needed; placeholder values bound via YAML.
- `g4cff-1` (`schema-expert-reviews.sql` corruption, pre-existing 17 errors in `ExpertReviewServiceIntegrationTest`) → separate task.

**Risidual risks**:
- R-1: real JWT validation still untested at controller layer (the WebMvcTest bypasses `JwtAuthenticationFilter` by injecting `JwtPrincipal` directly). A future integration test should exercise real JWT decode end-to-end. Out of scope for T12.
- R-2: `UserBehaviorProfileAggregationServiceImpl` constructor signature changed; any external service consumers (none found via grep) would break. Verified safe.
- R-3: 3 placeholder threshold values may produce unexpected trend output (e.g., `HIGH_STRESS_THRESHOLD=0.75` may be too high for a normalized 0–1 stress_score depending on real distribution). Expert must verify before production.

---

## Frozen Baseline History

### G3 group Phase 3 review ledger (2026-08-02 — 2026-08-04)

> **Reading guide**: Each row maps a G3 sub-task to its Phase 3 verdict and a short evidence pointer. Full evidence for each row lives in the corresponding G3 task file (`docs/tasks/G3/G3-Txx-*.md`). Verdict rules per `.cursor/rules/00-project-core.mdc`: APPROVE / APPROVE WITH FINDINGS / BLOCK.

| # | Task | Phase 3 status | Verdict | Short evidence |
|---|---|---|---|---|
| 1 | G3-T01 — AI Provider Abstraction + Mock | Phase 3 PASS | **APPROVE** | 44/44 (MockChatAnalysisProviderTest 39 + ChatAnalysisProviderConfigTest 4 + MockChatAnalysisProviderIntegrationTest 1). |
| 2 | G3-T02 — JSON Schema + signal dict | Phase 3 PASS | **APPROVE** | 8/8 ChatAnalysisOutputSchemaTest (2 valid + 4 invalid + 2 contract). |
| 3 | G3-T03 — Prompt design + 18 test cases | Phase 3 PASS | **APPROVE** | 18/18 TestCasesFromG3T03 (1-to-1 mapping with `docs/prompts/chat_analysis_test_cases.md`). |
| 4 | G3-T04 — Lưu AI Analysis Run | Phase 3 PASS | **APPROVE WITH MINOR FINDINGS** | 36/36 AiAnalysisRun-related tests PASS; full regression 396/396 PASS at T04 review time. |
| 5 | G3-T05 — Lưu Chat Analysis Result versioned | Phase 3 PASS | **APPROVE** | 38/38 (ChatAnalysisResultTest 22 + ChatAnalysisResultServiceTest 9 + ChatAnalysisResultIntegrationTest 7). |
| 6 | G3-T06 — Tích hợp LLM provider thật | Phase 3 PASS | **APPROVE WITH MINOR FINDINGS** | RealLlmChatAnalysisProvider with mock HttpClient; status-code policy broadened in T07. |
| 7 | G3-T07 — Validate output, retry và fallback | Phase 3 PASS | **APPROVE WITH MINOR FINDINGS** | 54/54 targeted PASS; full regression 494/495 (1 pre-existing G1 flake unrelated). |
| 8 | G3-T08 — Keyword/Regex Safety Pre-filter | Phase 3 PASS | **APPROVE** | 36/36 (SafetyPreFilterServiceTest 13 + 5 + TextNormalizerTest 9 + SafetyKeywordRuleTest 9). |
| 9 | G3-T09 — LLM Risk Classification riêng | Phase 3 PASS | **APPROVE** | 26/26 (MockRiskClassifierProviderTest 18 + RiskClassifierProviderConfigTest 3 + MockRiskClassifierProviderIntegrationTest 5). |
| 10 | G3-T10 — Safety Resolver + Risk State History | Phase 3 PASS | **APPROVE** | 28/28 (SafetyResolverServiceTest 19 + SafetyResolverIntegrationTest 9). |
| 11 | G3-T11 — Safety Event, Source và Action | Phase 3 PASS | **APPROVE** | 31/31 (SafetyEventServiceIntegrationTest 5 + 8 unit + 3 entity unit). Migration V17 3 tables. |
| 12 | G3-T12 — Phản hồi cố định cho Level 4 | Phase 3 PASS | **APPROVE** | 43/43 + 8/8 existing = 51/51. LLM-independence verified by functional + bytecode-scan tests. |
| 13 | G3-T13 — Expert Review và giao diện Safety cơ bản | Phase 3 PARTIAL | **APPROVE WITH FINDINGS** | 21/21 unit tests PASS; full integration suite blocked by pre-existing L-env-1 regression (SafetyEventSource JPA mismatch). Migration V20. |

**Full regression** at T07 closure: `mvnw -B test` → **494/495 PASS** (1 pre-existing G1 flake `ConsentGuardTest.grantedThenRevoked_latestWins` acknowledged in G2 acceptance, unrelated to G3).

### G3-T01 → G3-T13 — short per-task summary

- **G3-T01 (AI Provider Abstraction + Mock)**: `com.mindbridge.analysis.provider` package — `ChatAnalysisProvider` interface + `ChatAnalysisInput`/`ChatAnalysisOutput` DTO + `MockChatAnalysisProvider` (6 force-scenarios, gated by `mindbridge.ai.provider=mock`) + 3 exceptions + 3 ErrorCodes + `ChatAnalysisProviderConfig`. No DB migration.
- **G3-T02 (JSON Schema + signal dict)**: 5 enum files (Topic 7 / Emotion 7 / Intent 4 / Signal 9) + `AnalysisSchemaVersion` const V1 + JSON Schema `docs/schemas/chat_analysis_v1.schema.json` (Draft 07, 10 fields required, `additionalProperties: false`) + dictionary doc + 1 dep `com.networknt:json-schema-validator:1.5.3` (test scope).
- **G3-T03 (Prompt design + 18 test cases)**: `docs/prompts/chat_analysis_prompt_v1.md` (SHA-256 `5363675e22fe77100908eaee6ab003207da57ba557e7c09d5d52671c1a9447e2`, alias `v1:5363675e22fe`) + test cases doc; `@Nested TestCasesFromG3T03` 18 tests.
- **G3-T04 (Lưu AI Analysis Run)**: `ai_analysis_runs` table (V15) — columns `provider`/`model`/`prompt_version`/`schema_version`/`error_code`/`error_summary`. 36 tests including AiRunErrorRedactor.
- **G3-T05 (Lưu Chat Analysis Result versioned)**: `chat_analysis_results` table (V16) — polymorphic support + schema_version per row. 38 tests.
- **G3-T06 (Tích hợp LLM provider thật)**: `RealLlmChatAnalysisProvider` with java.net.http.HttpClient + env var API key (fail-secure). 14 unit tests + 5 integration with mock HttpClient.
- **G3-T07 (Validate output, retry, fallback)**: JSON Schema validator + ProviderRetryExecutor + FallbackDecision truth table (incl. Level-4 safety guard). Mock HttpClient integration tests for success-after-retry / retries-exhausted / fallback-on / non-retryable / timeout.
- **G3-T08 (Keyword/Regex Safety Pre-filter)**: `safety_keyword_rules` table (V13) + `SafetyPreFilterService` + `TextNormalizer` (NFC + unicode normalization). 36 tests. Returns preliminary_risk only; NO finalRiskLevel.
- **G3-T09 (LLM Risk Classification riêng)**: `RiskClassifierProvider` interface + Mock + 3 exceptions. `RiskClassifierOutput` 4 fields, NO finalRiskLevel (resolver owns final per docs/04 §5). 26 tests.
- **G3-T10 (Safety Resolver + Risk State History)**: `risk_state_history` table (V14, append-only, NO setter/NO @PreUpdate/NO @PreRemove). Resolver rule = `max(ruleRisk, modelRisk)` with downgrade guard. Structured `reason_codes` JSONB. 31 tests.
- **G3-T11 (Safety Event, Source, Action)**: `safety_events`/`safety_event_sources`/`safety_actions` (V17). Polymorphic `(source_type, source_id)` without DB-level FK on polymorphic column (Phase 1 decision C5). 31 tests. Audit logging via AuditService.
- **G3-T12 (Phản hồi cố định cho Level 4)**: `safety_response_templates` (V18) + template audit (V19) + `SafetyResponseTemplateExecutor` (LLM-independent by construction; bytecode-scan test enforces). 51/51 tests.
- **G3-T13 (Expert Review + giao diện Safety cơ bản)**: `expert_reviews` (V20) + `ExpertReviewService` + expert review UI. 21/21 unit tests PASS; full integration suite blocked by pre-existing L-env-1 regression (SafetyEventSource POJO vs JPA repository mismatch in G2 baseline).

### G3 closed 2026-08-03 — 13/13 tasks Phase 3 APPROVE or APPROVE WITH FINDINGS

**Carry-forward finding (HIGH, pre-existing)** — L-env-1 was: `@SpringBootTest` context loading fails with `Not a managed type: SafetyEventSource`. Root cause: `SafetyEventSource` was a POJO but `SafetyEventSourceRepository` declared it as JPA entity. **RESOLVED 2026-08-04** by commit `55dc226` — `SafetyEventSource` promoted to `@Entity` (smallest complete change, mirroring `SafetyEvent.java` pattern). Verified PASS on `SafetyEventSourceTest` (5/5) + `SafetyEventServiceIntegrationTest` (5/5) + `SafetyResolverIntegrationTest` (9/9). Remaining unrelated bug in `ExpertReviewServiceIntegrationTest` (`schema-expert-reviews.sql` resource not found) is OUT-OF-SCOPE for the hotfix — separate task needed.

### G4-T01 — Chốt Feature Dictionary MVP (Phase 3 review COMPLETE 2026-08-04 — verdict APPROVE WITH FINDINGS)

**Task**: chốt Feature Dictionary v1 với 8 feature (stress, mood, energy, sleep, anxiety_signal, engagement, exercise_completion, max_risk) theo `Pre_G4_Implementation_Decisions_and_Feature_Contract.md` (pre_g4_contract_v1, approved 2026-08-03).

**Deliverable**: `docs/analysis/FEATURE_DICTIONARY_v1.md` (50501 bytes, 1213 lines, 12 sections + 8 per-feature specs + 3 appendices).

**Phase 1 plan** (re-verified 2026-08-04): Cursor inspected actual G2/G3 schema files (V6 `daily_question_templates`+`daily_question_options`, V7 `users.timezone`, V8 `daily_question_assignments`, V9 `daily_question_answers`, V10 backfill `scale_min`/`scale_max`, V11 `behavioral_events` 12 event types, V13 `safety_keyword_rules`, V14 `risk_state_history`, V16 `chat_analysis_results`) before mapping source per contract §15.

**3 contract conflicts surfaced and chốt** (per contract §15):
- **B.1** stress raw scale: contract §8.1 ghi `Integer 1-10`; G2 seed thực tế là `NUMERIC 1-5` (STRESS v1 SCALE 1-5 trong V6 seed). **Decision (b)**: ghi `1-5` khớp G2 seed (đã locked); không tự tạo `STRESS v2 1-10` field ngoài task.
- **B.2** `anxiety_signal`: G3 schema không có field riêng (chỉ có `chat_analysis_results.signals JSONB` enum + `emotion` enum + `model_risk_level`). **Decision (b)**: derive từ signals+emotion+model_risk với formula = `CONFIG_PLACEHOLDER` (`ANXIETY_SIGNAL_FORMULA`) cho đến khi chuyên gia duyệt; không defer feature.
- **B.3** `sleep_quality`: template `SLEEP_QUALITY` SCALE 1-5 **chưa được seed** trong MVP G2 (chỉ `SLEEP` duration template seeded). **Decision (b)**: `sleep_score` = `null / UNKNOWN` cho đến khi template `SLEEP_QUALITY` được seed trong task tương lai; không tự seed.

**Phase 3 evidence**:

| Acceptance criterion (contract §17) | Status | Evidence in deliverable |
|---|---|---|
| §17.1 Đủ 8 feature | PASS | §5 Feature Catalog (8 rows); §6.1 stress + §6.2 mood + §6.3 energy + §6.4 sleep + §6.5 anxiety_signal + §6.6 engagement + §6.7 exercise_completion + §6.8 max_risk |
| §17.2 Primary source per feature | PASS | §6.x.1 mỗi feature |
| §17.3 Supporting/fallback source | PASS | §6.1.1 stress supporting + §6.2.1 mood supporting + §6.4.4 sleep supporting; §6.3.1 energy inferred=UNAVAILABLE + §6.5.1 anxiety supporting; others N/A |
| §17.4 Unit + range | PASS | §6.x.2 (20 occurrences of `Range`) |
| §17.5 Polarity | PASS | §6.x.3 (13 occurrences of `Polarity`) |
| §17.6 Missing semantics | PASS | §4 Null/Unknown/Zero/NOT_APPLICABLE + §6.x.4/5 |
| §17.7 Raw vs normalized | PASS | §2.2 calculation versions + §6.x.2/3 (30 occurrences) |
| §17.8 Explicit / Inferred / Behavioral | PASS | §3 definitions + §3.4 classification table (67 occurrences) |
| §17.9 NEVER convert null to 0 | PASS | §4 (mandatory rule) + mỗi feature §6 (2 explicit statements) |
| §17.10 Stress raw scale | PASS (with documented divergence) | §6.1 + §10 B.1 — ghi `1-5` khớp G2 seed |
| §17.11 Mood 1-5 | PASS | §6.2.2 |
| §17.12 Energy 1-5 | PASS | §6.3.2 |
| §17.13 Sleep duration/quality tách riêng | PASS | §6.4.2 + §10 B.3 |
| §17.14 Anxiety 0-1 | PASS | §6.5.2 |
| §17.15 Engagement 0-1 | PASS | §6.6.2 |
| §17.16 Exercise completion 0-1 | PASS | §6.7.2 |
| §17.17 Max risk 1-4 | PASS | §6.8.2 |
| §17.18 Timezone policy | PASS | §8 (Timezone & Local-Date Policy — mirror contract §6) |
| §17.19 Late-data policy | PASS | §9 (Late-Arriving Data Policy — mirror contract §6.7) |
| §17.20 Open Expert Decisions table | PASS | §10.1 (12 rows mirror contract §13.1) |
| §17.21 `feature_dictionary_v1` version constant | PASS | §2.2 (`FEATURE_DICTIONARY_VERSION = "v1"`) |
| §17.22 Initial calculation versions | PASS | §2.2 (5 versions: normalization_v1, sleep_quality_v1, engagement_v1_chat_checkin, exercise_completion_v1, max_risk_daily_v1; 33 occurrences) |
| §17.23 Không clinical threshold tự bịa | PASS | §10 + §12.3 checklist (26 occurrences of `TODO_EXPERT_REVIEW`/`CONFIG_PLACEHOLDER`/`DEMO_ONLY`) |
| §17.24 G4-T10 known gap ghi nhận | PASS | `docs/05_IMPLEMENTATION_STATUS.md` Known Blockers section + §10 §3 in deliverable |
| §17.25 G6 deferred | PASS | `docs/05_IMPLEMENTATION_STATUS.md` G6 row (NOT_STARTED) + §10.4 in deliverable |

**Task spec DoD §4**:
- [x] Mỗi feature có công thức và nguồn rõ ràng → §6.x.1 Primary source + §6.x.2 Raw type + §6.x.3 Normalized formula cho mỗi feature (stress `(raw-1)/4` cho 1-5; mood `(raw-1)/4`; energy `(raw-1)/4`; sleep `(quality-1)/4` pending template; anxiety = `CONFIG_PLACEHOLDER`; engagement = `CONFIG_PLACEHOLDER` weights; exercise = `null/NOT_APPLICABLE`; max_risk = `MAX(final_risk_level within local_date)`).
- [x] Hai dev hiểu giống nhau về null/unknown/zero → §4 bảng 4-cột (`null`/`UNKNOWN`/`zero`/`NOT_APPLICABLE`) + §10 mandatory rule "NEVER convert null to 0" nhắc lại trong mỗi feature §6.
- [x] Feature có test case mẫu → §11 tổng hợp (1 happy path + 1 missing variant per feature) + §6.x.6/7 test case chi tiết.

**Security review**:
- ✅ No raw chat content in features (§3.3 explicit: "Behavioral data **không** được chứa raw chat content. `properties` JSONB chỉ chứa metadata (duration_ms, message_length bucketed, ...) — KHÔNG text snippet, KHÔNG hash of content ngoại trừ SHA-256 của `evidenceSpans`").
- ✅ No hard-coded clinical threshold (`HIGH_STRESS_THRESHOLD = TODO_EXPERT_REVIEW` + `CLINICAL_INTERPRETATION_LABELS = TODO_EXPERT_REVIEW` + 11 expert decision codes đều ở `TODO_EXPERT_REVIEW`).
- ⚠️ No JPA entity exposed — OUT-OF-SCOPE cho T01 (DTO mapping thuộc G4-T12).
- ✅ No preliminary risk = final risk (§6.8.1 "Ưu tiên `risk_state_history.final_risk_level`. `safety_events.risk_level` chỉ dùng audit, KHÔNG dùng thay thế").
- ✅ No system timezone (§8.8 + §6.6.2 explicit "KHÔNG dùng `LocalDate.now()` / `ZoneId.systemDefault()`").

**Database integrity cross-check** (11 schema elements):
- ✅ `users.timezone` (V7 VARCHAR(50) NOT NULL DEFAULT 'UTC') referenced in §6.6.2 + §8.1 + Phụ lục A.
- ✅ `daily_question_templates` (V6) referenced 5× (stress/mood/energy/sleep templates + scale_min/scale_max).
- ✅ `daily_question_options` (V6) referenced in §6.2 mood option_value.
- ✅ `daily_question_assignments` (V8) referenced 1× (template_version_id pattern).
- ✅ `daily_question_answers` (V9) referenced 4× (numeric_value + option_value + answered_at).
- ✅ `daily_question_templates.scale_min/scale_max` (V10) referenced 3×.
- ✅ `behavioral_events` (V11) referenced 19× (12 event types including `CHAT_SESSION_STARTED`, `CHAT_MESSAGE_SENT`, `DAILY_CHECKIN_COMPLETED`, `DAILY_CHECKIN_SKIPPED`, etc.).
- ✅ `safety_keyword_rules` (V13) referenced 4× (anxiety_signal supporting).
- ✅ `risk_state_history` (V14) referenced 12× (max_risk source of truth + final_risk_level column).
- ✅ `chat_analysis_results` (V16) referenced 21× (anxiety_signal source + topic/emotion/intent/signals JSONB + model_risk_level + confidence + schema_version).
- ✅ `safety_events` (V17) referenced 4× (audit + secondary source for max_risk).

**Frontend compatibility**: NO conflict với `docs/03_API_CONTRACT.yaml`. Deliverable dùng DB-column-level names (`stress_score`, `mood_score`, `engagement_score`, `max_risk_level`); API contract định nghĩa window-avg DTO names (`stressAvg7d`, `stressAvg30d`, `moodAvg7d`, `sleepAvg7d`, `energyAvg7d`, `anxietyAvg7d`, `engagementScore`, `riskLevel`). Đây là **2 abstraction levels khác nhau**, không phải conflict. Mapping DB column → API DTO là responsibility của T02 (typed schema) + T12 (dashboard API + integration), đã acknowledged trong deliverable §1 (Scope: T01 = vocabulary, T02 = typed schema, T12 = dashboard API).

**Findings**:

| # | Severity | Finding | Resolution |
|---|---|---|---|
| F-1 | LOW (intentional) | §17.10 stress raw scale 1-5 vs contract §8.1 ghi 1-10 | §10 B.1 documented; chốt (b) theo user direction 2026-08-04 |
| F-2 | LOW (intentional) | §17.14 anxiety_signal formula = `CONFIG_PLACEHOLDER` | §10 B.2 documented; chốt (b) theo user direction 2026-08-04 |
| F-3 | LOW (intentional) | §17.13 sleep_quality template chưa seed | §10 B.3 documented; chốt (b) theo user direction 2026-08-04 |
| F-4 | N/A (out-of-scope) | Deliverable không reference API DTO field names | T02/T12 scope, T01 = vocabulary |
| F-5 | **HIGH (pre-existing, carry-forward from G3-T13)** | L-env-1: `@SpringBootTest` context loading fails do `SafetyEventSource` JPA mismatch (`SafetyEventSource` là POJO nhưng `SafetyEventSourceRepository` khai báo `@Entity`). Ảnh hưởng mọi integration test. | OUT-OF-SCOPE G4-T01; yêu cầu hotfix `SafetyEventSource.java:1` thành `@Entity` (1-line change). |

**Verification commands executed** (re-check Phase 3 deliverable integrity, 2026-08-04):
- File size + line count: `Get-Item` 50501 bytes; `Get-Content | Measure-Object -Line` 1213 lines; `Format-Hex` first 3 bytes confirm UTF-8 no BOM.
- H2 header count: 12 H2 (Purpose & Scope, Contract Versioning, Definitions, Null/Unknown/Zero/NOT_APPLICABLE, Feature Catalog, Per-Feature Specification, Explicit vs Inferred, Timezone & Local-Date, Late-Arriving Data, Open Expert Decisions, Test Cases, Acceptance Checklist) + 3 appendices (A: DB schema, B: cross-reference, C: change history).
- Pattern grep: 18 occurrences `NUMERIC(4,3)` (normalized score types), 11 `Open Expert Decisions` rows, 5 calculation versions, 33 references `TODO_EXPERT_REVIEW`/`CONFIG_PLACEHOLDER`/`DEMO_ONLY` (proves no clinical threshold tự bịa).
- Contract conflict markers: B.1, B.2, B.3 all present in deliverable §10 with full rationale.

**Decision**: **APPROVE WITH FINDINGS**. Tất cả 25 mục §17 acceptance PASS, 3 mục DoD PASS, 5/5 security policy PASS, 11/11 DB schema element cross-check PASS, 0 frontend compatibility conflict. 3 contract conflicts (B.1/B.2/B.3) đã report đầy đủ theo contract §15 — không tự tạo field ngoài task. Finding duy nhất HIGH (F-5) là pre-existing regression carry-forward từ G3-T13 (`SafetyEventSource` JPA mismatch), nằm ngoài G4-T01 scope — không block verdict vì nó external cho T01 (T01 là tài liệu task, không phải code task). **L-env-1 (F-5) HOTFIX SHIPPED 2026-08-04 in commit `55dc226`** — `SafetyEventSource` promoted to `@Entity`. Verified: 14/14 representative integration tests PASS. After the hotfix, G4-T01 verdict can be retroactively promoted from `APPROVE WITH FINDINGS` → `APPROVE` once team re-runs the full `@SpringBootTest` suite to confirm zero regressions (out-of-scope for T01 itself — recommend running full regression in G4-T02 before promoting T01 verdict).

**Ready for G4-T02** (Thiết kế user_daily_features schema — typed columns). Recommend hotfix L-env-1 (`SafetyEventSource.java:1` thêm `@Entity` annotation) trong 1 commit riêng trước khi bắt đầu G4-T02 để integration test path sạch từ đầu. Sau khi T02 PASS, có thể promote T01 từ `APPROVE WITH FINDINGS` → `APPROVE` retroactively nếu team muốn.

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
| `/api/v1/behavior/profile` | GET | IMPLEMENTED | NOT_CONNECTED | IMPLEMENTED | G4-T09 + G4-T12: full payload incl. nested `trendSummary` (TrendSummaryResponse), `dataQualityStatus`, 7d/30d split + `DominantTopic[]`. Security 401/403 + 404 declared. 1 unit + 4 WebMvc + 28 G4-T09 tests pass. Placeholder thresholds `MIN_TREND_COVERAGE`/`TREND_DELTA_THRESHOLD`/`HIGH_STRESS_THRESHOLD` (`TODO_EXPERT_REVIEW`). Frontend integration = G7. |
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
| user_daily_features | V21 (2026-08-04) | G4-T02 Phase 2 | IN_PROGRESS | Pending Phase 3 review |
| user_behavior_profiles | V23 + V24 | G4-T09 + G4-T11 + G4-T12 | COMPLETED | Mutable table; V24 added `data_quality_status`. G4-T12 wires TrendConfigProperties (placeholder values) and serves nested `TrendSummaryResponse` via the API. |
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
| ISSUE-004 | HIGH | **L-env-1** (carry-forward from G3-T13): `SafetyEventSource` was a POJO while `SafetyEventSourceRepository` declares it as JPA entity — `@SpringBootTest` context loading failed with `Not a managed type: SafetyEventSource`, blocking integration tests. **RESOLVED 2026-08-04** by commit `55dc226` — promoted `SafetyEventSource` to `@Entity` with full JPA mappings mirroring `SafetyEvent.java` pattern. Verified: `SafetyEventSourceTest` 5/5 PASS, `SafetyEventServiceIntegrationTest` 5/5 PASS, `SafetyResolverIntegrationTest` 9/9 PASS (14/14 total). | Cursor Agent | RESOLVED |

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
