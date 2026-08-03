# MindBridge AI — Database MVP

## 1. Purpose

File này mô tả database cần triển khai cho MVP.

Nó là tài liệu rút gọn dành cho quá trình code bằng Cursor.

Không tạo tất cả các bảng trong một lần.

Mỗi task chỉ tạo hoặc sửa các bảng nằm trong phạm vi task đó.

---

## 2. General Conventions

- Database: PostgreSQL.
- Primary key: UUID.
- Tên bảng: snake_case.
- Tên cột: snake_case.
- Timestamp: `timestamptz`.
- Tất cả timestamp lưu UTC.
- Schema change phải dùng Flyway.
- Không sửa migration đã merge hoặc apply.
- Không trả JPA Entity trực tiếp qua API.
- Dùng JSONB cho dữ liệu linh hoạt.
- Dữ liệu thường xuyên query phải dùng typed column.
- Raw data tách khỏi derived data.
- AI result không ghi đè raw data.
- CBT version là immutable row.
- Ownership phải xác định được từ database relation.

---

## 3. Identity and Consent

## 3.1. users

Mục đích:

Lưu thông tin tài khoản.

Cột:

| Column | Type | Requirement |
|---|---|---|
| id | uuid | PK |
| email | citext | Unique, not null |
| password_hash | text | Not null |
| display_name | varchar(100) | Not null |
| role | varchar(20) | Not null |
| status | varchar(20) | Not null |
| created_at | timestamptz | Not null |
| updated_at | timestamptz | Not null |

Role:

- USER
- EXPERT
- ADMIN

Status:

- ACTIVE
- SUSPENDED
- DELETED

---

## 3.2. consent_events

Mục đích:

Lưu lịch sử cấp hoặc thu hồi consent.

Cột:

| Column | Type |
|---|---|
| id | uuid |
| user_id | uuid |
| consent_type | varchar(50) |
| action | varchar(20) |
| policy_version | varchar(50) |
| metadata | jsonb |
| occurred_at | timestamptz |

Consent Type:

- CHAT_ANALYSIS
- PERSONALIZATION
- EXPERT_SHARING

Action:

- GRANTED
- REVOKED

Quy tắc:

- Append-only.
- Không update đè event cũ.
- Consent hiện tại lấy từ event mới nhất theo thời gian.
- Không dùng `MAX(policy_version)` để xác định consent mới nhất.

---

## 3.3. audit_logs

Mục đích:

Audit các thao tác và quyết định quan trọng.

Cột:

- id
- category
- action
- actor_type
- actor_id
- subject_type
- subject_id
- request_id
- metadata
- created_at

Không lưu raw chat hoặc nội dung nhạy cảm đầy đủ trong audit.

---

## 4. Chat and Daily Check-in

## 4.1. chat_sessions

Cột:

- id
- user_id
- title
- status
- started_at
- closed_at
- created_at
- updated_at

Status:

- ACTIVE
- CLOSED
- ARCHIVED

Index:

```text
(user_id, updated_at DESC)
```

Không sử dụng self-FK từ message để đại diện session.

---

## 4.2. conversation_messages

Cột:

- id
- session_id
- user_id
- role
- content
- redacted
- created_at
- updated_at

Role:

- USER
- ASSISTANT
- SYSTEM

Index:

```text
(session_id, created_at)
(user_id, created_at)
```

Quy tắc:

- Message phải thuộc session.
- Session phải thuộc user.
- Raw content tách khỏi AI analysis.
- AI failure không làm mất message.

---

## 4.3. daily_question_templates

Cột:

- id
- code
- version
- question_type
- prompt
- answer_schema
- status
- created_at

Unique:

```text
(code, version)
```

Question Type:

- SCALE
- SINGLE_CHOICE
- TEXT
- NUMBER

---

## 4.4. daily_question_options

Cột:

- id
- template_id
- value
- label
- order_index

Unique đề xuất:

```text
(template_id, value)
```

---

## 4.5. daily_question_assignments

Cột:

- id
- user_id
- template_id
- assigned_for_date
- timezone
- status
- created_at

