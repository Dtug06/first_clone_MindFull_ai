# G8 — Kiểm thử, Bảo mật, Triển khai và Tài liệu

| Field | Value |
|---|---|
| Độ khó | Trung bình nhưng chạy xuyên suốt |
| Phụ thuộc | Xuyên suốt G1-G7 |
| Số task | 14 |
| Mục tiêu gói | Chứng minh hệ thống hoạt động đúng, an toàn ở mức MVP, có thể dựng lại và demo ổn định. |
| Đầu ra tổng | Test suite, deployment, seed/demo, tài liệu setup/API/database và release checklist hoàn chỉnh. |

## Tổng quan

Gói G8 chạy xuyên suốt: test (unit/integration/E2E), security, deployment, secret management, backup/restore và release documentation. Đảm bảo hai dev và giảng viên có thể dựng lại project từ đầu.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G8-T01](G8-T01-lap-test-strategy-va-definition-of-done.md) | Lập Test Strategy và Definition of Done | Chung/Test | MUST | To do |
| 2 | [G8-T02](G8-T02-migration-va-database-integrity-test.md) | Migration và Database Integrity Test | Backend/DB/Test | MUST | To do |
| 3 | [G8-T03](G8-T03-security-va-ownership-test.md) | Security và Ownership Test | Backend/Security/Test | MUST | To do |
| 4 | [G8-T04](G8-T04-ai-contract-va-provider-failure-test.md) | AI Contract và Provider Failure Test | Backend/AI/Test | MUST | To do |
| 5 | [G8-T05](G8-T05-safety-test-suite.md) | Safety Test Suite | Backend/Safety/Test | MUST | To do |
| 6 | [G8-T06](G8-T06-behavior-calculation-regression-test.md) | Behavior Calculation Regression Test | Backend/Data/Test | MUST | To do |
| 7 | [G8-T07](G8-T07-cbt-version-va-state-machine-test.md) | CBT Version và State Machine Test | Backend/CBT/Test | MUST | To do |
| 8 | [G8-T08](G8-T08-matching-regression-test.md) | Matching Regression Test | Backend/Matching/Test | MUST | To do |
| 9 | [G8-T09](G8-T09-end-to-end-test-va-demo-scenario.md) | End-to-End Test và Demo Scenario | Full-stack/Test | MUST | To do |
| 10 | [G8-T10](G8-T10-ci-build-va-quality-gate.md) | CI Build và Quality Gate | DevOps/Test | SHOULD | To do |
| 11 | [G8-T11](G8-T11-cau-hinh-moi-truong-va-quan-ly-secret.md) | Cấu hình môi trường và quản lý secret | DevOps/Security | MUST | To do |
| 12 | [G8-T12](G8-T12-deployment-health-check-va-observability-co-ban.md) | Deployment, Health Check và Observability cơ bản | DevOps | MUST | To do |
| 13 | [G8-T13](G8-T13-backup-restore-va-retention-toi-thieu.md) | Backup, Restore và Retention tối thiểu | DB/Privacy | SHOULD | To do |
| 14 | [G8-T14](G8-T14-hoan-thien-tai-lieu-ky-thuat-va-release-checklist.md) | Hoàn thiện tài liệu kỹ thuật và Release Checklist | Chung/Docs | MUST | To do |

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
