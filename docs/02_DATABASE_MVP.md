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

Cột:

- id
- user_id
- source_type
- source_id
- provider
- model
- prompt_version
- schema_version
- status
- input_hash
- token_usage
- latency_ms
- error_code
- started_at
- completed_at

Status:

- PENDING
- SUCCEEDED
- FAILED

Quy tắc:

- Không overwrite run cũ.
- Reprocess tạo run mới.
- Không lưu raw secret.
- Input hash dùng để audit, không thay thế source.

---

## 5.2. chat_analysis_results

Cột:

- id
- analysis_run_id
- conversation_message_id
- user_id
- topics
- emotions
- intent
- signals
- model_risk_level
- rule_risk_level
- final_risk_level
- confidence
- evidence_spans
- created_at

Dữ liệu linh hoạt:

- topics: jsonb hoặc text array.
- emotions: jsonb.
- signals: jsonb.
- evidence_spans: jsonb.

Quy tắc:

- Tách model risk, rule risk và final risk.
- Không chỉ lưu một risk level mà mất nguồn quyết định.
- Kết quả phải gắn với AI run.

---

## 6. Safety

## 6.1. risk_state_history

Cột:

- id
- user_id
- risk_level
- source_type
- source_id
- reason_codes
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
- safety_actions
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