Status:

- ASSIGNED
- ANSWERED
- SKIPPED
- EXPIRED

Unique phải ngăn giao trùng cùng câu hỏi, user và local date.

---

## 4.6. daily_question_answers

Cột:

- id
- assignment_id
- user_id
- answer_type
- numeric_value
- text_value
- option_value
- answered_at
- metadata

Answer Type:

- NUMERIC
- TEXT
- OPTION

Quy tắc:

- Một assignment chỉ có một answer.
- Phải validate exactly-one value theo answer_type.
- User answer phải đúng owner của assignment.

---

## 4.7. behavioral_events

Mục đích:

Lưu chỉ mục hành vi thống nhất phục vụ phân tích.

Cột:

- id
- user_id
- event_type
- source_type
- source_id
- occurred_at
- local_date
- timezone
- properties
- schema_version

Event MVP:

- CHAT_SESSION_STARTED
- CHAT_MESSAGE_SENT
- DAILY_CHECKIN_COMPLETED
- DAILY_CHECKIN_SKIPPED
- EXERCISE_ASSIGNED
- EXERCISE_STARTED
- EXERCISE_COMPLETED
- EXERCISE_SKIPPED
- PROGRAM_ACCEPTED
- PROGRAM_PAUSED
- RECOMMENDATION_OPENED
- RECOMMENDATION_HELPFUL

Quy tắc:

- Event không thay thế bảng nghiệp vụ.
- Event phải có source khi có thể.
- Event schema phải có version.

---

## 5. AI Analysis

## 5.1. ai_analysis_runs

> **Updated 2026-08-02 (G3-T04):** concrete schema implemented in V15 migration. The 3-state
> status (PENDING / SUCCEEDED / FAILED) listed below was extended to a 4-state
> lifecycle (PENDING / RUNNING / SUCCEEDED / FAILED) per task G3-T04 §2 and the
> user-approved Phase 1 decision. The `token_usage` JSONB column was split into
> two nullable BIGINT columns (`input_tokens`, `output_tokens`) per rule 28
> ("Do not use JSONB as a replacement for every typed field") and rule 29
> ("Frequently queried metrics must use typed columns"). The `source_type` /
> `source_id` polymorphic design was deferred — T04 only supports chat analysis
> (1 row per `message_id`), and the FK enforces referential integrity. A future
> task may add nullable `source_type` / `source_id` columns if the same table
> needs to track runs from other source classes (daily-question-answer,
> behavioral-event, etc.). `user_id` is a denormalized snapshot captured at
> run-creation time (NOT a FK; the source of truth is `conversation_messages.user_id`).

Cột (V15 hiện thực):

- id (UUID PK)
- message_id (UUID FK → conversation_messages, default NO ACTION — leave audit row in place if the parent message is removed; a future retention task decides the policy)
- user_id (UUID, denormalized snapshot, no FK)
- provider (VARCHAR 50)
- model (VARCHAR 100)
- prompt_version (VARCHAR 50)
- schema_version (VARCHAR 10, default "V1")
- status (VARCHAR 20, CHECK in PENDING/RUNNING/SUCCEEDED/FAILED)
- input_hash (VARCHAR 64, SHA-256 hex, regex check)
- output_hash (VARCHAR 64 NULL, SHA-256 hex, regex check)
- error_code (VARCHAR 50 NULL, CHECK in 3 AI codes)
- error_summary (VARCHAR 200 NULL, REDACTED, never raw chat)
- latency_ms (INTEGER, default 0, CHECK >= 0)
- input_tokens (BIGINT NULL)
- output_tokens (BIGINT NULL)
- model_risk_level (SMALLINT NULL, 1..4)
- confidence (NUMERIC(4,3) NULL, 0..1)
- created_at (TIMESTAMPTZ, default NOW())
- started_at (TIMESTAMPTZ NULL)
- completed_at (TIMESTAMPTZ NULL)

Status (4-state, G3-T04 thay vì 3-state ban đầu):

- PENDING
- RUNNING
- SUCCEEDED
- FAILED

