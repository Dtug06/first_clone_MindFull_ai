# G5-T01 — Chốt phạm vi CBT MVP và content ownership

| Field | Value |
|---|---|
| Group | G5 — Danh mục và Vận hành liệu trình CBT |
| Priority | MUST |
| Tags | Nghiệp vụ/Content |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giới hạn nội dung đủ demo và xác định ai được phép tạo/duyệt.

## 2. Công việc chi tiết

- Chốt 4 chương trình: STRESS, MOOD, WORRY, SLEEP.
- Mỗi chương trình 3 module, mỗi module 1-2 exercise.
- Định nghĩa trạng thái content draft/pending/approved/retired.
- Phân biệt nội dung demo và nội dung đã được chuyên gia duyệt.
- Không để LLM tự tạo hoặc sửa liệu trình production.

## 3. Đầu ra cần bàn giao

CBT MVP content inventory.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Có danh sách program/module/exercise dự kiến.
- [ ] Mỗi nội dung có owner và approval status.
- [ ] Scope không vượt quá khả năng hai dev.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G5-T01** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
