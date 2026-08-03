# G3-T07 — Validate output, retry và fallback

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | MUST |
| Tags | Backend/AI |
| Status | Phase 3 PASS (2026-08-02) |
| Owner | Cursor (G3-T07) |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Xử lý output không ổn định mà không làm hỏng dữ liệu.

## 2. Công việc chi tiết

- Validate JSON Schema trước khi tạo result.
- Retry có giới hạn cho lỗi mạng/format có thể phục hồi.
- Không retry vô hạn hoặc retry Level 4 không kiểm soát.
- Fallback sang mock/unknown result theo môi trường nếu được cấu hình.
- Lưu invalid output hash và error category để debug.

## 3. Đầu ra cần bàn giao

Validation pipeline và chính sách retry.

## 4. Hoàn thành khi (Definition of Done)

- [x] JSON thiếu field không được lưu như result thành công. (Phase 3 verified — `ChatAnalysisSchemaValidator` runs before persistence; invalid → `InvalidAnalysisOutputException` → FAILED row.)
- [x] Timeout kết thúc trong thời gian cấu hình. (Phase 3 verified — per-attempt `requestTimeoutMs` enforced on `HttpRequest`; total wall-clock bounded by `maxAttempts × requestTimeoutMs + backoff`.)
- [x] Test được retry success, retry exhausted và fallback. (Phase 3 verified — `ProviderRetryExecutorTest` + `FallbackDecisionTest` + G3-T07 integration test all PASS.)

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 7. Phase 1 Plan (read-only, awaiting approval)

### Scope decision (locked here to avoid ambiguity)

This task **defines and centralizes the validation + retry + fallback policy
for BOTH AI providers** that are wired today:

1. `com.mindbridge.analysis.provider.ChatAnalysisProvider`
   (implemented by `MockChatAnalysisProvider` G3-T01 and
   `RealLlmChatAnalysisProvider` G3-T06).
2. `com.mindbridge.safety.classifier.RiskClassifierProvider`
   (implemented by `MockRiskClassifierProvider` G3-T09; no real impl yet).

Rationale: the task body explicitly says "Không retry vô hạn hoặc retry
Level 4 không kiểm soát" — Level 4 is a Safety concept owned by the risk
classifier pipeline (T09), so a policy that excluded it would silently
leave T09 un-policed. Both providers share the same failure surface
(timeout / unavailable / invalid output) and the same `ai_analysis_runs`
audit row (DB-MVP §5.1, V15 migration), so a single policy keeps audit
columns consistent and avoids the "implement twice / miss one stream"
risk the user called out in the Phase 1 brief.

Per task rule "không tự bịa nếu tài liệu khác đã có con số chuẩn",
T07 will NOT introduce a new retry limit on its own. It will reuse the
existing property already shipped by G3-T06:

```
mindbridge.ai.real.max-retries (default 1)
```

…exposed under a common namespace
`mindbridge.ai.provider.retry.{max-attempts,initial-backoff-ms}`
(read by both pipelines). This is a rename + generalization of the
T06 property, kept backwards-compatible (the old key still works as
a fallback if present). Phase 1 asks the user to confirm the rename
is acceptable rather than inventing a new number.

### Validation pipeline (runs BEFORE result is persisted)

1. Provider returns a raw payload string (real LLM JSON) or a built
   `ChatAnalysisOutput` (mock).
2. For the real provider path, parse JSON → JSON tree.
3. Validate the tree against
   `docs/schemas/chat_analysis_v1.schema.json` (Draft 07). Library
   already on the test classpath from G3-T02:
   `com.networknt:json-schema-validator:1.5.3`. Phase 1 proposes
   **moving it from `test` to `compile` scope** so the production
   validator class can use it. Reason: we need schema validation
   at runtime, not just in tests. User must approve this dependency
   scope change.
4. Map the validated tree → Java DTO (`ChatAnalysisOutput` /
   `RiskClassifierOutput` depending on pipeline). The DTO's compact
   constructor enforces the same constraints as a second line of
   defence (already shipped for chat analysis in G3-T02, present in
   `RiskClassifierOutput` since T09).
5. If any step fails → throw
   `InvalidAnalysisOutputException` / `InvalidRiskClassifierOutputException`.
   The calling service (`AiAnalysisRunService.startRun`) catches it
   and writes a FAILED `ai_analysis_runs` row with the existing
   error code + a redacted error summary. **No successful result is
   ever persisted** (current behaviour, preserved).