Index:

- ai_analysis_runs_message_created_desc (message_id, created_at DESC)
- ai_analysis_runs_status_created_desc (status, created_at DESC)
- ai_analysis_runs_created_at (created_at)

Quy tắc:

- Không overwrite run cũ.
- Reprocess tạo run mới.
- Không lưu raw chat ở bất kỳ đâu (DB, log, file) — chỉ hash SHA-256.
- Input hash dùng để audit, không thay thế source.
- Lifecycle terminal constraints: SUCCEEDED row phải có output_hash;
  FAILED row phải có error_code; non-terminal row phải có completed_at NULL.
- message_id FK default NO ACTION (matches V14 risk_state_history) — a future
  retention task decides whether audit rows follow the parent message or
  stay behind after a message deletion.

---

## 5.2. chat_analysis_results

> **G3-T05 (2026-08-02) — Cập nhật từ v0 wishlist sang thiết kế V16.**
> Bảng này chỉ lưu **preliminary risk** từ chat analysis model (`model_risk_level`).
> `rule_risk_level` và `final_risk_level` thuộc về `risk_state_history` (§6.1).
> Xem thêm G3-T05 Phase 1 §1 "Conflict I must report".

Cột (V16 migration):

| # | Column | Type | Nullable | Default | Ghi chú |
|---|---|---|---|---|---|
| 1 | `id` | UUID | NOT NULL | `gen_random_uuid()` | PK |
| 2 | `analysis_run_id` | UUID | NOT NULL | — | FK → `ai_analysis_runs(id)` |
| 3 | `conversation_message_id` | UUID | NOT NULL | — | FK → `conversation_messages(id)` |
| 4 | `user_id` | UUID | NOT NULL | — | Snapshot (FK nguồn = `conversation_messages.user_id`) |
| 5 | `topic` | VARCHAR(40) | NOT NULL | — | Enum string (Topic) — singular, v1 schema chỉ chứa dominant value |
| 6 | `emotion` | VARCHAR(20) | NOT NULL | — | Enum string (Emotion) — singular |
| 7 | `intent` | VARCHAR(20) | NOT NULL | — | Enum string (Intent) — singular |
| 8 | `signals` | JSONB | NOT NULL | `'[]'::jsonb` | Array of Signal strings |
| 9 | `evidence_spans` | JSONB | NOT NULL | `'[]'::jsonb` | Array of `{start, end, textHash}` (SHA-256, không raw text) |
| 10 | `model_risk_level` | SMALLINT | NOT NULL | — | 1..4; preliminary risk từ chat analysis model; **không phải final risk** |
| 11 | `confidence` | NUMERIC(4,3) | NOT NULL | — | 0..1 |
| 12 | `analysis_status` | VARCHAR(20) | NOT NULL | `'ACTIVE'` | ACTIVE / SUPERSEDED / INVALIDATED |
| 13 | `supersedes_id` | UUID | NULL | — | Self-FK → bản cũ bị thay thế (null khi bản đầu tiên) |
| 14 | `created_at` | TIMESTAMPTZ | NOT NULL | `NOW()` | — |

**Không lưu ở đây** (thuộc `risk_state_history` §6.1):

- `rule_risk_level` — preliminary risk từ keyword/regex pre-filter (G3-T08).
- `final_risk_level` (= `risk_level`) — quyết định risk cuối cùng từ Safety Resolver (G3-T10).

Dữ liệu linh hoạt:

- `signals`: JSONB array (string enum values). Map từ schema v1 (đã là array).
- `evidence_spans`: JSONB array of `{start, end, textHash}`. SHA-256 hex, không raw text.

Quy tắc:

