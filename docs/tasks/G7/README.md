# G7 — Kết nối Frontend, Dashboard và Admin

| Field | Value |
|---|---|
| Độ khó | Trung bình / Nhiều integration |
| Phụ thuộc | Theo từng API G1-G6 |
| Số task | 12 |
| Mục tiêu gói | Tận dụng frontend hiện có, thay mock bằng API thật và hoàn thiện các trạng thái UI cần thiết. |
| Đầu ra tổng | Toàn bộ flow chính chạy qua UI; mock/hard-code còn lại được liệt kê hoặc loại bỏ; admin cơ bản xem Safety, Matching và CBT. |

## Tổng quan

Gói G7 gắn API thật vào frontend prototype hiện có: Auth/Consent, Chat, Check-in, Dashboard, Matching, CBT và Admin/Expert. Tất cả màn hình chính phải có loading/empty/error và phải bỏ hết mock data.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G7-T01](G7-T01-audit-frontend-hien-tai-va-lap-api-mapping.md) | Audit frontend hiện tại và lập API Mapping | Frontend/Analysis | MUST | To do |
| 2 | [G7-T02](G7-T02-chuan-hoa-openapi-contract-va-typescript-dto.md) | Chuẩn hóa OpenAPI Contract và TypeScript DTO | Full-stack/Contract | MUST | To do |
| 3 | [G7-T03](G7-T03-api-client-auth-interceptor-va-error-handler.md) | API Client, Auth Interceptor và Error Handler | Frontend | MUST | To do |
| 4 | [G7-T04](G7-T04-tich-hop-auth-va-consent-ui.md) | Tích hợp Auth và Consent UI | Frontend | MUST | To do |
| 5 | [G7-T05](G7-T05-tich-hop-chat-ui.md) | Tích hợp Chat UI | Frontend | MUST | To do |
| 6 | [G7-T06](G7-T06-tich-hop-daily-check-in-ui.md) | Tích hợp Daily Check-in UI | Frontend | MUST | To do |
| 7 | [G7-T07](G7-T07-dashboard-hanh-vi.md) | Dashboard hành vi | Frontend/Data | MUST | To do |
| 8 | [G7-T08](G7-T08-program-matching-proposal-ui.md) | Program Matching Proposal UI | Frontend/Matching | MUST | To do |
| 9 | [G7-T09](G7-T09-cbt-program-exercise-va-progress-ui.md) | CBT Program, Exercise và Progress UI | Frontend/CBT | MUST | To do |
| 10 | [G7-T10](G7-T10-admin-expert-dashboard-co-ban.md) | Admin/Expert Dashboard cơ bản | Frontend/Admin | SHOULD | To do |
| 11 | [G7-T11](G7-T11-loading-empty-error-va-accessibility-states.md) | Loading, Empty, Error và Accessibility States | Frontend/Quality | MUST | To do |
| 12 | [G7-T12](G7-T12-loai-bo-mock-va-kiem-tra-e2e-frontend.md) | Loại bỏ mock và kiểm tra E2E frontend | Frontend/Test | MUST | To do |

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
