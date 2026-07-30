# G4-T04 — Kết hợp explicit và inferred signal

| Field | Value |
|---|---|
| Group | G4 — Phân tích hành vi và Hồ sơ người dùng |
| Priority | MUST |
| Tags | Backend/Analysis |
| Status | To do |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Ưu tiên lời user tự khai báo nhưng vẫn dùng AI khi thiếu dữ liệu.

## 2. Công việc chi tiết

- Định nghĩa trọng số MVP, ví dụ explicit cao hơn inferred.
- Không tính inferred khi confidence dưới ngưỡng cấu hình.
- Lưu source flags và confidence sau kết hợp.
- Không coi thiếu check-in là score bằng 0.
- Version hóa công thức để có thể tính lại.

## 3. Đầu ra cần bàn giao

Feature calculation service v1.

## 4. Hoàn thành khi (Definition of Done)

- [ ] Test đúng trường hợp explicit only, inferred only, cả hai và thiếu dữ liệu.
- [ ] Confidence/coverage phản ánh đúng nguồn.
- [ ] Không hard-code ngưỡng rải rác.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G4-T04** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
