# G5 — Danh mục và Vận hành liệu trình CBT

| Field | Value |
|---|---|
| Độ khó | Khó / Khối lượng lớn |
| Phụ thuộc | G1; có thể phát triển song song G3-G4 bằng mock |
| Số task | 15 |
| Mục tiêu gói | Quản lý nội dung CBT theo version bất biến và cho user đi qua toàn bộ vòng đời chương trình. |
| Đầu ra tổng | Có 4 chương trình demo; user được gán/chấp nhận, làm assessment, nhận/nộp bài, cập nhật tiến độ và state transition. |

## Tổng quan

Gói G5 lớn nhất: 4 chương trình CBT demo (STRESS, MOOD, WORRY, SLEEP), mỗi chương trình có nhiều module/exercise version bất biến. User đi từ PROPOSED đến COMPLETED, có assessment, submission, progress và state transition audit được.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G5-T01](G5-T01-chot-pham-vi-cbt-mvp-va-content-ownership.md) | Chốt phạm vi CBT MVP và content ownership | Nghiệp vụ/Content | MUST | To do |
| 2 | [G5-T02](G5-T02-thiet-ke-program-catalog-va-program-version.md) | Thiết kế Program Catalog và Program Version | Backend/DB | MUST | To do |
| 3 | [G5-T03](G5-T03-thiet-ke-module-va-module-version.md) | Thiết kế Module và Module Version | Backend/DB | MUST | To do |
| 4 | [G5-T04](G5-T04-thiet-ke-exercise-template-va-version.md) | Thiết kế Exercise Template và Version | Backend/DB | MUST | To do |
| 5 | [G5-T05](G5-T05-content-approval-cho-cbt.md) | Content Approval cho CBT | Backend/Governance | MUST | To do |
| 6 | [G5-T06](G5-T06-seed-4-chuong-trinh-cbt-demo.md) | Seed 4 chương trình CBT demo | Content/Data | MUST | To do |
| 7 | [G5-T07](G5-T07-user-program-va-state-machine.md) | User Program và State Machine | Backend/CBT | MUST | To do |
| 8 | [G5-T08](G5-T08-program-state-transition-history.md) | Program State Transition History | Backend/DB | MUST | To do |
| 9 | [G5-T09](G5-T09-baseline-weekly-va-final-assessment.md) | Baseline, Weekly và Final Assessment | Backend/CBT | MUST | To do |
| 10 | [G5-T10](G5-T10-user-module-progress-va-unlock-logic.md) | User Module Progress và Unlock Logic | Backend/CBT | MUST | To do |
| 11 | [G5-T11](G5-T11-exercise-assignment.md) | Exercise Assignment | Backend/CBT | MUST | To do |
| 12 | [G5-T12](G5-T12-exercise-submission.md) | Exercise Submission | Backend/CBT | MUST | To do |
| 13 | [G5-T13](G5-T13-program-review-va-dieu-chinh-nhip.md) | Program Review và điều chỉnh nhịp | Backend/CBT | SHOULD | To do |
| 14 | [G5-T14](G5-T14-pause-resume-escalate-va-withdraw.md) | Pause, Resume, Escalate và Withdraw | Backend/CBT/Safety | MUST | To do |
| 15 | [G5-T15](G5-T15-cbt-frontend-integration-va-runtime-test.md) | CBT Frontend Integration và Runtime Test | Full-stack/CBT | MUST | To do |

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
