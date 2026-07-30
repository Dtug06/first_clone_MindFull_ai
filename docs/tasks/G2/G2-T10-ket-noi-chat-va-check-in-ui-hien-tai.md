# G2-T10 — Kết nối Chat và Check-in UI hiện tại

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Frontend/Integration |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Thay mock bằng API thật nhưng giữ giao diện cơ bản đang có.

## 2. Công việc chi tiết

- Kết nối danh sách session, tạo session và lịch sử message.
- Kết nối form Daily Check-in và lịch sử trả lời.
- Thêm loading, empty, validation và retry state.
- Hiển thị lỗi ownership/401 bằng thông báo phù hợp.
- Loại bỏ hard-coded data ở các màn hình đã tích hợp.

## 3. Đầu ra cần bàn giao

Vertical slice Chat + Daily Check-in hoàn chỉnh.

## 4. Hoàn thành khi (Definition of Done)

- [ ] User thực hiện toàn bộ luồng bằng frontend.
- [ ] Reload trang vẫn đọc lại được dữ liệu từ DB.
- [ ] Không cần Postman hoặc chỉnh DB thủ công để demo luồng.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T10** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
