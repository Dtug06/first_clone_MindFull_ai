# Pre-G4 Implementation Decisions and Feature Contract

**Project:** MindBridge AI  
**Document type:** Pre-G4 implementation contract  
**Scope:** G4-T01 → G4-T12, with G4-T10 deferred  
**Version:** `pre_g4_contract_v1`  
**Status:** Approved for G4-T01 Phase 1 planning  
**Last updated:** 2026-08-03

---

# 1. Purpose

Tài liệu này chốt các quyết định còn thiếu trước khi bắt đầu G4-T01.

Mục tiêu:

- xác định thứ tự thực hiện G4;
- chốt nguồn dữ liệu của 8 feature;
- định nghĩa explicit, inferred và behavioral data;
- chốt timezone/local-date policy;
- chốt unit, miền giá trị và normalization;
- chốt cách xử lý dữ liệu thiếu;
- ghi rõ các quyết định còn chờ chuyên gia;
- ghi nhận known gap do G4-T10 bị hoãn;
- ngăn Cursor tự suy luận hoặc tự bịa ngưỡng.

Tài liệu này là nguồn quyết định cho G4-T01 Phase 1.

Cursor không được tự thay đổi các quyết định trong tài liệu này nếu chưa được duyệt.

---

# 2. Approved G4 Execution Order

Thứ tự thực hiện được duyệt:

```text
G4-T01
→ G4-T02
→ G4-T03
→ G4-T04
→ G4-T05
→ G4-T06
→ G4-T07
→ G4-T08
→ G4-T09
→ G4-T11
→ G4-T12
```

## 2.1 Deferred task

```text
G4-T10 — DEFERRED
```

Lý do:

- G4-T10 tạo immutable profile snapshot chủ yếu phục vụ G6 Program Matching.
- G6 chưa nằm trong phạm vi hiện tại.
- G4-T09 vẫn được triển khai với mô hình mutable current profile.

## 2.2 Dependency intent

```text
T01 = vocabulary + feature contract
T02 = typed schema
T03 = source aggregation
T04 = explicit/inferred combine
T05 = daily aggregation job
T06 = 7/30-day window aggregation
T07 = trend/streak
T08 = engagement + dominant topics
T09 = current behavior profile
T11 = data-quality policy
T12 = dashboard API + integration
```

Không được triển khai T09 trước T06/T07 nếu điều đó khiến profile phải patch lại nhiều lần.

---

# 3. Known Gap Because G4-T10 Is Deferred

Thêm đúng dòng sau vào `docs/05_IMPLEMENTATION_STATUS.md`:

```markdown
- **Known gap — G4-T10 deferred:** `user_behavior_profiles` created by G4-T09 is mutable and stores only the current profile; immutable profile snapshots are not available, so G6 Program Matching must remain deferred until G4-T10 is implemented.
```

## 3.1 Consequences

Khi G4-T10 chưa có:

- `user_behavior_profiles` chỉ giữ trạng thái hiện tại;
- profile cũ không được lưu bất biến;
- không thể tái lập chính xác profile tại thời điểm matching;
- G6 không được coi là production-ready;
- không được dùng mutable profile thay cho immutable snapshot mà không ghi rõ limitation.

---

# 4. Definitions of Data Source Types

## 4.1 Explicit data

Dữ liệu user tự khai báo trực tiếp.

Nguồn hiện tại:

```text
Daily Question Answer
```

Nguồn tương lai:

```text
CBT Assessment
Self-reported form
Approved manual assessment
```

Ví dụ:

```text
stress score
mood score
energy score
sleep duration
sleep quality
```

Explicit data có độ ưu tiên cao hơn inferred data khi cùng mô tả một feature.

---

## 4.2 Inferred data

Dữ liệu được suy luận từ nội dung hoặc AI pipeline.

Nguồn:

```text
Chat Analysis Result
LLM Structured Output
Keyword/Regex Signal
Safety Classification
```

Mỗi inferred value phải có:

```text
confidence
source
schema_version
model_version
prompt_version
analysis_result_id hoặc source reference
```

Không được tạo inferred field mới nếu G3 schema chưa có field tương ứng.

---

## 4.3 Behavioral data

Dữ liệu được tổng hợp từ hành vi của user.

Ví dụ:

```text
message_count
chat_session_count
active_chat_day_count
checkin_completed_count
checkin_skipped_count
exercise_started_count
exercise_completed_count
```

Behavioral data không được chứa raw chat content.

---

# 5. Feature Source Mapping

