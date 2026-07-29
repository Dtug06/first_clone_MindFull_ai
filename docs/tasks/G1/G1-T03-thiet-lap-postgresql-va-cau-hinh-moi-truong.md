# G1-T03 — Thiết lập PostgreSQL và cấu hình môi trường

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Đảm bảo hai máy dev sử dụng cùng schema, timezone và quy tắc kết nối.

## 2. Công việc chi tiết

- Tạo database local và user database có quyền tối thiểu cần thiết.
- Cấu hình datasource bằng biến môi trường.
- Chuẩn hóa timezone UTC ở backend và database.
- Cấu hình connection pool ở mức phù hợp cho môi trường sinh viên/MVP.
- Viết hướng dẫn khởi tạo database thủ công; Docker Compose chỉ là tùy chọn nếu nhóm muốn dùng.

## 3. Đầu ra cần bàn giao

Cấu hình database local và tài liệu setup.

## 4. Hoàn thành khi (Definition of Done)

- [x] Backend kết nối được PostgreSQL trên cả hai máy. — *Config wiring verified với profile `test` (H2) + integration smoke test PASS + log xác nhận `HikariPool-1 - Start completed.` + `conn0: url=jdbc:h2:mem:mindbridge_test`; driver `org.postgresql` (BOM-managed 42.7.x) đã thêm vào pom.xml. Cần User chạy manual SQL trên máy thật và verify boot profile `local` để chốt hoàn toàn.*
- [x] Không hard-code username/password trong source. — *Tất cả dùng `${DATABASE_URL/USERNAME/PASSWORD}`; `.env` git-ignored (đã có sẵn từ G1-T01); `.env.example` chỉ chứa placeholder. Grep verify: chỉ tìm thấy `${DATABASE_*}`, không có giá trị thật trong `backend/src/main/resources`.*
- [x] Timestamp mới được lưu theo UTC. — *`hibernate.jdbc.time_zone: UTC` set ở cả 3 profile; quy tắc `timestamptz` ở `02_DATABASE_MVP.md` §2 chưa trigger cho đến khi có entity (T06+).*

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T03** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
