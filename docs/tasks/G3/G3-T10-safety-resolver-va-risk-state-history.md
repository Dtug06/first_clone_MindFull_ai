# G3-T10 — Safety Resolver và Risk State History

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/Safety |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T10) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Hợp nhất pre-filter và classifier thành quyết định cuối có thể giải thích.

## 2. Công việc chi tiết

- Định nghĩa rule ưu tiên giữa keyword, LLM risk và trạng thái risk hiện tại.
- Tạo risk_state_history cho mọi thay đổi Level 1-4.
- Lưu source, rule version, model version, confidence và reason.
- Ngăn tự động hạ risk bất hợp lý nếu chưa qua điều kiện review.
- Tạo API nội bộ lấy risk state hiện tại.

## 3. Đầu ra cần bàn giao

Safety Resolver và lịch sử risk.

## 4. Hoàn thành khi (Definition of Done)

- [x] Cùng input/rule version cho cùng quyết định xác định được. **PASS (2026-08-02)** — `SafetyResolverService` is pure function; `DeterministicAndAppendOnly` 3/3 + `IntegrationTest` 9/9 with fixed inputs.
- [x] Mọi thay đổi risk có bản ghi lịch sử append-only. **PASS (2026-08-02)** — `RiskStateHistoryRepository` exposes only `append()`; reflection-scan verifies no `update()`/`setStatus()` mutator.
- [x] Risk hiện tại được truy vấn đúng. **PASS (2026-08-02)** — `RiskStateHistoryRepository.findLatestByConversationMessageId` + `SafetyResolverService.getCurrentRiskState` covered by `GetCurrentRiskState` 2/2 + `IntegrationTest` 9/9.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T10** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
