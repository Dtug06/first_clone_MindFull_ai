# G6 — Program Matching và Recommendation

| Field | Value |
|---|---|
| Độ khó | Khó |
| Phụ thuộc | G4 + G5 + Safety G3 |
| Số task | 12 |
| Mục tiêu gói | Chọn chương trình bằng rule có thể giải thích, lưu toàn bộ candidate và nối quyết định với user program. |
| Đầu ra tổng | Profile Snapshot chạy matching, lưu run/candidates/decision, user chấp nhận và tạo user_program; recommendation cơ bản có feedback. |

## Tổng quan

Gói G6 chạy rule engine: safety gate, eligibility, exclusion, scoring, snapshot, candidate và decision. Mọi quyết định đều giải thích được từ dữ liệu đã lưu. Recommendation cơ bản và feedback để đo usefulness.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G6-T01](G6-T01-chot-rule-threshold-va-reason-code-v1.md) | Chốt Rule, Threshold và Reason Code v1 | Nghiệp vụ/Backend | MUST | To do |
| 2 | [G6-T02](G6-T02-matching-trigger-va-tao-profile-snapshot.md) | Matching Trigger và tạo Profile Snapshot | Backend/Matching | MUST | To do |
| 3 | [G6-T03](G6-T03-safety-gate-va-data-quality-gate.md) | Safety Gate và Data Quality Gate | Backend/Matching | MUST | To do |
| 4 | [G6-T04](G6-T04-program-matching-run.md) | Program Matching Run | Backend/DB | MUST | To do |
| 5 | [G6-T05](G6-T05-eligibility-va-exclusion-evaluation.md) | Eligibility và Exclusion Evaluation | Backend/Matching | MUST | To do |
| 6 | [G6-T06](G6-T06-candidate-scoring.md) | Candidate Scoring | Backend/Matching | MUST | To do |
| 7 | [G6-T07](G6-T07-luu-matching-candidate.md) | Lưu Matching Candidate | Backend/DB | MUST | To do |
| 8 | [G6-T08](G6-T08-matching-decision.md) | Matching Decision | Backend/DB | MUST | To do |
| 9 | [G6-T09](G6-T09-user-chap-nhan-tu-choi-de-xuat.md) | User chấp nhận/từ chối đề xuất | Full-stack/Matching | MUST | To do |
| 10 | [G6-T10](G6-T10-recommendation-co-ban-theo-trang-thai.md) | Recommendation cơ bản theo trạng thái | Backend/Recommendation | SHOULD | To do |
| 11 | [G6-T11](G6-T11-recommendation-feedback.md) | Recommendation Feedback | Full-stack/Recommendation | SHOULD | To do |
| 12 | [G6-T12](G6-T12-explainability-audit-va-test-matching.md) | Explainability, audit và test matching | Backend/Test | MUST | To do |

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
