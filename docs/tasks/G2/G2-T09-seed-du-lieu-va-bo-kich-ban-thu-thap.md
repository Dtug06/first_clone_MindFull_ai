# G2-T09 — Seed dữ liệu và bộ kịch bản thu thập

| Field | Value |
|---|---|
| Group | G2 — Chat, Daily Check-in và Thu thập dữ liệu hành vi |
| Priority | MUST |
| Tags | Data/Test |
| Status | Phase 3 PASS (2026-08-01) |
| Owner | ____________________ |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Tạo dữ liệu giả đủ để phát triển G3/G4 mà không phụ thuộc user thật.

## 2. Công việc chi tiết

- Tạo 10-20 user giả bằng script/seed môi trường dev.
- Tạo 7-30 ngày check-in với các pattern stress/sleep khác nhau.
- Tạo chat session và message giả không chứa dữ liệu thật.
- Tạo event tương ứng.
- Ghi rõ dữ liệu nào chỉ dành cho test/demo.

## 3. Đầu ra cần bàn giao

Seed dataset phát triển và tài liệu kịch bản.

## 4. Hoàn thành khi (Definition of Done)

- [x] Có thể reset và seed lại môi trường dev. — `DevSeedService.reset()` + `DevSeedService.run(scenario)`, opt-in via `--mindbridge.seed.run=true`.
- [x] Dữ liệu đủ cho test trung bình 7 ngày, trend và matching sau này. — 15 users × 30 days × 5 templates = 2250 assignments / 2160 answers (with 30% sporadic-skip); per-group trajectories (trending-up, trending-down, stable-low, stable-high, recovery, sporadic) verified by integration tests.
- [x] Không dùng dữ liệu cá nhân thật. — All emails match `*@mindbridge.test`; chat script contains no `@`, no 3+ digit runs, no PII patterns; behavioral events carry only metadata (length, role, flags), never raw content.

## 7. Bonus — fix G2-T05 flaky test

Trong quá trình Phase 2 của G2-T09, full test suite phát hiện 1 pre-existing flake trong `DailyQuestionAssignmentIntegrationTest.userChangesTimezone_sameDate_noDuplicate` (test G2-T05). Test này dùng UTC→Asia/Ho_Chi_Minh timezone change gần day-boundary — fail khi UTC và VN rơi vào 2 calendar day khác nhau. Fix:

- `@TestConfiguration static TestClockConfig` cung cấp `@Primary Clock frozenClock()` = `Clock.fixed(2026-06-15T12:00:00Z, UTC)` — chọn noon UTC nên mọi timezone trong test (UTC, Asia/Ho_Chi_Minh, America/Los_Angeles) đều cho cùng local date, không còn day-boundary race.
- Thêm `schema-daily-question-answers.sql` + `schema-behavioral-events.sql` vào `@Sql` của test (defensive cleanup `behavioralEventRepository.deleteAll()` cần tables này — trước đây pass chỉ vì context cache reuse schema từ test khác).

Sau fix: `./mvnw.cmd -B test` BUILD SUCCESS, 179/179 tests pass.

## 8. Phase 3 — Review findings

### 8.1 DoD compliance
- §4.1 (reset + seed lại dev): PASS — `run_createsAllRequiredRows`, `reset_isIdempotent_secondRunProducesSameCounts`, `reset_doesNotDeleteNonDemoUsers`.
- §4.2 (đủ dữ liệu 7 ngày average + trend): PASS — 15 users × 30 days × 5 templates; `trendingUp_stressIncreasesAcross30Days`, `trendingDown_stressDecreasesAcross30Days`, `stableLow_stressWithinLowRange` confirm per-group trajectories.
- §4.3 (không PII thật): PASS — `demoEmailsFollowMindbridgeTestDomain`, `chatMessagesContainNoPIIPatterns`, `behavioralEventPropertiesNeverContainRawMessageContent`, `eventsEmittedOnlyByBehavioralEventService`.
- §2 "Ghi rõ dữ liệu chỉ dành cho test/demo": PASS — `DemoChatScript.DEMO_ONLY` header, javadoc `SEED-ONLY` warnings on every `*ForSeed(...)` method, `.env.example` documents dev-only nature.

### 8.2 Security
- Activation gate `@ConditionalOnProperty(... havingValue="true")` + default `false` in `application.yml` — stock `mvn spring-boot:run` does not seed. ✅
- Runtime prod guard throws `IllegalStateException` if `spring.profiles.active` contains `prod`. ✅
- `reset()` filters by `demo-user-*@mindbridge.test` email pattern — never deletes non-demo users. ✅
- **Added during Phase 3:** `SeedGuard.requireSeedAllowed()` — runtime backstop on every `*ForSeed(...)` method. Throws if invoked outside `test` profile AND `mindbridge.seed.run=false`. Defense-in-depth for accidental cross-module calls.
- PII checks verified by integration tests (no `@`, no 3+ digit runs in chat content; event properties carry only metadata).

### 8.3 Database integrity
- FK CASCADE on `users.id` cascades to assignments, answers, chat sessions, messages, behavioral events. ✅
- `daily_question_templates` left untouched by `reset()` — version-immutable per CBT rule. ✅
- Deterministic UUIDs (`00000000-...-NNNNNNNNNNNN`) ensure re-seed never orphans old FK refs. ✅
- Idempotency: `reset()` → `run()` always starts clean; verified by `reset_isIdempotent_secondRunProducesSameCounts`. ✅
- `submitAnswerForSeed` synthesizes a `SubmitAnswerRequest` and runs the same 8-step validation pipeline — no validation shortcut. ✅
- `sendMessageForSeed` (added in Phase 3) now also checks `session.status == CLOSED` and throws `ChatSessionClosedException` — matches the regular `sendMessage` contract.

### 8.4 Frontend compatibility
- No frontend files touched. Seed is a backend CLI tool; operators trigger via JVM args. Frontend remains a UI prototype. ✅

### 8.5 Code quality
- Lint: 9 warnings found → 9 fixed (unused imports/fields/locals). `mvn test` clean. ✅
- Tests: 181/181 pass (was 179, +2 for `SeedGuard` happy path + production-throw case). ✅

### 8.6 Phase 3 verdict: PASS

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G2-T09** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.
