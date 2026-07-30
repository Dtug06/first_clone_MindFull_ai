# G1-T08 — Quản lý consent dạng lịch sử

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend/Privacy |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Ghi nhận việc user cấp hoặc thu hồi quyền phân tích/cá nhân hóa.

## 2. Công việc chi tiết

- Định nghĩa consent type: chat analysis, personalization, expert sharing.
- Lưu sự kiện GRANTED/REVOKED dạng append-only.
- Tạo API xem consent hiện tại, cấp và thu hồi consent.
- Tạo ConsentGuard để các module AI/Recommendation kiểm tra trước khi xử lý.
- Lưu policy version và thời điểm user đồng ý.

## 3. Đầu ra cần bàn giao

Consent API và service kiểm tra quyền.

## 4. Hoàn thành khi (Definition of Done)

- [x] Có thể truy vết đầy đủ lịch sử cấp/thu hồi. → Bảng consent_events append-only (V3 migration từ G1-T04), không có setter trên entity để sửa row, chỉ có factory `record()`. Test `appendOnly_historyPreserved` verify 2 GRANTED+REVOKED → 2 rows còn nguyên, current state = REVOKED.
- [x] Khi consent phân tích bị thu hồi, message mới không được gửi sang AI. → `ConsentGuard.requireChatAnalysisConsent()` throws `ConsentRequiredException` khi latest event là REVOKED. Module AI (G3) sẽ gọi method này.
- [x] Consent hiện tại được tính đúng theo sự kiện mới nhất. → `findLatestPerTypeByUser` (PostgreSQL DISTINCT ON) + `findLatestByUserAndType` cho single-type. Test verify current state = latest event.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