## 5.1 Stress

### Primary source

```text
Daily Answer explicit stress value
```

### Supporting/fallback source

```text
Chat Analysis stress signal
```

Chỉ dùng khi:

- G3 schema hiện có approved stress signal;
- signal có version rõ ràng;
- confidence đạt `MIN_INFERRED_CONFIDENCE`.

### Rule

```text
Explicit stress > inferred stress
```

Không được tự tạo stress signal nếu G3 chưa có.

---

## 5.2 Mood

### Primary source

```text
Daily Answer explicit mood value
```

### Supporting/fallback source

```text
Chat Analysis mood/emotion signal
```

Chỉ dùng khi semantics tương thích với feature contract.

Không được map tùy tiện emotion label sang mood score nếu chưa có mapping được duyệt.

---

## 5.3 Energy

### Primary source

```text
Daily Answer explicit energy value
```

### Inferred source

Mặc định:

```text
UNAVAILABLE
```

Chỉ dùng inferred energy/fatigue signal khi:

- G3 schema đã có field rõ ràng;
- semantics phù hợp;
- có confidence;
- có version.

Không được tự bịa field `energy_signal` hoặc `fatigue_signal`.

Nếu không có Daily Answer và G3 không có field phù hợp:

```text
energy = null / UNKNOWN
```

---

## 5.4 Sleep

### Primary explicit sources

```text
Daily Answer sleep_duration_hours
Daily Answer sleep_quality_raw
```

### Supporting inferred source

```text
Chat Analysis sleep_problem_signal
```

Chỉ dùng khi G3 schema đã có field versioned tương ứng.

### Rule

- Không dùng inferred signal thay thế explicit duration.
- Không dùng inferred signal thay thế explicit quality.
- Inferred sleep-problem signal chỉ là supporting/fallback evidence.
- Không tự kết hợp duration và quality thành clinical sleep score.

---

## 5.5 Anxiety Signal

### Primary source

```text
Latest effective versioned Chat Analysis result
```

### Supporting source

```text
Keyword/Regex signal
```

Keyword/regex chỉ dùng làm supporting evidence hoặc pre-filter.

Không được dùng keyword match thay cho approved final inferred signal.

---

## 5.6 Engagement

### Calculation v1 sources

```text
Chat behavioral data
Daily Check-in behavioral data
```

### Included components

```text
message_count
active_chat_session_count
active_chat_day_count
checkin_assigned_count
checkin_completed_count
checkin_completion_ratio
```

### Excluded components in v1

```text
exercise_started_count
exercise_completed_count
```

Lý do:

```text
G5 chưa triển khai
```

Không được coi exercise data chưa tồn tại là `0`.

---

## 5.7 Exercise Completion

### Source

```text
Future G5 exercise_assignments
Future G5 exercise_submissions
```

### Current status

```text
NOT_APPLICABLE
```

### Current value

```text
null
```

Không đưa feature này vào `engagement_v1_chat_checkin`.

---

## 5.8 Max Risk

### Source of truth

```text
Final risk produced by G3 Safety Resolver
```

Ưu tiên:

```text
risk_state_history.final_risk_level
```

### Secondary source

```text
safety_events.risk_level
```

`safety_events` chỉ dùng làm:

- audit;
- event lookup;
- Level 3–4 event handling.

Không dùng `safety_events.risk_level` thay thế final risk history trừ khi G3 implementation đã định nghĩa rõ đây là nguồn final-risk đầy đủ.

### Rule

```text
Never use preliminary model risk as max_risk
```

---

# 6. Timezone and Local-Date Policy

## 6.1 User timezone

Mỗi user phải có timezone dạng IANA Zone ID.

Ví dụ:

```text
Asia/Ho_Chi_Minh
```

## 6.2 Source of truth

Nếu `users.timezone` đã tồn tại:

```text
Use users.timezone
```

Nếu chưa tồn tại:

- báo dependency/conflict;
- không tự dùng system timezone;
- có thể cấu hình default demo:

```text
Asia/Ho_Chi_Minh
```

nhưng phải đánh dấu:

```text
DEMO_ONLY
```

Không được âm thầm lưu default demo như user timezone chính thức.

---

## 6.3 Timestamp storage

Tất cả event timestamp:

```text
occurred_at
created_at
updated_at
submitted_at
```

phải lưu bằng UTC.

Khuyến nghị database:

```text
TIMESTAMPTZ
```

---

## 6.4 Feature date

`feature_date` được tính như sau:

