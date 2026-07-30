# G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi

| Field | Value |
|---|---|
| Độ khó | Trung bình |
| Phụ thuộc | G1 |
| Số task | 10 |
| Mục tiêu gói | Thu thập dữ liệu gốc có cấu trúc và không cấu trúc, đồng thời ghi nhật ký hành vi thống nhất. |
| Đầu ra tổng | User chat và trả lời check-in từ frontend; dữ liệu raw và behavioral event được lưu đúng user, đúng phiên, đúng ngày. |

## Tổng quan

Gói G2 tập trung vào dữ liệu gốc: chat session, conversation message, daily question, daily answer và behavioral event. Sau gói này, hệ thống đã có dữ liệu thật (không mock) để frontend hiển thị và để các gói sau (G3, G4) phân tích.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G2-T01](G2-T01-thiet-ke-chat-session.md) | Thiết kế Chat Session | Backend/DB | MUST | To do |
| 2 | [G2-T02](G2-T02-luu-va-truy-van-conversation-message.md) | Lưu và truy vấn Conversation Message | Backend/DB | MUST | To do |
| 3 | [G2-T03](G2-T03-chuan-hoa-xu-ly-noi-dung-va-du-lieu-nhay-cam.md) | Chuẩn hóa xử lý nội dung và dữ liệu nhạy cảm | Backend/Privacy | MUST | To do |
| 4 | [G2-T04](G2-T04-xay-daily-question-template-catalog.md) | Xây Daily Question Template Catalog | Backend/DB | MUST | To do |
| 5 | [G2-T05](G2-T05-giao-daily-question-theo-ngay.md) | Giao Daily Question theo ngày | Backend/Scheduler | MUST | To do |
| 6 | [G2-T06](G2-T06-ghi-nhan-daily-question-answer.md) | Ghi nhận Daily Question Answer | Backend/DB | MUST | To do |
| 7 | [G2-T07](G2-T07-xay-behavioral-event-log.md) | Xây Behavioral Event Log | Backend/Data | MUST | To do |
| 8 | [G2-T08](G2-T08-idempotency-va-chong-submit-lap.md) | Idempotency và chống submit lặp | Backend | SHOULD | To do |
| 9 | [G2-T09](G2-T09-seed-du-lieu-va-bo-kich-ban-thu-thap.md) | Seed dữ liệu và bộ kịch bản thu thập | Data/Test | MUST | To do |
| 10 | [G2-T10](G2-T10-ket-noi-chat-va-check-in-ui-hien-tai.md) | Kết nối Chat và Check-in UI hiện tại | Frontend/Integration | MUST | To do |

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
