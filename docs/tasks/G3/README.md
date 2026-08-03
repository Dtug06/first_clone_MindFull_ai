# G3 — Tích hợp LLM và Safety

| Field | Value |
|---|---|
| Độ khó | Khó |
| Phụ thuộc | G1, G2 |
| Số task | 13 |
| Mục tiêu gói | Chuyển chat thành dữ liệu có cấu trúc bằng LLM có sẵn, đồng thời áp dụng Safety Gate độc lập và có thể kiểm thử. |
| Đầu ra tổng | Message được phân tích thành JSON hợp lệ; risk được quyết định và ghi lịch sử; Level 3-4 chặn matching; Level 4 dùng phản hồi cố định. |

## Tổng quan

Gói G3 đưa LLM vào pipeline chat, đồng thời tách bạch phần Safety khỏi response generation. Mọi analysis có structured output, có risk level được audit, và Level 3-4 tự động chặn matching. Level 4 phải dùng fixed template, không cho LLM tự do.

## Danh sách task

| # | Task ID | Tiêu đề | Phân loại | Ưu tiên | Trạng thái |
|---|---|---|---|---|---|
| 1 | [G3-T01](G3-T01-tao-ai-provider-abstraction-va-mock-provider.md) | Tạo AI Provider Abstraction và Mock Provider | Backend/AI | MUST | Phase 3 PASS (2026-08-02) |
| 2 | [G3-T02](G3-T02-dinh-nghia-json-schema-va-tu-dien-tin-hieu.md) | Định nghĩa JSON Schema và từ điển tín hiệu | AI/Data | MUST | Phase 3 PASS (2026-08-02) |
| 3 | [G3-T03](G3-T03-thiet-ke-prompt-phan-tich-chat.md) | Thiết kế prompt phân tích chat | AI | MUST | Phase 3 PASS (2026-08-02) |
| 4 | [G3-T04](G3-T04-luu-ai-analysis-run.md) | Lưu AI Analysis Run | Backend/DB | MUST | Phase 3 PASS (2026-08-02) |
| 5 | [G3-T05](G3-T05-luu-chat-analysis-result-dang-versioned.md) | Lưu Chat Analysis Result dạng versioned | Backend/DB | MUST | Phase 3 PASS (2026-08-02) |
| 6 | [G3-T06](G3-T06-tich-hop-llm-provider-that.md) | Tích hợp LLM Provider thật | Backend/AI | MUST | Phase 3 PASS (2026-08-02) |
| 7 | [G3-T07](G3-T07-validate-output-retry-va-fallback.md) | Validate output, retry và fallback | Backend/AI | MUST | Phase 3 PASS (2026-08-02) |
| 8 | [G3-T08](G3-T08-keyword-regex-safety-pre-filter.md) | Keyword/Regex Safety Pre-filter | Backend/Safety | MUST | Phase 3 PASS (2026-08-02) |
| 9 | [G3-T09](G3-T09-llm-risk-classification-rieng.md) | LLM Risk Classification riêng | Backend/Safety | MUST | Phase 3 PASS (2026-08-02) |
| 10 | [G3-T10](G3-T10-safety-resolver-va-risk-state-history.md) | Safety Resolver và Risk State History | Backend/Safety | MUST | Phase 3 PASS (2026-08-02) |
| 11 | [G3-T11](G3-T11-safety-event-source-va-action.md) | Safety Event, Source và Action | Backend/Safety | MUST | Phase 3 PASS (2026-08-02) |
| 12 | [G3-T12](G3-T12-phan-hoi-co-dinh-cho-level-4.md) | Phản hồi cố định cho Level 4 | Backend/Content | MUST | Phase 3 PASS (2026-08-02) |
| 13 | [G3-T13](G3-T13-expert-review-va-giao-dien-safety-co-ban.md) | Expert Review và giao diện Safety cơ bản | Full-stack/Safety | SHOULD | **Phase 3 APPROVE WITH FINDINGS** (2026-08-03) |

## Quy tắc chung

- Mỗi task có file riêng trong folder này; không gộp nhiều task vào một file.
- Mỗi task có 4 phần chính: Mục tiêu, Công việc chi tiết, Đầu ra cần bàn giao, Hoàn thành khi.
- Cursor chỉ thực hiện từng task nhỏ theo ID, không được giao một prompt làm toàn bộ gói.
- Mọi thay đổi API/Database/Safety/CBT phải review chéo giữa hai dev.
- Khi hoàn thành, cập nhật checklist và trạng thái trong file task tương ứng.

## Liên kết

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- Trạng thái tổng: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
