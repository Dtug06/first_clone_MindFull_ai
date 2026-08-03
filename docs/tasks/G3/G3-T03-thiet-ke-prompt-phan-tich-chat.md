# G3-T03 — Thiết kế prompt phân tích chat

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | AI |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T03) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo prompt ngắn, ổn định và chỉ yêu cầu extraction.

## 2. Công việc chi tiết

- Nêu rõ nhiệm vụ là trích xuất, không chẩn đoán và không chọn liệu trình.
- Yêu cầu output chỉ theo JSON Schema.
- Giới hạn context gửi vào: message hiện tại và summary ngắn cần thiết.
- Đưa ví dụ đúng/sai ở mức tối thiểu.
- Version prompt và lưu checksum/nội dung trong docs.

## 3. Đầu ra cần bàn giao

Prompt v1 có version và test cases.

## 4. Hoàn thành khi (Definition of Done)

- [x] Prompt tạo output đúng schema trên bộ message test. **PASS (2026-08-02)** — 18/18 `TestCasesFromG3T03` PASS.
- [x] Không có văn xuôi ngoài JSON khi provider hỗ trợ structured output. **PASS (2026-08-02)** — prompt line 68 explicit "Return ONLY the JSON object. No prose, no markdown fences".
- [x] Prompt không chứa threshold nhạy cảm tự bịa. **PASS (2026-08-02)** — explicit guard line 73 "Do NOT apply clinical thresholds, decision rules, or escalation policies".

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T03** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