- `analysis_status IN ('ACTIVE', 'SUPERSEDED', 'INVALIDATED')` (CHECK).
- `model_risk_level BETWEEN 1 AND 4` (CHECK).
- `supersedes_id IS NULL OR supersedes_id <> id` (CHECK — không tự tham chiếu).
- Trigger: tại mọi thời điểm, chỉ có **tối đa 1 row ACTIVE** cho mỗi `conversation_message_id`.
- Trigger: row ACTIVE có `supersedes_id` thì row bị superseded phải đang ở trạng thái SUPERSEDED/INVALIDATED.
- Kết quả phải gắn với AI run (`analysis_run_id` FK).
- Rerun: tạo row MỚI (không UPDATE đè), `supersedes_id` trỏ về bản cũ.
- Index: `(conversation_message_id, analysis_status, created_at DESC) WHERE analysis_status = 'ACTIVE'` — phục vụ "lấy result hiệu lực mới nhất" (DoD §4.3).
- Index: `(user_id, created_at DESC)` — phục vụ G4 `behavior_daily_features` aggregation.

---

## 6. Safety

## 6.1. risk_state_history

> **Lưu ý ranh giới trách nhiệm:**
> Bảng này lưu `risk_level` (= final risk), `model_risk_level` (từ chat analysis),
> và `rule_risk_level` (từ keyword/regex pre-filter). `model_risk_level` ở đây là bản snapshot
> tại thời điểm Safety Resolver quyết định — không liên quan trực tiếp đến
> `chat_analysis_results` (§5.2) nhưng cùng mang ý nghĩa "tín hiệu từ model".

Cột:

- id
- user_id
- risk_level (= final_risk_level)
- model_risk_level (từ chat analysis classifier hoặc null)
- rule_risk_level (từ keyword/regex pre-filter, mặc định 1)
- current_risk_level (risk level trước khi resolve)
- source_type
- source_id
- reason_codes (JSONB array)
- confidence
- occurred_at

Lưu Level 1–4.

---

## 6.2. safety_events

Cột:

- id
- user_id
- risk_level
- status
- summary
- created_at
- resolved_at

Status:

- OPEN
- UNDER_REVIEW
- RESOLVED
- DISMISSED

MVP ưu tiên tạo event cho Level 3–4.

---

## 6.3. safety_event_sources

Cột:

- id
- safety_event_id
- source_type
- source_id
- created_at

Source Type:

- CHAT_ANALYSIS
- DAILY_ANSWER
- EXERCISE_SUBMISSION
- PROGRAM_ASSESSMENT

Quy tắc:

- `source_id` là **polymorphic reference** — KHÔNG có FK constraint ở DB level.
  Trade-off chấp nhận theo user-approved G3-T11 Phase 1 (decision C5):
  application layer (`SafetyEventService`) chịu trách nhiệm verify
  ownership/identity. Lý do: hỗ trợ nhiều loại source mà không phải
  thêm FK conditional phức tạp. Khi một source type mới được thêm
  vào, cần update `source_type` CHECK constraint VÀ application-layer
  ownership check tương ứng.
- Mỗi Safety Event phải có ít nhất một source — enforced ở service
  layer (PostgreSQL không có "required child" constraint sạch; trigger
  + app-layer check rõ hơn và dễ test).

---

## 6.4. expert_reviews

Cột:

- id
- safety_event_id
- reviewer_id
- decision
- note
- created_at

Decision có thể gồm:

- CONTINUE_MONITORING
- ESCALATE
- DISMISS
- REQUEST_FOLLOWUP

Giá trị cụ thể phải được xác nhận trước khi triển khai production.

---

## 6.5. safety_actions

> **Promoted 2026-08-02 (G3-T11):** bảng này từng nằm trong danh sách
> DEFERRED (§12) — G3-T11 promote lên MVP scope theo user-approved
> Phase 1 spec. Một Safety Event có NHIỀU Safety Actions (quan hệ 1-n);
> mỗi action độc lập — một action FAILED không chặn action khác.

Cột (V17 hiện thực):

