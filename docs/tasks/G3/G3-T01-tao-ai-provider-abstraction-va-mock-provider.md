# G3-T01 — Tạo AI Provider Abstraction và Mock Provider

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/AI |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T01) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Không để toàn bộ dự án phụ thuộc trực tiếp một nhà cung cấp LLM.

## 2. Công việc chi tiết

- Tạo interface ChatAnalysisProvider và DTO input/output.
- Tạo MockChatAnalysisProvider trả dữ liệu cố định theo kịch bản test.
- Cho phép chọn provider bằng configuration/profile.
- Tạo exception riêng cho timeout, invalid output và provider unavailable.
- Không để controller gọi SDK LLM trực tiếp.

## 3. Đầu ra cần bàn giao

AI adapter abstraction và mock implementation.

## 4. Hoàn thành khi (Definition of Done)

- [x] Toàn flow phân tích chạy được khi không có Internet/API key. **PASS (2026-08-02)** — `MockChatAnalysisProviderIntegrationTest` 1/1 + default `mindbridge.ai.provider=mock` boots full context.
- [x] Đổi mock/real provider không phải sửa business service. **PASS (2026-08-02)** — `ChatAnalysisProviderConfig` resolves bean via `@ConditionalOnProperty`; T06 real branch added without touching T01.
- [x] Có unit test cho mock provider. **PASS (2026-08-02)** — `MockChatAnalysisProviderTest` 39 cases across 7 nested classes.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T01** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