```text
UTC timestamp
→ convert to user timezone
→ take LocalDate
```

Không dùng system default timezone.

---

## 6.5 Local-day boundary

Một local day:

```text
00:00:00
→ before 00:00:00 of the next day
```

theo timezone của user.

---

## 6.6 Scheduler policy

Scheduler có thể chạy theo UTC.

Nhưng aggregation phải chạy:

```text
per-user
per-local-date
per-user-timezone
```

---

## 6.7 Late-arriving data

Dữ liệu đến muộn:

- thuộc về local date thực tế theo `occurred_at`;
- không bị chuyển sang ngày job chạy;
- phải hỗ trợ recompute:

```text
user_id + local_date
```

---

## 6.8 Timezone change

Khi user đổi timezone:

- dữ liệu lịch sử giữ `feature_date` và timezone đã dùng trước đó;
- không tự recompute toàn bộ lịch sử;
- backfill/recompute phải là task riêng.

---

## 6.9 Code constraints

Không dùng trực tiếp:

```java
LocalDate.now()
ZoneId.systemDefault()
```

trong business calculator.

Phải truyền rõ:

```java
Clock
ZoneId
```

---

# 7. General Feature Data Rules

1. Raw value từ Daily Question phải được giữ nguyên.
2. `user_daily_features` dùng normalized value `0.0–1.0` khi cần combine.
3. `null` nghĩa là thiếu dữ liệu hoặc không áp dụng.
4. Không chuyển `null` thành `0`.
5. Explicit và inferred value phải được xử lý riêng trước khi combine.
6. Confidence phải là field riêng.
7. Mỗi feature phải có polarity.
8. Không hard-code clinical label.
9. Mọi phép tính phải có `calculation_version`.
10. Không expose JPA entity trực tiếp qua API.

---

# 8. Unit and Value Range Contract

## 8.1 Stress

### Raw scale

```text
Integer 1–10
```

Ý nghĩa:

```text
1 = hoàn toàn không căng thẳng
10 = mức căng thẳng cao nhất user tự cảm nhận
```

### Database raw type

```text
SMALLINT
CHECK stress_raw BETWEEN 1 AND 10
```

### Normalized feature

```text
stress_score NUMERIC(4,3)
Range: 0.000–1.000
```

Công thức:

```text
stress_score = (stress_raw - 1) / 9.0
```

### Polarity

```text
HIGHER_IS_WORSE
```

### Missing

```text
null / UNKNOWN
```

### Notes

- Không tự gắn nhãn `HIGH_STRESS`.
- `HIGH_STRESS_THRESHOLD = TODO_EXPERT_REVIEW`.

---

## 8.2 Mood

### Raw representation

```text
VERY_LOW
LOW
NEUTRAL
GOOD
VERY_GOOD
```

Mapping:

| Mood code | Raw score |
|---|---:|
| `VERY_LOW` | 1 |
| `LOW` | 2 |
| `NEUTRAL` | 3 |
| `GOOD` | 4 |
| `VERY_GOOD` | 5 |

### Database type

```text
SMALLINT 1–5
```

hoặc:

```text
mood_code + mood_raw_score
```

### Normalized feature

```text
mood_score NUMERIC(4,3)
Range: 0.000–1.000
```

Công thức:

```text
mood_score = (mood_raw_score - 1) / 4.0
```

### Polarity

```text
HIGHER_IS_BETTER
```

### Missing

```text
null / UNKNOWN
```

### Notes

- Text note lưu riêng.
- Text tự do không được dùng trực tiếp làm score.

---

## 8.3 Energy

### Raw scale

```text
Integer 1–5
```

Ý nghĩa:

```text
1 = rất ít năng lượng
5 = rất nhiều năng lượng
```

### Database type

```text
SMALLINT
CHECK energy_raw BETWEEN 1 AND 5
```

### Normalized feature

```text
energy_score NUMERIC(4,3)
Range: 0.000–1.000
```

Công thức:

```text
energy_score = (energy_raw - 1) / 4.0
```

### Polarity

```text
HIGHER_IS_BETTER
```

### Missing

```text
null / UNKNOWN
```

---

## 8.4 Sleep

Sleep duration và sleep quality phải được lưu riêng.

### Sleep duration

```text
sleep_duration_hours NUMERIC(4,2)
Range: 0.00–24.00
Nullable: true
```

### Sleep quality

```text
sleep_quality_raw SMALLINT
Range: 1–5
Nullable: true
```

Ý nghĩa:

