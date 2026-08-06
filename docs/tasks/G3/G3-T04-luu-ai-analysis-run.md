# G3-T04 — Lưu AI Analysis Run

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/DB |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T04) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Truy vết mỗi lần gọi AI và lỗi phát sinh.

## 2. Công việc chi tiết

- Tạo bảng ai_analysis_runs với provider, model, prompt_version, schema_version, status, latency, token usage và error code.
- Lưu input_hash/output_hash thay vì raw prompt nếu không cần.
- Quản lý trạng thái PENDING/RUNNING/SUCCEEDED/FAILED.
- Gắn run với source message.
- Index theo message, status và created_at.

## 3. Đầu ra cần bàn giao

Migration và service ghi AI run.

## 4. Hoàn thành khi (Definition of Done)

- [x] Mỗi lần gọi provider tạo một run. **PASS (2026-08-02)** — `AiAnalysisRunIntegrationTest.twoRunsNoDedup` 1/1 + `AiAnalysisRunServiceTest` 9/9.
- [x] Run thất bại lưu lỗi kỹ thuật nhưng không lộ raw chat. **PASS (2026-08-02)** — `AiRunErrorRedactor` 13/13 (truncate 200 + non-ASCII→placeholder).
- [x] Có thể truy vết result dùng model/prompt/schema nào. **PASS (2026-08-02)** — V15 `ai_analysis_runs` 19 cols includes `provider`/`model`/`prompt_version`/`schema_version`/`input_hash`/`output_hash`.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T04** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
