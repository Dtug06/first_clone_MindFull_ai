# G2-T09 — Seed dữ liệu và bộ kịch bản thu thập

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Data/Test |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo dữ liệu giả đủ để phát triển G3/G4 mà không phụ thuộc user thật.

## 2. Công việc chi tiết

- Tạo 10-20 user giả bằng script/seed môi trường dev.
- Tạo 7-30 ngày check-in với các pattern stress/sleep khác nhau.
- Tạo chat session và message giả không chứa dữ liệu thật.
- Tạo event tương ứng.
- Ghi rõ dữ liệu nào chỉ dành cho test/demo.

## 3. Đầu ra cần bàn giao

Seed dataset phát triển và tài liệu kịch bản.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Có thể reset và seed lại môi trường dev.
- [ ] Dữ liệu đủ cho test trung bình 7 ngày, trend và matching sau này.
- [ ] Không dùng dữ liệu cá nhân thật.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T09** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