|| Column | Type | Requirement |
||---|---|---|
|| id | uuid | PK |
|| safety_event_id | uuid | FK → `safety_events(id)` ON DELETE CASCADE |
|| action_type | varchar(30) | Not null — `SHOW_TEMPLATE` / `BLOCK_MATCHING` / `FLAG_REVIEW` / `PAUSE_PROGRAM` |
|| status | varchar(20) | Not null, default `PENDING` — `PENDING` / `SUCCEEDED` / `FAILED` / `SKIPPED` |
|| error_message | text | Nullable — populated khi execution fail |
|| executed_at | timestamptz | Nullable — populated khi execution xong (SUCCEEDED/FAILED/SKIPPED) |
|| created_at | timestamptz | Not null, default `NOW()` |

Quy tắc:

- Mỗi Safety Event phải có ít nhất một action — enforced ở service
  layer (cùng lý do polymorphic).
- Action execution KHÔNG thuộc scope G3-T11 — T11 chỉ persist row với
  status = `PENDING`. Runtime execution phân chia theo action_type:
  - `SHOW_TEMPLATE` — G3-T12 (Fixed Level 4 Response)
  - `BLOCK_MATCHING` — G6 (Program Matching Safety Gate)
  - `FLAG_REVIEW` — G3-T13 (Expert Review)
  - `PAUSE_PROGRAM` — task sau (CBT runtime integration)
- CASCADE delete từ `safety_events` — actions là children.

Index:

```text
safety_actions_event_idx      — (safety_event_id)
safety_actions_type_status_idx — (action_type, status) — phục vụ monitor PENDING
```

> **Historical note (pre-G3-T11):** schema wishlist v0 liệt kê
> `safety_events`/`safety_event_sources` nhưng không định nghĩa
> `safety_actions`. G3-T11 (2026-08-02) định nghĩa bảng này lần đầu
> và đồng thời gỡ khỏi §12 deferred list.

---

## 6.6. safety_keyword_rules

Mục đích:

Lưu các rule keyword / regex đã được version hóa dùng cho Safety
Pre-filter (Keyword/Regex Pre-filter). Rule phải ở trạng thái
APPROVED trước khi pre-filter đánh giá. Trạng thái, workflow duyệt
và quy tắc version mới tạo khi sửa nội dung mô phỏng theo
content versioning của CBT (`docs/04_SAFETY_AND_CBT_RULES.md` §15).

Cột:

| Column | Type | Requirement |
|---|---|---|
| id | uuid | PK |
| code | varchar(100) | Not null — định danh rule family, vd `SAFETY_SELF_HARM_V1` |
| rule_version | varchar(50) | Not null — phiên bản nội bộ rule, vd `v1` |
| pattern | text | Not null — keyword hoặc regex source (KEYWORD thì là literal, REGEX thì là Java regex) |
| match_type | varchar(20) | Not null — `KEYWORD` hoặc `REGEX` |
| preliminary_risk | smallint | Not null — risk gợi ý của rule, 1..4 |
| status | varchar(20) | Not null — `DRAFT`, `PENDING_REVIEW`, `APPROVED`, `RETIRED` |
| approved_by | uuid | Nullable — FK `users(id) ON DELETE SET NULL` |
| approved_at | timestamptz | Nullable — thời điểm được duyệt |
| created_at | timestamptz | Not null |
| updated_at | timestamptz | Not null |
| lock_version | bigint | Nullable — JPA optimistic lock |

Unique:

```text
(code, rule_version)
```

Mỗi `code` chỉ được có tối đa một row ở trạng thái `APPROVED`
tại một thời điểm — enforced bằng partial unique index
`WHERE status = 'APPROVED'`.

Check constraint:

```text
match_type IN ('KEYWORD', 'REGEX')
status IN ('DRAFT', 'PENDING_REVIEW', 'APPROVED', 'RETIRED')
preliminary_risk BETWEEN 1 AND 4
```

Index:

```text
safety_keyword_rules_one_active_per_code  — UNIQUE (code) WHERE status = 'APPROVED'
safety_keyword_rules_approved_lookup      — (code) WHERE status = 'APPROVED'
```

Quy tắc:

- KHÔNG seed rule production trong migration V13. Pre-filter trả
  về "no signal" cho đến khi chuyên gia chèn row APPROVED.
- Pattern và match_type là bất biến sau khi tạo — sửa nội dung
  phải tạo row mới với `rule_version + 1`.