```text
1 = rất kém
5 = rất tốt
```

### G4 v1 sleep feature

```text
sleep_score NUMERIC(4,3)
Range: 0.000–1.000
```

Công thức:

```text
sleep_score = (sleep_quality_raw - 1) / 4.0
```

### Polarity

```text
HIGHER_IS_BETTER
```

### Calculation version

```text
sleep_quality_v1
```

### Constraints

- Không tự kết hợp duration và quality.
- Không tự kết luận thiếu ngủ.
- `SLEEP_DURATION_POLICY = TODO_EXPERT_REVIEW`.
- `SLEEP_COMBINATION_FORMULA = TODO_EXPERT_REVIEW`.

---

## 8.5 Anxiety Signal

### Type

```text
NUMERIC(4,3)
```

### Range

```text
0.000–1.000
```

### Meaning

```text
0.000 = không có hoặc rất ít bằng chứng
1.000 = tín hiệu xuất hiện rất mạnh trong validated output
```

### Polarity

```text
HIGHER_IS_WORSE
```

### Companion fields

```text
anxiety_signal_confidence
anxiety_signal_source
analysis_result_id
schema_version
model_version
prompt_version
```

### Missing

```text
null / UNKNOWN
```

### Constraints

- Không dùng boolean.
- Không dùng scale 1–4.
- Không được hiểu là chẩn đoán anxiety disorder.
- `MIN_INFERRED_CONFIDENCE = TODO_EXPERT_REVIEW`.

---

## 8.6 Engagement

### Aggregated feature

```text
engagement_score NUMERIC(4,3)
Range: 0.000–1.000
```

### Polarity

```text
HIGHER_IS_MORE
```

### Raw components

```text
message_count
active_chat_session_count
active_chat_day_count
checkin_assigned_count
checkin_completed_count
checkin_completion_ratio
exercise_assigned_count
exercise_completed_count
```

### G4 v1

```text
engagement_v1_chat_checkin
```

Chỉ dùng:

```text
CHAT
DAILY_CHECKIN
```

Không dùng exercise data trong v1.

### Weights

```text
ENGAGEMENT_COMPONENT_WEIGHTS = CONFIG_PLACEHOLDER
```

Không hard-code trong nhiều Java service.

---

## 8.7 Exercise Completion

### Internal representation

```text
exercise_completion_ratio NUMERIC(5,4)
Range: 0.0000–1.0000
```

### Formula

```text
completed_required_exercises / assigned_required_exercises
```

### Display

```text
percentage = ratio * 100
```

### Polarity

```text
HIGHER_IS_BETTER
```

### Missing semantics

```text
null
UNKNOWN
NOT_APPLICABLE
```

Không dùng `0` khi chưa có assignment.

### Current decision

```text
G5 not implemented
exercise_completion = null / NOT_APPLICABLE
```

---

## 8.8 Max Risk

### Type

```text
SMALLINT
```

### Range

```text
1–4
```

### Source

```text
Final risk from G3 Safety Resolver
```

### Daily feature

```text
max_risk_level =
MAX(final_risk_level within local date)
```

### Window feature

```text
max_risk_7d
max_risk_30d
```

### Polarity

```text
HIGHER_IS_WORSE
```

### Missing

```text
null / UNKNOWN
```

Không mặc định Level 1 khi không có dữ liệu.

Không normalize trong database.

---

# 9. Final Feature Contract Table

| Feature | Primary source | Supporting source | Raw/Input | Aggregated value | Range | Polarity |
|---|---|---|---|---|---|---|
| `stress` | Daily Answer | Chat Analysis stress signal | Integer `1–10` | `stress_score` | `0.0–1.0` | `HIGHER_IS_WORSE` |
| `mood` | Daily Answer | Chat Analysis mood/emotion signal | Enum/score `1–5` | `mood_score` | `0.0–1.0` | `HIGHER_IS_BETTER` |
| `energy` | Daily Answer | Only if approved G3 field exists | Integer `1–5` | `energy_score` | `0.0–1.0` | `HIGHER_IS_BETTER` |
| `sleep` | Daily Answer | Chat Analysis sleep-problem signal | Quality `1–5` + hours | `sleep_score` | `0.0–1.0` | `HIGHER_IS_BETTER` |
| `anxiety_signal` | Chat Analysis | Keyword/regex evidence | Structured signal | Same | `0.0–1.0` | `HIGHER_IS_WORSE` |
| `engagement` | Behavioral events | — | Counts/ratios | Normalized score | `0.0–1.0` | `HIGHER_IS_MORE` |
| `exercise_completion` | Future G5 data | — | Completed/assigned | Ratio | `0.0–1.0` | `HIGHER_IS_BETTER` |
| `max_risk` | G3 final risk history | Safety event audit | Final risk | Max level | `1–4` | `HIGHER_IS_WORSE` |