### Retry policy (per pipeline, shared config namespace)

| Property | Default | Meaning |
|---|---|---|
| `mindbridge.ai.provider.retry.max-attempts` | 1 (already shipped as `mindbridge.ai.real.max-retries`) | Total attempts (1 = no retry, 2 = one retry, …). |
| `mindbridge.ai.provider.retry.initial-backoff-ms` | 200 | Initial backoff before the first retry. Subsequent retries (if max-attempts > 2) double this value (200 → 400 → 800 …). |
| `mindbridge.ai.provider.retry.request-timeout-ms` | 20000 (already shipped as `mindbridge.ai.real.request-timeout-ms`) | Per-attempt HTTP timeout. The current `RealLlmChatAnalysisProvider` already enforces this. |

Retry is **only** applied for failures classified as:

* `ProviderTimeoutException` / `RiskClassifierTimeoutException`
* `ProviderUnavailableException` (5xx, 429) / `RiskClassifierUnavailableException`

Retry is **NOT** applied for:

* `InvalidAnalysisOutputException` / `InvalidRiskClassifierOutputException`
  — retrying a malformed payload is wasteful; surface as FAILED so
  audit captures the bad hash.
* Any `Level 4` model risk on a successful response — T07 does not
  retry a Level 4 payload (the user's "không retry Level 4 không kiểm
  soát" warning). The classifier pipeline returns the Level 4 signal
  once; the Safety Resolver (T10) opens the safety event. No
  second call to the provider.

### Fallback policy (environment-gated)

Property: `mindbridge.ai.provider.fallback.enabled`
(default `true` in `dev` profile, `false` in `prod` profile —
locked here as the user's "theo môi trường nếu được cấu hình"
phrasing permits env-specific defaults).

| Situation | `fallback.enabled = true` (dev) | `fallback.enabled = false` (prod) |
|---|---|---|
| All retries exhausted | `MockChatAnalysisProvider` is invoked for one last attempt; if THAT succeeds, the result is persisted as `provider = "fallback-mock"` (new enum value in `ai_analysis_runs.provider`). `output_hash` is computed from the fallback result. | Throw the last upstream exception. Service writes FAILED row. No success ever persisted. |
| `InvalidAnalysisOutputException` (malformed JSON) | Same as above. | Same as above. |
| Successful response with `modelRiskLevel = 4` (classifier pipeline) | **NO fallback**. The Level 4 signal is the authoritative input to the Safety Resolver; masking it with a mock would silently downgrade risk (rule "Do not silently downgrade a model risk signal"). | Same — no fallback. |

Phase 1 explicitly asks the user to confirm this table before coding,
because the "no fallback for Level 4" rule is a Safety-domain
decision, not a developer decision (rule
`.cursor/rules/00-project-core.mdc` line 33 "When uncertain, stop").

### Audit row fields (no DB migration)

`ai_analysis_runs` (V15) already has the columns needed:

* `error_code` (VARCHAR 50, 3 codes enum-checked for chat analysis,
  3 codes for risk classifier). T07 will **NOT** add new codes; it
  uses existing codes. The category the user mentioned
  (`MISSING_FIELD`, `INVALID_ENUM`, `TIMEOUT`, `PROVIDER_ERROR`)
  is encoded as a sub-field **inside `error_summary`** with the
  prefix `CATEGORY=`, e.g. `CATEGORY=MISSING_FIELD; reason=...`.
  Reasoning: avoids a schema migration for the MVP, keeps the
  schema-enforced enum stable. If the user prefers a separate
  `error_category` column, T07 can add V17 migration — flagged
  here as a Phase 1 question, not assumed.
* `output_hash` — only set on SUCCEEDED. **Never set on a row whose
  validation failed** — the hash is for valid outputs only. The
  `hash` of the INVALID payload is captured separately in a new
  column **OR** in `error_summary` (same question as above;
  default: `error_summary` to avoid migration). SHA-256 hex of the
  raw payload bytes (UTF-8), uppercase-or-lowercase consistent with
  the rest of the schema (`lowercase hex`). Test asserts the value
  is present in `error_summary` and is a valid 64-char hex string.
* `error_summary` (VARCHAR 200) — redacted via existing
  `AiRunErrorRedactor.redact(...)` which already strips non-ASCII
  printable characters. The `CATEGORY=...` prefix is ASCII, so it
  survives the redactor.

### Files planned to be created/modified (no changes in Phase 1)

**New** (under `backend/src/main/java/com/mindbridge/analysis/provider/`
unless noted):

* `provider/validation/ChatAnalysisSchemaValidator.java` — wraps
  `networknt` validator, exposes `validate(String json)`.
* `provider/validation/RiskClassifierSchemaValidator.java` — same,
  for the risk-classifier schema (which is the DTO compact
  constructor for now; a JSON Schema file may be added in a later
  task — T07 only adds the runtime class).
* `provider/pipeline/ProviderRetryExecutor.java` — generic retry
  helper using the shared properties; callable from both provider
  configs.
* `provider/pipeline/FallbackDecision.java` — encodes the table
  above; pure function `boolean shouldFallback(Outcome, env)`.

**Modified**:

* `pom.xml` — move `com.networknt:json-schema-validator:1.5.3`
  from `<scope>test</scope>` to `<scope>compile</scope>` (the
  validator now runs in production). Justification documented
  inline as a comment.
* `application.yml` — add the three new properties under
  `mindbridge.ai.provider.retry` and
  `mindbridge.ai.provider.fallback`; keep the old
  `mindbridge.ai.real.max-retries` as a deprecated alias.
* `provider/ChatAnalysisProvider.java` — JavaDoc only; no
  behavioural change.
* `provider/impl/RealLlmChatAnalysisProvider.java` — replace the
  internal retry loop with a call to `ProviderRetryExecutor`;
  add validation step before returning. Retry counter becomes
  `retry.max-attempts`.
* `provider/impl/MockChatAnalysisProvider.java` — unchanged
  (mock has nothing to retry; validation is a no-op on built DTO).
* `safety/classifier/provider/impl/MockRiskClassifierProvider.java`
  — unchanged. Safety pipeline applies retry/fallback at the
  *call site* (T11 pipeline; T07 does not change T09). T07's job
  is to ship the policy classes; wiring them into the call site
  is a follow-up task (T11+) the user mentioned in their Phase 1
  brief.

**Tests** (new):

* `provider/validation/ChatAnalysisSchemaValidatorTest.java` —
  2 valid + 4 invalid samples (mirrors `ChatAnalysisOutputSchemaTest`
  but exercises the new compile-scope validator).
* `provider/pipeline/ProviderRetryExecutorTest.java` — three
  scenarios per user request: `retry_success`,
  `retry_exhausted`, `fallback`. Also asserts: timeout aborts
  within configured `request-timeout-ms` (not unbounded);
  `Invalid*Exception` is NOT retried; Level 4 success is NOT
  retried (no second provider call).
* `provider/pipeline/FallbackDecisionTest.java` — table-driven
  test of every cell of the fallback matrix above.
* `run/AiAnalysisRunServiceFallbackIntegrationTest.java` —
  `@SpringBootTest` + fake provider that fails N times then
  succeeds via the mock fallback. Asserts `provider =
  "fallback-mock"` and the row is SUCCEEDED with valid
  `output_hash`. Asserts the invalid-output hash is captured in
  `error_summary` as `INVALID_HASH=<hex>`.

### Open questions for the user (Phase 1 must resolve before Phase 2)

1. **Retry count**: reuse T06's default of `1` (i.e. one retry),
   or change it? The task body does not specify a number; we must
   not invent one.
2. **Fallback "fallback-mock" provider label**: accept it as a new
   value in `ai_analysis_runs.provider` (VARCHAR 50, no DB CHECK on
   values — current schema permits arbitrary strings), or refuse
   to fallback and throw?
3. **Audit granularity**: keep using `error_summary` for the
   `CATEGORY=...` prefix and the invalid-payload hash (no
   migration), or add a V17 migration for a new
   `error_category` column and `invalid_payload_hash` column?
   Recommendation: no migration (MVP scope, small two-dev team,
   schema stability > minor query convenience). User's call.
4. **Dependency scope move**: is `networknt:json-schema-validator`
   acceptable as a production dependency? It is already on the
   test classpath (added in G3-T02). Moving it to compile scope
   is the smallest practical way to satisfy DoD §4.1 at runtime.
   No alternative library proposed (the user explicitly excluded
   inventing dependencies in the Phase 1 brief).
5. **Risk-classifier pipeline wiring**: confirm that T07 ships the
   policy classes only, and the call-site wiring (Safety Resolver
   / chat pipeline) is deferred to T11+ as the user suggested.
   This avoids duplicate retry code in T09's `MockRiskClassifierProvider`.

### Definition-of-Done mapping (sanity check vs. task body)

| DoD line | Plan evidence |
|---|---|
| JSON thiếu field không được lưu như result thành công. | Schema validator runs before persistence; invalid → `InvalidAnalysisOutputException` → FAILED row. Covered by `ProviderRetryExecutorTest` + integration test. |
| Timeout kết thúc trong thời gian cấu hình. | `request-timeout-ms` enforced per-attempt; `ProviderRetryExecutorTest` asserts elapsed ≤ `request-timeout-ms × max-attempts + backoff` (not unbounded). |
| Test được retry success, retry exhausted và fallback. | Three explicit tests in `ProviderRetryExecutorTest` + `FallbackDecisionTest` + integration test. |

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T07** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.

## 8. Phase 3 Review (2026-08-02)

### 8.1 Verdict

**APPROVE WITH MINOR FINDINGS.**

### 8.2 Verification commands + results

| Step | Command | Result |
|---|---|---|
| T07 targeted suite | `mvnw -B test -Dtest='ChatAnalysisSchemaValidatorTest,ProviderRetryExecutorTest,FallbackDecisionTest,RealLlmChatAnalysisProviderTest,RealLlmChatAnalysisProviderG3T07IntegrationTest,RealLlmChatAnalysisProviderMissingKeyTest,ChatAnalysisProviderConfigTest,RiskClassifierSchemaValidatorTest,MockChatAnalysisProviderIntegrationTest'` | **54/54 PASS, BUILD SUCCESS** (15s) |
| Full regression | `mvnw -B test` | **494/495 PASS, BUILD FAILURE** — 1 pre-existing G1 flake `ConsentGuardTest.grantedThenRevoked_latestWins` (acknowledged in §4 line 117, unrelated to T07) |
| Cross-task isolation | `git diff HEAD -- backend/src/main/java/com/mindbridge/analysis` | **0 lines changed** (whole `analysis/` directory is untracked; T07 adds only new files) |
| DB integrity | `Glob V*.sql` in `backend/src/main/resources/db/migration` | Max = `V16__create_chat_analysis_results.sql` (owned by G3-T05); no V17+ added by T07 |
| Security | `Grep` for `printStackTrace` / `System.out` / `sk-` / `Bearer ` / raw payload logging in `backend/src/main/java/com/mindbridge/analysis` | 0 hits in main code; only `Bearer ` literal is in the `Authorization` header construction (never logged); no key literals anywhere |
| Frontend compat | `Grep` for `mindbridge\.ai|ChatAnalysisProvider|provider/pipeline|...` in `frontend/src/` | Only `support@mindbridge.ai` (contact email); no T07 integration surface |

### 8.3 Findings (deferred, non-blocking)

- **M-doc-1**: §4 line 111 of `docs/05_IMPLEMENTATION_STATUS.md` quoted stale defaults `maxAttempts=3, requestTimeoutMs=15000`. Code + YAML correctly say `1` and `20000` per Phase 1 plan. Doc wording fixed inline.
- **M-doc-2**: §4 line 115 quoted `git diff backend/src/main/java/com/mindbridge/analysis` as verification; that command returns 0 lines because the whole directory is untracked. Reworded to "T07 only adds new files, doesn't touch existing tracked code" with the correct verification command.
- **M-process-1**: G3 working tree (including all T07 deliverables) is untracked on branch `feature/minh` (still at commit `4006e53 add all task G1`). Out of scope for T07 but flagged for the team's attention before next Phase 3 / before `git pull`.
- **L-stub-1**: `RiskClassifierSchemaValidator.validate(String json)` throws `UnsupportedOperationException` — intentional stub, documented in source. Reimplement when a future task ships a dedicated JSON Schema for risk.
- **L-redact-1**: Stale surefire report for `MockChatAnalysisProviderIntegrationTest` claiming "duplicate provider key" — reconciled. The two `provider:` keys in `application.yml` are at different nesting levels and parse cleanly; test now PASSES.

### 8.4 Ready for next task

G3-T08 (Safety Pre-filter: keyword/regex) is unblocked. Recommend deferring M-doc-1 + M-doc-2 fixes to a documentation housekeeping commit so the next Phase 3 reviews don't carry the same narrative gap.
