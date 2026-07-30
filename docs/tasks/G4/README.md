# G4 — Phân tích hành vi và Hồ sơ người dùng

| Field | Value |
|---|---|
| Độ khó | Khó |
| Phụ thuộc | G2, G3; một phần G5 để có dữ liệu bài tập |
| Số task | 12 |
| Mục tiêu gói | Biến dữ liệu gốc và kết quả AI thành feature theo ngày, xu hướng và profile có thể dùng cho dashboard/matching. |
| Đầu ra tổng | Scheduled job tạo user_daily_features; profile và snapshot có coverage/confidence; API dashboard trả dữ liệu 7/30 ngày. |

## Tổng quan

Gói G4 biến dữ liệu rời rạc thành feature theo ngày, profile hiện tại và snapshot bất biến. Khi gói này xong, dashboard có số liệu thật và matching có dữ liệu đầu vào ổn định kèm coverage/confidence.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G4-T01](G4-T01-chot-feature-dictionary-mvp.md) | Chốt Feature Dictionary MVP | Data/Analysis | MUST | To do |
| 2 | [G4-T02](G4-T02-thiet-ke-user-daily-features.md) | Thiết kế user_daily_features | Backend/DB | MUST | To do |
| 3 | [G4-T03](G4-T03-chuan-hoa-nguon-du-lieu-trong-ngay.md) | Chuẩn hóa nguồn dữ liệu trong ngày | Backend/Data | MUST | To do |
| 4 | [G4-T04](G4-T04-ket-hop-explicit-va-inferred-signal.md) | Kết hợp explicit và inferred signal | Backend/Analysis | MUST | To do |
| 5 | [G4-T05](G4-T05-daily-feature-aggregation-job.md) | Daily Feature Aggregation Job | Backend/Scheduler | MUST | To do |
| 6 | [G4-T06](G4-T06-tinh-cua-so-7-ngay-va-30-ngay.md) | Tính cửa sổ 7 ngày và 30 ngày | Backend/Analysis | MUST | To do |
| 7 | [G4-T07](G4-T07-trend-streak-va-thay-doi-so-voi-baseline.md) | Trend, streak và thay đổi so với baseline | Backend/Analysis | MUST | To do |
| 8 | [G4-T08](G4-T08-engagement-va-dominant-topics.md) | Engagement và Dominant Topics | Backend/Analysis | SHOULD | To do |
| 9 | [G4-T09](G4-T09-xay-user-behavior-profile-hien-tai.md) | Xây User Behavior Profile hiện tại | Backend/DB | MUST | To do |
| 10 | [G4-T10](G4-T10-tao-profile-snapshot-bat-bien.md) | Tạo Profile Snapshot bất biến | Backend/DB | MUST | To do |
| 11 | [G4-T11](G4-T11-data-quality-coverage-va-confidence.md) | Data Quality, coverage và confidence | Backend/Data | MUST | To do |
| 12 | [G4-T12](G4-T12-dashboard-api-va-kiem-thu-tinh-toan.md) | Dashboard API và kiểm thử tính toán | Full-stack/Data | MUST | To do |

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
