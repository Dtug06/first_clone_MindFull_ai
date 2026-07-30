# G5-T09 — Baseline, Weekly và Final Assessment

| Field | Value |
|---|---|
| Group | G5 — Danh mục và Vận hành liệu trình CBT |
| Priority | MUST |
| Tags | Backend/CBT |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Đo dữ liệu trước-trong-sau liệu trình bằng form có version.

## 2. Công việc chi tiết

- Tạo program_assessments gắn user_program và assessment_type.
- Lưu schema_version, answers, score summary và submitted_at.
- Chỉ cho baseline khi state phù hợp; final trước completion.
- Tạo API lấy form và submit assessment.
- Validate answer theo schema.

## 3. Đầu ra cần bàn giao

Assessment API và data model.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Baseline hoàn thành mới chuyển ACTIVE theo rule.
- [ ] Final gắn đúng program version.
- [ ] Không submit assessment của user khác.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G5-T09** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
