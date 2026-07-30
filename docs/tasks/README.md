# MindBridge AI — Task Catalog

Task catalog được tách theo từng gói (G1 → G8). Mỗi task là một file `.md` riêng để Cursor có thể đọc và thực hiện đúng phạm vi.

## Tổng quan 8 gói

| Gói | Tên | Độ khó | Phụ thuộc | Số task |
|---|---|---|---|---|
| [G1](./G1/README.md) | Nền tảng Backend, Authentication và Consent | Trung bình | Không | 10 |
| [G2](./G2/README.md) | Chat, Daily Check-in và Thu thập dữ liệu hành vi | Trung bình | G1 | 10 |
| [G3](./G3/README.md) | Tích hợp LLM và Safety | Khó | G1, G2 | 13 |
| [G4](./G4/README.md) | Phân tích hành vi và Hồ sơ người dùng | Khó | G2, G3; một phần G5 để có dữ liệu bài tập | 12 |
| [G5](./G5/README.md) | Danh mục và Vận hành liệu trình CBT | Khó / Khối lượng lớn | G1; có thể phát triển song song G3-G4 bằng mock | 15 |
| [G6](./G6/README.md) | Program Matching và Recommendation | Khó | G4 + G5 + Safety G3 | 12 |
| [G7](./G7/README.md) | Kết nối Frontend, Dashboard và Admin | Trung bình / Nhiều integration | Theo từng API G1-G6 | 12 |
| [G8](./G8/README.md) | Kiểm thử, Bảo mật, Triển khai và Tài liệu | Trung bình nhưng chạy xuyên suốt | Xuyên suốt G1-G7 | 14 |

## Thứ tự phụ thuộc

1. **G1** — Backend foundation + Auth + Consent (nền tảng chung).
2. **G2** — Chat + Daily Check-in + Behavioral Event (dữ liệu gốc).
3. **G3** — LLM + Safety (chạy được khi có G1 + G2).
4. **G4** — Behavior Analysis + Profile (cần G2 + G3).
5. **G5** — CBT Catalog + Runtime (cần G1; có thể làm song song G3-G4 bằng mock).
6. **G6** — Program Matching + Recommendation (cần G4 + G5 + Safety của G3).
7. **G7** — Frontend Integration (theo từng API G1-G6).
8. **G8** — Test, Security, Deploy, Docs (chạy xuyên suốt).

## Quy ước

- Mỗi file task có dạng: `Gx-Txx-slug-from-title.md`.
- Trong file có 4 phần: **Mục tiêu**, **Công việc chi tiết**, **Đầu ra cần bàn giao**, **Hoàn thành khi**.
- Ưu tiên `MUST` bắt buộc cho MVP; `SHOULD` chỉ làm sau khi flow chính ổn định.
- Cursor chỉ nhận 1 task mỗi lần; không giao toàn bộ gói trong một prompt.
- Sau khi task hoàn thành và review, cập nhật `docs/05_IMPLEMENTATION_STATUS.md`.

## Mẫu giao task cho Cursor

```text
Task ID: [Gx-Txx]
Read the relevant project rules and documents first.
Phase 1 - Read-only plan: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
Phase 2 - Implement only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
Phase 3 - Open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
```