---

# 10. Null, Unknown, Zero and Not Applicable

## 10.1 `null`

Dùng khi:

- không có đủ dữ liệu;
- feature chưa được tính;
- feature không áp dụng;
- nguồn chưa tồn tại.

## 10.2 `UNKNOWN`

Trạng thái API/DTO khi chưa thể kết luận.

## 10.3 `zero`

Chỉ dùng khi dữ liệu nguồn thực sự chứng minh giá trị thấp nhất bằng 0.

## 10.4 `NOT_APPLICABLE`

Dùng khi feature không áp dụng.

Ví dụ:

```text
exercise_completion = NOT_APPLICABLE
```

khi user chưa có program/exercise assignment.

## 10.5 Mandatory rule

```text
Never convert null to 0
for average, trend, profile or confidence calculation.
```

---

# 11. Explicit and Inferred Combination Policy

## 11.1 Priority

```text
Explicit > Inferred
```

## 11.2 Initial rule

```text
If explicit exists:
    final value = explicit value
    inferred value = supporting evidence

Else if inferred exists and confidence is accepted:
    final value = inferred value

Else:
    final value = null / UNKNOWN
```

## 11.3 Conflicting sources

Nếu explicit và inferred lệch đáng kể:

- explicit vẫn là final value;
- lưu `source_conflict = true`;
- không tự average hai nguồn;
- không tự giảm confidence bằng công thức chưa được duyệt;
- công thức combine confidence để T11 quyết định.

## 11.4 Prohibited behavior

Không được:

```text
missing explicit = 0
missing inferred = 0
explicit + inferred / 2 mặc định
```

---

# 12. Versioning

## 12.1 Contract version

```text
pre_g4_contract_v1
feature_dictionary_v1
```

## 12.2 Initial calculation versions

```text
normalization_v1
sleep_quality_v1
engagement_v1_chat_checkin
exercise_completion_v1
max_risk_daily_v1
```

## 12.3 Versioning rules

Khi công thức thay đổi:

- tạo version mới;
- không đổi nghĩa version cũ;
- ghi version vào daily feature/profile;
- recompute chỉ khi có task/policy rõ ràng.

---

# 13. Open Expert Decisions

Hiện chưa có ngưỡng G4 nào được chuyên gia phê duyệt.

Cho phép và bắt buộc dùng:

```text
TODO_EXPERT_REVIEW
CONFIG_PLACEHOLDER
DEMO_ONLY
```

## 13.1 Decision table

| Decision code | Nội dung | Trạng thái |
|---|---|---|
| `MIN_INFERRED_CONFIDENCE` | Confidence tối thiểu để dùng inferred signal | `TODO_EXPERT_REVIEW` |
| `MIN_TOPIC_CONFIDENCE` | Confidence tối thiểu để tính dominant topics | `TODO_EXPERT_REVIEW` |
| `MIN_TREND_COVERAGE` | Coverage tối thiểu để kết luận trend | `TODO_EXPERT_REVIEW` |
| `TREND_DELTA_THRESHOLD` | Ngưỡng phân biệt `UP/DOWN/STABLE` | `TODO_EXPERT_REVIEW` |
| `ENGAGEMENT_COMPONENT_WEIGHTS` | Trọng số Chat và Daily Check-in | `CONFIG_PLACEHOLDER` |
| `DATA_QUALITY_THRESHOLDS` | Ngưỡng `SUFFICIENT/LOW/INSUFFICIENT` | `TODO_EXPERT_REVIEW` |
| `HIGH_STRESS_THRESHOLD` | Ngưỡng diễn giải stress cao | `TODO_EXPERT_REVIEW` |
| `SLEEP_DURATION_POLICY` | Cách diễn giải thời lượng ngủ | `TODO_EXPERT_REVIEW` |
| `SLEEP_COMBINATION_FORMULA` | Công thức kết hợp duration + quality | `TODO_EXPERT_REVIEW` |
| `CONFIDENCE_COMBINATION_FORMULA` | Công thức combine confidence | `TODO_EXPERT_REVIEW` |
| `CLINICAL_INTERPRETATION_LABELS` | Các nhãn diễn giải chuyên môn | `TODO_EXPERT_REVIEW` |

