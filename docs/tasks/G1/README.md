# G1 — Nền tảng Backend, Authentication và Consent

| Field | Value |
|---|---|
| Độ khó | Trung bình |
| Phụ thuộc | Không |
| Số task | 10 |
| Mục tiêu gói | Tạo nền móng kỹ thuật ổn định để frontend hiện tại có thể gọi API an toàn và các nhóm chức năng sau dùng chung một chuẩn. |
| Đầu ra tổng | Backend Spring Boot chạy ổn định; PostgreSQL/Flyway hoạt động; user đăng ký, đăng nhập, phân quyền và quản lý consent được từ frontend. |

## Tổng quan

Gói G1 chuẩn bị mọi thứ cần thiết để hai dev có thể bắt đầu code backend và tích hợp với frontend hiện tại. Khi gói này xong, frontend có thể đăng ký, đăng nhập, lấy thông tin user và quản lý consent; backend có cấu trúc package, error format, validation và migration chuẩn.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G1-T01](G1-T01-dong-bang-baseline-va-chuan-hoa-repository.md) | Đóng băng baseline và chuẩn hóa repository | Chung | MUST | To do |
| 2 | [G1-T02](G1-T02-khoi-tao-spring-boot-java-21.md) | Khởi tạo Spring Boot Java 21 | Backend | MUST | To do |
| 3 | [G1-T03](G1-T03-thiet-lap-postgresql-va-cau-hinh-moi-truong.md) | Thiết lập PostgreSQL và cấu hình môi trường | Backend/DB | MUST | To do |
| 4 | [G1-T04](G1-T04-thiet-lap-flyway-va-extension-postgresql.md) | Thiết lập Flyway và extension PostgreSQL | Backend/DB | MUST | To do |
| 5 | [G1-T05](G1-T05-chuan-hoa-dto-validation-va-api-response.md) | Chuẩn hóa DTO, validation và API response | Backend | MUST | To do |
| 6 | [G1-T06](G1-T06-dang-ky-dang-nhap-va-jwt.md) | Đăng ký, đăng nhập và JWT | Backend/Auth | MUST | To do |
| 7 | [G1-T07](G1-T07-phan-quyen-user-expert-va-admin.md) | Phân quyền USER, EXPERT và ADMIN | Backend/Auth | MUST | To do |
| 8 | [G1-T08](G1-T08-quan-ly-consent-dang-lich-su.md) | Quản lý consent dạng lịch sử | Backend/Privacy | MUST | To do |
| 9 | [G1-T09](G1-T09-audit-co-ban-va-request-tracing.md) | Audit cơ bản và request tracing | Backend/Security | SHOULD | To do |
| 10 | [G1-T10](G1-T10-health-check-swagger-cors-va-ket-noi-frontend.md) | Health check, Swagger, CORS và kết nối frontend | Full-stack | MUST | To do |

## Quy tắc chung

- Mỗi task có file riêng trong folder này; không gộp nhiều task vào một file.
- Mỗi task có 4 phần chính: Mục tiêu, Công việc chi tiết, Đầu ra cần bàn giao, Hoàn thành khi.
- Cursor chỉ thực hiện từng task nhỏ theo ID, không được giao một prompt làm toàn bộ gói.
- Mọi thay đổi API/Database/Safety/CBT phải review chéo giữa hai dev.
- Khi hoàn thành, cập nhật checklist và trạng thái trong file task tương ứng.

## Liên kết

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- Trạng thái tổng: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
