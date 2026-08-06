# G2-T10 — Kết nối Chat và Check-in UI hiện tại

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Frontend/Integration |
| Status | Phase 3 PASS (2026-08-01) |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Thay mock bằng API thật nhưng giữ giao diện cơ bản đang có.

## 2. Công việc chi tiết

- Kết nối danh sách session, tạo session và lịch sử message.
- Kết nối form Daily Check-in và lịch sử trả lời.
- Thêm loading, empty, validation và retry state.
- Hiển thị lỗi ownership/401 bằng thông báo phù hợp.
- Loại bỏ hard-coded data ở các màn hình đã tích hợp.

## 3. Đầu ra cần bàn giao

Vertical slice Chat + Daily Check-in hoàn chỉnh.

## 4. Hoàn thành khi (Definition of Done)

| [x] User thực hiện toàn bộ luồng bằng frontend.
- [x] Reload trang vẫn đọc lại được dữ liệu từ DB.
- [x] Không cần Postman hoặc chỉnh DB thủ công để demo luồng.

## 7. Phase 3 — Review findings (2026-08-01)

### 7.1 DoD compliance

| DoD | Status | How |
|---|---|---|
| §4.1 User thực hiện toàn bộ luồng bằng frontend | PASS | Chat: token login → typing indicator → first message via `POST /chat/sessions` → follow-up via `POST /chat/sessions/{id}/messages` (with `Idempotency-Key`). Check-in: `GET /daily-checkins/today` → per-assignment `POST /daily-checkins/{id}/answer` → per-card success state. All from UI, zero Postman/curl. |
| §4.2 Reload vẫn đọc lại dữ liệu từ DB | PASS | Chat: `sessionId` in `sessionStorage` → `listMessages(sessionId, 0, 20)` on mount. Check-in: `today()` re-runs on every mount; `assignment.answered === true` items render as read-only. Auth: token + user in `localStorage` (G1-T10). |
| §4.3 Không cần Postman / chỉnh DB thủ công | PASS | Seed (G2-T09) creates demo users; UI walks through happy path with no DB manipulation. |

### 7.2 Security

- **Ownership boundary**: chat session IDs and check-in assignment IDs come from authenticated `GET`/`POST` calls. `JwtAuthFilter` on backend resolves userId from Bearer token — frontend never sends userId. ✅
- **401 → clear + redirect**: `ApiClient.handleUnauthorized` clears persisted auth on 401; `UserLayout` guard renders `<Navigate to="/auth" replace />` when no token. Defense-in-depth. ✅
- **403**: rendered as "You don't have access…" banner (chat) / same on check-in — never raw exception text. ✅
- **No token logging**: token only flows through `localStorage` ⇄ `Authorization: Bearer` header. No `console.*` anywhere in the new code; no `JSON.stringify(token)`. ✅
- **No raw message logging**: `message.content` is input to `POST` or rendered in DOM — never written to storage except via the server. ✅
- **No PII escape**: `moodOptions`, `suggestedPrompts` remain UI presentation strings from `data/index.ts` (no PII, no user data baked in). ✅

### 7.3 Database integrity

- **No new migration**. G2-T10 is frontend-only. ✅
- **API contract not changed**: `docs/03_API_CONTRACT.yaml` unchanged; all DTOs (`ChatSessionResponse`, `ChatMessageResponse`, `DailyQuestionAssignmentResponse`, `DailyAnswerRequest`, `CheckinHistoryResponse`, …) match YAML field-by-field. ✅
- **No backend code touched**: `git status` shows zero changes outside `frontend/src/**/*`. ✅

### 7.4 Lint + build

- `npm run lint`: exit 0, 2 warnings (both pre-existing in `auth/AuthContext.tsx:168` and `components/ui/BreathingOrb.tsx:56` — none introduced by G2-T10). ✅
- `npm run build`: exit 0, 1978 modules, `dist/assets/index-Bmc6SASc.js` 444.86 kB / 127.49 kB gzipped. ✅
- `./mvnw.cmd -B test`: BUILD SUCCESS, **181/181 tests pass** — no backend regression. ✅

### 7.5 Mapping to backend DTOs (verified)

| Contract entry | Frontend DTO | Required field parity |
|---|---|---|
| `ChatSessionResponse` | `ChatSessionResponse` (chat.ts) | `id, status, createdAt, updatedAt` ✅ |
| `ChatMessageResponse` | `ChatMessageResponse` (chat.ts) | `id, sessionId, role, content, createdAt, analysisStatus` ✅ |
| `ChatSessionPageResponse` / `ChatMessagePageResponse` | identical wrappers ✅ | `items, page, size, totalElements, totalPages` ✅ |
| `CreateChatSessionRequest` / `SendMessageRequest` | `title?:string\|null` / `content:string` ✅ | |
| `DailyQuestionAssignmentResponse` | `DailyQuestionAssignmentResponse` (dailyquestion.ts) | `assignmentId, templateCode, questionType, prompt, assignedForDate, answered` ✅ |
| `DailyQuestionOptionResponse` | identical ✅ | `value, label, orderIndex` ✅ |
| `DailyAnswerRequest` | `DailyAnswerRequest` with optional numeric/text/option ✅ | |
| `DailyAnswerResponse` | `id, assignmentId, answeredAt` ✅ | |
| `CheckinHistoryResponse` | `date, timezone, answers` ✅ | |

`Idempotency-Key` header attached per G2-T08 spec to two endpoints: `POST /chat/sessions/{id}/messages` and `POST /daily-checkins/{id}/answer`.

### 7.6 Verdict

**Phase 3 PASS.** G2 group is now complete end-to-end:

- G2-T04..T06 → real template/assignment/answer DB persistence (backend)
- G2-T07 → behavioral event log (backend)
- G2-T08 → idempotency (backend) — integrated at the front of `sendMessage` and `submitAnswer`
- G2-T09 → deterministic seed (backend)
- **G2-T10 → real chat sessions + real check-in answers from the UI, end-to-end reload-safe, idempotency-protected, ownership-respecting**

G3 prerequisite (G2-T05 frontend check-in + G2-T02 conversation messages) is now satisfied, so the G3 LLM/Safety group can begin.

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T10** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