- Status transition chỉ thông qua các method controlled
  (`submitForReview` → `approve` → `retire`).
- Cursor/developer KHÔNG tự ý chèn rule production — theo
  `docs/04_SAFETY_AND_CBT_RULES.md` §6.
- `preliminary_risk` chỉ là input cho Safety Resolver
  (T10); KHÔNG được coi là risk cuối cùng.

---

## 7. Behavior Analysis

## 7.1. user_daily_features

Một row cho mỗi user, ngày và feature version.

Cột:

- id
- user_id
- feature_date
- stress_score
- mood_score
- energy_score
- sleep_hours
- anxiety_signal
- engagement_score
- exercise_completion_rate
- max_risk_level
- explicit_coverage
- inferred_confidence
- feature_version
- model_bundle_version
- extra_features
- computed_at

Unique:

```text
(user_id, feature_date, feature_version)
```

Metric thường dùng phải là typed column.

---

## 7.2. user_behavior_profiles

Một current profile cho mỗi user.

Cột:

- id
- user_id
- window_end
- stress_avg_7d
- stress_avg_30d
- mood_avg_7d
- sleep_avg_7d
- energy_avg_7d
- anxiety_avg_7d
- engagement_score
- risk_level
- dominant_topics
- trend_summary
- data_coverage
- confidence
- profile_version
- calculated_at

Unique:

```text
user_id
```

---

## 7.3. user_behavior_profile_snapshots

Cột:

- id
- profile_id
- user_id
- window_start
- window_end
- snapshot_reason
- snapshot_data
- calculation_version
- created_at

Snapshot Reason:

- DAILY_REFRESH
- SIGNIFICANT_CHANGE
- PRE_PROGRAM_MATCHING
- PROGRAM_STARTED
- WEEKLY_REVIEW
- SAFETY_ESCALATION

Snapshot không được update sau khi đã dùng cho Matching.

---

## 8. CBT Catalog

## 8.1. intervention_programs

Logical identity của chương trình.

Cột:

- id
- code
- created_at

Unique:

```text
code
```

Ví dụ:

- CBT-STRESS
- CBT-MOOD
- CBT-WORRY
- CBT-SLEEP

---

## 8.2. intervention_program_versions

Cột:

- id
- program_id
- version
- name
- goal
- duration_weeks
- eligibility_rule_set_version
- exclusion_rule_set_version
- status
- approved_at
- created_at

Unique:

```text
(program_id, version)
```

Status:

- DRAFT
- PENDING_REVIEW
- APPROVED
- RETIRED

Approved version là bất biến.

---

## 8.3. program_modules

Cột:

- id
- program_id
- code
- created_at

---

## 8.4. program_module_versions

Cột:

- id
- module_id
- program_version_id
- version
- order_index
- name
- goal
- unlock_conditions
- status
- created_at

---

## 8.5. exercise_templates

Cột:

- id
- code
- created_at

---

## 8.6. exercise_template_versions

Cột:

- id
- exercise_template_id
- module_version_id
- version
- name
- instructions
- response_schema
- estimated_minutes
- status
- created_at

Exercise Version bất biến sau khi approved.

---

## 9. CBT Runtime

## 9.1. user_programs

Cột:

- id
- user_id
- program_version_id
- matching_decision_id
- state
- current_module_version_id
- proposed_at
- accepted_at
- started_at
- completed_at
- paused_at
- lock_version
- created_at
- updated_at

State:

- PROPOSED
- ACCEPTED
- BASELINE
- ACTIVE
- PAUSED
- ESCALATED
- FINAL_ASSESSMENT
- COMPLETED
- MAINTENANCE
- WITHDRAWN

---

## 9.2. program_state_transitions

Append-only.

Cột:

- id
- user_program_id
- from_state
- to_state
- trigger_type
- trigger_id
- actor_type
- actor_id
- reason_code
- occurred_at

---

## 9.3. user_module_progress

Cột:

- id
- user_program_id
- module_version_id
- status
- unlocked_at
- started_at
- completed_at