## 13.2 Technical normalization

Các công thức normalization trong tài liệu này được duyệt cho:

```text
normalization_v1
```

Chúng là phép biến đổi kỹ thuật, không phải clinical threshold.

---

# 14. Deferred Decisions

Các quyết định sau không chặn G4-T01:

## 14.1 Typed database column names

Chốt trong G4-T02.

Ví dụ:

```text
SMALLINT
NUMERIC
JSONB
ENUM code
```

## 14.2 Trend formula

Chốt trong G4-T07.

Không được tự bịa trong T01.

## 14.3 Engagement formula

Chốt trong G4-T08.

T01 chỉ định nghĩa component và contract.

## 14.4 Confidence combination formula

Chốt trong G4-T11.

T01 chỉ xác định required inputs.

---

# 15. Actual Schema Mapping Requirement

Trong G4-T01/T03 Phase 1, Cursor phải inspect G2/G3 source thật.

Không được giả định cột hoặc field tồn tại.

Phải xác minh:

```text
Daily Question answer field names
Chat analysis field names
Risk history field names
Safety event field names
Behavioral event types
users.timezone availability
```

Nếu conceptual contract không map được vào source hiện tại:

```text
Report CONTRACT_CONFLICT
```

Không tự tạo field ngoài task.

---

# 16. Implementation Constraints

1. Không trả JPA entity trực tiếp qua API.
2. Không log raw chat, password, JWT hoặc dữ liệu nhạy cảm.
3. Không hard-code threshold rải rác.
4. Không đổi nghĩa feature mà không bump version.
5. Không dùng preliminary risk thay final risk.
6. Không dùng missing value như score 0.
7. Không tự tạo clinical label.
8. Không gộp sleep duration và quality khi chưa được duyệt.
9. Không tính exercise completion khi chưa có assignment.
10. Không coi thiếu exercise data là engagement thấp.
11. Không dùng system timezone trong business logic.
12. Không tự tạo inferred energy signal.
13. Không tự map emotion label sang mood score nếu chưa có contract.
14. Không tự average explicit và inferred.

---

# 17. Acceptance Criteria for G4-T01

- [ ] Đã định nghĩa đủ 8 feature.
- [ ] Mỗi feature có primary source.
- [ ] Mỗi feature có supporting/fallback source nếu tồn tại.
- [ ] Mỗi feature có unit và range.
- [ ] Mỗi feature có polarity.
- [ ] Mỗi feature có missing semantics.
- [ ] Đã phân biệt raw và normalized value.
- [ ] Đã phân biệt explicit, inferred và behavioral.
- [ ] Không dùng `0` thay missing data.
- [ ] Stress dùng raw scale `1–10`.
- [ ] Mood dùng enum/score `1–5`.
- [ ] Energy dùng score `1–5`.
- [ ] Sleep duration và quality tách riêng.
- [ ] Anxiety signal dùng range `0–1`.
- [ ] Engagement dùng normalized score `0–1`.
- [ ] Exercise completion dùng ratio `0–1`.
- [ ] Max risk dùng final G3 risk scale `1–4`.
- [ ] Có timezone policy.
- [ ] Có late-data policy.
- [ ] Có Open Expert Decisions table.
- [ ] Có `feature_dictionary_v1`.
- [ ] Có initial calculation versions.
- [ ] Không có clinical threshold tự bịa.
- [ ] G4-T10 known gap đã được ghi nhận.
- [ ] G6 tiếp tục deferred khi chưa có immutable snapshot.
- [ ] Cursor inspect actual G2/G3 field names trước khi map.

---

# 18. Instruction to Cursor

Dùng đoạn sau khi bắt đầu G4-T01:

```text
Read this document as the approved Pre-G4 contract.

You may now begin G4-T01 Phase 1 read-only planning.

Do not modify files.

Inspect:
- actual G2 Daily Question schema;
- actual G3 Chat Analysis schema;
- actual risk_state_history schema;
- actual safety_events schema;
- actual behavioral_events schema;
- actual users timezone support.

Do not invent:
- missing fields;
- thresholds;
- weights;
- inferred signals;
- clinical labels;
- database columns outside the approved task.

Return:
1. current source mapping;
2. contract mismatches;
3. proposed Feature Dictionary structure;
4. exact files to create or modify;
5. unresolved decisions;
6. verification plan.

Stop after the read-only plan and wait for approval.
```
