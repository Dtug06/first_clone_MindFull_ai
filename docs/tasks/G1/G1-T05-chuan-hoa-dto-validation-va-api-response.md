# G1-T05 — Chuẩn hóa DTO, validation và API response

| Field | Value |
|---|---|
| Group | G1 — Nền tảng Backend, Authentication và Consent |
| Priority | MUST |
| Tags | Backend |
| Status | To do → Completed |
| Owner | ____________________ → UNASSIGNED |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Ngăn entity bị expose trực tiếp và thống nhất contract cho frontend.

## 2. Công việc chi tiết

- Tạo request/response DTO cho từng API.
- Áp dụng Bean Validation cho email, password, enum và trường bắt buộc.
- Tạo cấu trúc lỗi gồm code, message, fieldErrors, timestamp và requestId.
- Tạo mapper rõ ràng; không trả field nhạy cảm.
- Chuẩn hóa pagination response dùng chung.

## 3. Đầu ra cần bàn giao

Bộ DTO và format response/error dùng chung.

## 4. Hoàn thành khi (Definition of Done)

- [x] Frontend nhận lỗi validation theo cùng một cấu trúc. → GlobalExceptionHandler xử lý MethodArgumentNotValidException, trả ErrorResponse với fieldErrors[]. Đã verify compile + test BUILD SUCCESS.
- [x] Không controller nào trả thẳng JPA entity. → Hiện tại chưa có controller nào. T05 tạo DTO infrastructure; các module service/controller sau sẽ tuân thủ.
- [ ] Swagger mô tả được request/response chính. → Deferred sang G1-T10 (thêm springdoc-openapi + @Schema annotation lên DTOs).

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G1-T05** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
