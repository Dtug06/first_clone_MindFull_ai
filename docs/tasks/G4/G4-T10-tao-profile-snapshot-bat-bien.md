# G4-T10 — Tạo Profile Snapshot bất biến

| Field | Value |
|---|---|
| Group | G4 — Phân tích hành vi và Hồ sơ người dùng |
| Priority | MUST |
| Tags | Backend/DB |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Giữ đúng profile được dùng tại thời điểm ra quyết định.

## 2. Công việc chi tiết

- Tạo user_behavior_profile_snapshots với window, data, calculation_version và reason.
- Tạo snapshot cho DAILY_REFRESH, PRE_PROGRAM_MATCHING, PROGRAM_STARTED, SAFETY_ESCALATION.
- Snapshot append-only và không cập nhật lại.
- Lưu source feature range/hash để truy vết.
- Tạo API nội bộ lấy snapshot.

## 3. Đầu ra cần bàn giao

Profile snapshot workflow.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Matching luôn tham chiếu snapshot cụ thể.
- [ ] Snapshot cũ không thay đổi khi profile hiện tại cập nhật.
- [ ] Có thể giải thích dữ liệu tại thời điểm quyết định.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G4-T10** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