Status:

- LOCKED
- AVAILABLE
- IN_PROGRESS
- COMPLETED
- SKIPPED

---

## 9.4. exercise_assignments

Cột:

- id
- user_program_id
- module_progress_id
- exercise_template_version_id
- assigned_at
- due_at
- status

Status:

- ASSIGNED
- STARTED
- COMPLETED
- SKIPPED
- EXPIRED

---

## 9.5. exercise_submissions

Cột:

- id
- exercise_assignment_id
- user_id
- completion_status
- answer_data
- pre_intensity
- post_intensity
- difficulty_score
- helpfulness_score
- duration_seconds
- submitted_at

Một assignment chỉ có một final submission trong MVP, trừ khi task quy định retry.

---

## 9.6. program_assessments

Cột:

- id
- user_program_id
- assessment_type
- assessment_data
- completed_at

Type:

- BASELINE
- WEEKLY
- FINAL

---

## 9.7. program_reviews

Cột:

- id
- user_program_id
- review_type
- decision
- reviewer_type
- reviewer_id
- summary
- created_at

Không sử dụng bảng này thay cho state-transition history.

---

## 10. Program Matching

## 10.1. program_matching_runs

Cột:

- id
- user_id
- profile_snapshot_id
- trigger_reason
- rule_set_version
- threshold_bundle_version
- status
- started_at
- completed_at

---

## 10.2. program_matching_candidates

Cột:

- id
- matching_run_id
- program_version_id
- eligibility_passed
- exclusion_passed
- symptom_match_score
- goal_match_score
- time_fit_score
- engagement_fit_score
- data_confidence_score
- safety_penalty
- final_score
- reason_codes
- rejection_reasons

---

## 10.3. program_matching_decisions

Cột:

- id
- matching_run_id
- selected_program_version_id
- decision_type
- reason_codes
- decided_by
- decided_at

Decision Type:

- SELECTED
- NO_MATCH
- DEFERRED
- EXPERT_REVIEW
- SAFETY_BLOCKED

Decided By:

- RULE_ENGINE
- EXPERT
- USER_CHOICE
- SYSTEM_FALLBACK

---

## 11. Recommendation

## 11.1. recommendations

Cột:

- id
- user_id
- profile_snapshot_id
- type
- content_reference
- reason_codes
- status
- created_at

---

## 11.2. recommendation_feedback

Cột:

- id
- recommendation_id
- user_id
- action
- helpfulness_score
- created_at

---

## 12. Deferred Tables

Chưa bắt buộc trong MVP đầu:

- expert_access_grants
- maintenance_plans
- program_outcomes
- ~~safety_actions~~ — PROMOTED lên MVP scope bởi G3-T11 (2026-08-02); xem §6.5
- rule_set_versions
- clinical_thresholds nâng cao
- content_approvals đầy đủ
- knowledge_document_versions
- knowledge_chunk_embeddings đa model
- idempotency_keys
- outbox_events

Chỉ tạo khi có task được phê duyệt.

---

## 13. Migration Order

Thứ tự đề xuất:

1. PostgreSQL extensions.
2. Users.
3. Consent.
4. Audit.
5. Chat.
6. Daily Question.
7. Behavioral Event.
8. AI Analysis.
9. Safety.
10. Daily Features.
11. Behavior Profile.
12. CBT Catalog.
13. CBT Runtime.
14. Program Matching.
15. Recommendation.
16. RAG nếu còn thời gian.

Không tạo tất cả bảng trong một migration.

---

## 14. Ownership Rules

- User chỉ đọc dữ liệu thuộc chính mình.
- `user_id` lấy từ authenticated principal.
- Không tin tưởng user_id từ request body.
- Ownership phải kiểm tra tại service.
- Expert access cần rule riêng.
- Admin action quan trọng phải audit.

---

## 15. Current Implementation Tracking

Sau mỗi task, cập nhật trạng thái:

```text
Implemented:
- table
- migration
- task
- pull request

Planned:
- table
- task

Deferred:
- table
- reason
```