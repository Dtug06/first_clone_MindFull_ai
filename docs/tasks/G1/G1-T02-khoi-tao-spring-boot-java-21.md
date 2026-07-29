# G1-T02 — Khởi tạo Spring Boot Java 21

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend |
| Status | Local review PASS (2026-07-30); chờ push origin sau khi G1-T01 merge |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo backend theo cấu trúc dễ mở rộng, không phát sinh kiến trúc quá phức tạp.

## 2. Công việc chi tiết

- Khởi tạo Maven project dùng Java 21 và phiên bản Spring Boot thống nhất.
- Thêm Spring Web, Validation, Security, Data JPA, Actuator, Flyway và PostgreSQL driver.
- Tổ chức package theo domain hoặc modular monolith, tránh gom toàn bộ vào controller/service/repository chung.
- Tạo cấu hình application-local, application-test và application-prod.
- Tạo GlobalExceptionHandler và format lỗi API thống nhất.

## 3. Đầu ra cần bàn giao

Backend build được và có cấu trúc package chuẩn.

## 4. Hoàn thành khi (Definition of Done)

- [x] mvn clean test chạy thành công.
- [x] Ứng dụng khởi động với profile local.
- [ ] Một API lỗi validation trả đúng format thống nhất. — **Deferred sang G1-T05** (chuẩn hoá DTO, validation và API response). Lý do: T02 chỉ scaffold; chưa có controller/DTO để trigger validation error.

**Review note (Phase 3, 2026-07-30, local)**:

| Aspect | Result | Note |
|---|---|---|
| DoD §4.1 `mvn clean test` | PASS | BUILD SUCCESS (6.131s); Surefire "No tests to run" |
| DoD §4.2 boot profile local | PASS | Started in 2.84s; profile=local; Tomcat 8080; context /api/v1 |
| DoD §4.3 validation error format | DEFERRED | Không có controller ở T02; ErrorResponse format sẽ đến ở G1-T05 |
| Security (§ 10-backend.mdc § Security) | n/a + OK | Không có secret; server.error.include-* all `never` |
| Database integrity | n/a | T02 không đụng DB; data-jpa/postgresql/flyway deferred |
| Frontend compatibility | OK | 0 file frontend changed; context-path /api/v1 khớp OpenAPI § servers |
| `00-project-core.mdc` invariants | 32/32 PASS | (xem commit review note chi tiết) |
| `mvnw clean compile` | PASS | BUILD SUCCESS (9.96s); 1 source file + 5 resource |
| `GET /api/v1/actuator/health` | PASS | 200 {"status":"UP"} |
| `GET /api/v1/nonexistent` | PASS | 404, không lộ stack trace / SQL / class name |

**Findings (Phase 3)**:

1. **OpenAPI `/health` endpoint** (file § path `/health`, line 30) chưa được T02 implement — actuator path hiện tại là `/actuator/health`. Đây là expected deferral — `backend/README.md` đã ghi rõ mapping sẽ đến ở G1-T10. Không phải defect.
2. **T02 branch base**: T02 được tạo từ `feature/G1-T01-baseline-standardization` (không từ `develop`) vì T01 chưa merge. Khi T01 merged → rebase T02 lên develop trước khi merge T02.

**Không commit được lên origin** vì thiếu GitHub credentials (cùng block với T01).

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T02** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
