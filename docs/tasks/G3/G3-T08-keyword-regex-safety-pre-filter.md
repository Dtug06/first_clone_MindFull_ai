# G3-T08 — Keyword/Regex Safety Pre-filter

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/Safety |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T08) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Có lớp phát hiện nhanh độc lập với LLM.

## 2. Công việc chi tiết

- Xây danh sách rule có version và được lưu ngoài code hoặc cấu hình quản trị.
- Chuẩn hóa text trước khi match; xử lý biến thể cơ bản.
- Trả về matched rule, evidence và preliminary risk.
- Không dùng keyword match như kết luận duy nhất.
- Viết test dương tính, âm tính và false-positive cơ bản.

## 3. Đầu ra cần bàn giao

Safety pre-filter service.

## 4. Hoàn thành khi (Definition of Done)

- [x] Rule match trả được mã rule và bằng chứng. **PASS (2026-08-02)** — `MatchedRule` DTO carries `code`/`matchType`/`evidence`; `SingleRuleMatch` + `MultipleRules` tests assert full payload.
- [x] Rule version được ghi trong quyết định. **PASS (2026-08-02)** — `code` is version + intent combination (`KEYWORD_SELF_HARM_V1`); recorded in every `MatchedRule`.
- [x] Không hard-code rải rác trong nhiều service. **PASS (2026-08-02)** — only `SafetyPreFilterService` matches keywords; `Grep` confirms no `Pattern.matches` elsewhere.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T08** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
