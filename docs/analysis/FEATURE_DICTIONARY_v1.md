# Feature Dictionary v1

| Field | Value |
|---|---|
| Project | MindBridge AI |
| Document type | Feature dictionary (technical artifact, no code) |
| Scope | G4-T01 — chot feature dictionary MVP cho 8 feature |
| Contract reference | `docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md` (`pre_g4_contract_v1`) |
| Version | `feature_dictionary_v1` |
| Status | APPROVED FOR IMPLEMENTATION (G4-T02 onward) |
| Last updated | 2026-08-04 |

> Tai lieu nay la nguon quyet dinh cho G4-T02 → G4-T12. Cursor khong duoc tu thay doi cac quyet dinh trong tai lieu nay neu chua duoc duyet.
>
> Moi nguong / trong so / cong thuc combine chua co chuyen gia duyet deu duoc danh dau `TODO_EXPERT_REVIEW`, `CONFIG_PLACEHOLDER` hoac `DEMO_ONLY`. Khong hard-code cac gia tri nay trong code.

---

## §1. Purpose & Scope

### 1.1 Purpose

Tai lieu nay chot **dinh nghia day du cho 8 feature MVP** ma G4 se compute va dashboard se hien thi:

1. `stress`
2. `mood`
3. `energy`
4. `sleep`
5. `anxiety_signal`
6. `engagement`
7. `exercise_completion`
8. `max_risk`

Moi feature duoc mo ta day du ve:

- **Source**: bang / cot / API field cu the.
- **Classification**: explicit / inferred / behavioral.
- **Raw & aggregated value**: kieu du lieu, mien gia tri, don vi.
- **Polarity**: `HIGHER_IS_WORSE`, `HIGHER_IS_BETTER`, `HIGHER_IS_MORE`.
- **Missing semantics**: `null` / `UNKNOWN` / `NOT_APPLICABLE`.
- **Calculation version**: cong thuc normalization.
- **Test case mau**: input → output mong doi (≥ 1 case / feature).
## §2. Contract Versioning

### 2.1 Ba loai version, ba vai tro khac nhau

De T02, T04 dung nhat quan, tai lieu nay phan biet ro 3 khai niem version:

| Version concept | Vai tro | Thay doi khi | VD |
|---|---|---|---|
| `feature_dictionary_vN` | **Schema cua dictionary** (tai lieu nay) | Them/xoa feature; doi taxonomy ten feature; doi primary source | `feature_dictionary_v1` → `v2` (them feature `cognitive_load`) |
| `feature_calculation_<name>_vN` | **Cong thuc tinh** raw → aggregated cua 1 feature | Doi normalization; doi aggregation; doi combine rule | `stress_calculation_v1` (raw 1—5) → `v2` (raw 1—5 co clamp 0.05) |
| `chat_analysis_schema_vN` | **Schema cua G3 chat analysis output** (da co tu G3-T02) | Them/xoa field; doi field type | `chat_analysis_schema_v1` (locked 2026-08-02) |

**Quan trong**:

- `feature_dictionary_v1` (tai lieu nay) KHONG bump khi chi doi cong thuc tinh → bump `feature_calculation_*_vN`.
- `feature_dictionary_v1` (tai lieu nay) bump khi them feature moi hoac doi primary source.
- `chat_analysis_schema_v1` thuoc G3-T02; G4 doc no nhung khong sua no.

### 2.2 Initial versions cho G4

| Version constant | Value | Notes |
|---|---|---|
| `FEATURE_DICTIONARY_VERSION` | `v1` | Tai lieu nay |
| `NORMALIZATION_VERSION` | `normalization_v1` | Ap dung cho stress/mood/energy/sleep quality normalization |
| `SLEEP_QUALITY_CALCULATION_VERSION` | `sleep_quality_v1` | Hien `null / UNKNOWN` — SLEEP_QUALITY template chua seed |
| `ENGAGEMENT_CALCULATION_VERSION` | `engagement_v1_chat_checkin` | CHAT + DAILY_CHECKIN only; cong thuc = `CONFIG_PLACEHOLDER` |
| `EXERCISE_COMPLETION_CALCULATION_VERSION` | `exercise_completion_v1` | `NOT_APPLICABLE` — G5 chua co |
| `MAX_RISK_CALCULATION_VERSION` | `max_risk_daily_v1` | `MAX(risk_level)` per local date |
| `CHAT_ANALYSIS_SCHEMA_VERSION` (external) | `V1` | Tu G3-T02, khong thuoc G4 |

### 2.3 Versioning rules (theo contract §12.3)

Khi cong thuc thay doi:

1. Tao version moi (`*_v2`).
2. KHONG doi nghia version cu — giu cho audit / recompute.
- Ghi version vao `user_daily_features.calculation_version` / profile record (T02/T09 quyet dinh column name cu the).
- Recompute du lieu cu chi khi co task / policy ro rang.

---
## §3. Definitions: Explicit / Inferred / Behavioral

Tai lieu nay phan biet 3 loai nguon du lieu theo contract §4.

### 3.1 Explicit data

User tu khai bao truc tiep qua Daily Question.

| Loai | Source that | DB column |
|---|---|---|
| Stress | `daily_question_templates.code=`'STRESS'` v1 SCALE` | `daily_question_answers.numeric_value` (NUMERIC, 1—5) |
| Mood | `daily_question_templates.code=`'MOOD'` v1 SINGLE_CHOICE` | `daily_question_answers.option_value` (VARCHAR(50), `'1'..`'5'`) |
| Energy | `daily_question_templates.code=`'ENERGY'` v1 SCALE` | `daily_question_answers.numeric_value` (NUMERIC, 1—5) |
| Sleep (duration, hien tai) | `daily_question_templates.code=`'SLEEP'` v1 NUMBER` | `daily_question_answers.numeric_value` (NUMERIC, 0—24) |

**Quy tac**: Explicit co do uu tien cao hon inferred khi cung mo ta 1 feature.

### 3.2 Inferred data

Du lieu suy ra tu AI pipeline (chat analysis) hoac keyword/regex pre-filter.

| Loai | Source that | DB column |
|---|---|---|
| Chat Analysis result | `chat_analysis_results` (effective ACTIVE row) | columns: `topic`, `emotion`, `intent`, `signals JSONB`, `evidence_spans JSONB`, `model_risk_level SMALLINT 1..4`, `confidence NUMERIC(4,3)`, `schema_version` |
| Keyword/regex signal | `safety_keyword_rules` (status='APPROVED') + matched on text | (khong persist; tinh runtime tu pre-filter) |
| Final risk | `risk_state_history.risk_level` (resolved by G3 Safety Resolver) | SMALLINT 1—4 |

Moi inferred value phai co **companion fields**:

```text
confidence
source
schema_version
model_version          (chat_analysis_results.model_risk_level → V16 khong luu model_version; dung schema_version='V1' + future provider version)
prompt_version         (chi ap dung neu feature phu thuoc LLM classifier)
analysis_result_id hoac source reference
```

**Quy tac (contract §4.2)**: Khong duoc tao inferred field moi neu G3 schema chua co field tuong ung. Doi voi `anxiety_signal` (xem §6.5), tin hieu duoc derive tu cac field co san trong `chat_analysis_results` — khong tao field moi.

### 3.3 Behavioral data

Du lieu tong hop tu hanh vi cua user (event counts).

| Event | `behavioral_events.event_type` | `source_type` |
|---|---|---|
| Chat session started | `CHAT_SESSION_STARTED` | `CHAT_SESSION` |
| Chat message sent | `CHAT_MESSAGE_SENT` | `CONVERSATION_MESSAGE` |
| Daily check-in completed | `DAILY_CHECKIN_COMPLETED` | `DAILY_QUESTION_ANSWER` |
| Daily check-in skipped | `DAILY_CHECKIN_SKIPPED` | `DAILY_QUESTION_ASSIGNMENT` |
| Exercise (FUTURE — G5) | `EXERCISE_ASSIGNED/STARTED/COMPLETED/SKIPPED` | (G5) |
| Program (FUTURE — G6) | `PROGRAM_ACCEPTED/PAUSED` | (G6) |

**Quy tac (contract §4.3)**: Behavioral data **khong** duoc chua raw chat content. `properties` JSONB chi chua metadata (duration_ms, message_length bucketed, ...) — KHONG text snippet, KHONG hash of content ngoai tru SHA-256 cua `evidenceSpans` (da co tu G3-T02).

### 3.4 Bang phan loai 8 feature

| Feature | Classification | Primary | Supporting |
|---|---|---|---|
| `stress` | **explicit** | Daily Answer | Chat Analysis inferred (chua map duoc, xem §6.1) |
| `mood` | **explicit** | Daily Answer | Chat Analysis emotion (chua map duoc, xem §6.2) |
| `energy` | **explicit** | Daily Answer | `UNAVAILABLE` (xem §6.3) |
| `sleep` | **explicit** | Daily Answer (duration only trong MVP) | `UNAVAILABLE` cho quality (xem §6.4) |
| `anxiety_signal` | **inferred** | Chat Analysis | Keyword/regex (G3-T08) |
| `engagement` | **behavioral** | `behavioral_events` (CHAT + DAILY_CHECKIN) | — |
| `exercise_completion` | **behavioral** (FUTURE) | G5 `exercise_*` (chua co) | — |
| `max_risk` | **safety-derived observable** (khong thuoc 3 loai tren) | `risk_state_history` | `safety_events` (audit) |

> Ghi chu: `max_risk` khong thuoc explicit / inferred / behavioral. No la observable an toan tu G3 Safety Resolver — khong phai user self-report, khong phai AI classification, khong phai event count.

---
## §4. Null / Unknown / Zero / NOT_APPLICABLE

**Diem quan trong nhat cua tai lieu nay** — theo yeu cau task spec "hai dev hieu giong nhau ve null/unknown/zero". Bang nay la BAT BUOC, khong thay doi khi implement.

### 4.1 Bon gia tri, bon cach hieu

| Symbol | Layer | Dung khi | Visible cho frontend? | Dung trong average/trend/profile? |
|---|---|---|---|---|
| `null` (DB column) | DB | Khong co du du lieu / feature chua tinh / nguon chua ton tai | Map sang `UNKNOWN` o DTO | **KHONG** (bo qua, khong tham gia phep tinh) |
| `UNKNOWN` (API/DTO string) | API boundary | Tuong duong `null` o API | Co | **KHONG** |
| `0` (numeric) | DB / API | **Chi khi** du lieu nguon thuc su chung minh gia tri thap nhat = 0 (vd user thuc su khong co chat session, khong co checkin nao completed) | Co | Co (neu thuc su co data) |
| `NOT_APPLICABLE` (string enum) | API/DTO | Feature khong ap dung (vd `exercise_completion` khi user chua co program) | Co | **KHONG** |

### 4.2 Mandatory rule

```text
NEVER convert null to 0
for average, trend, profile, or confidence calculation.
```

Day la loi nghiem trong nhat co the xay ra o G4. VD:

- User khong tra loi Daily Question trong ngay → `stress_raw = null` → `stress_score = null` (DB) → DTO `UNKNOWN`. **KHONG** duoc set `stress_score = 0`.
- User khong co chat session trong ngay → `engagement_score` raw inputs rong → `engagement_score = null / UNKNOWN`. **KHONG** duoc set 0.
- User chua co program → `exercise_completion = NOT_APPLICABLE`. **KHONG** duoc set 0.

### 4.3 Map DB → DTO

| DB column value | DTO / API field value |
|---|---|
| `NULL` | `"UNKNOWN"` (string, cho explicit/inferred/behavioral) hoac `"NOT_APPLICABLE"` (khi feature khong ap dung) |
| `0.000` (numeric) | `0.000` (giu nguyen — day la du lieu that) |
| `0.000` trong DB do loi code (null→0 conversion) | **Day la BUG** — phai sua, khong map |

### 4.4 Phan biet feature-level vs record-level

- **Feature-level missing**: feature chua ap dung cho user (vd `exercise_completion` khi G5 chua co assignment) → `NOT_APPLICABLE` o moi record cho den khi dieu kien thay doi.
- **Record-level missing**: record cu the cho 1 user × 1 local_date bi thieu du lieu nguon → `null` o column → `UNKNOWN` o DTO.

---
## §5. Feature Catalog (bang tong 8 feature)

| # | Feature | Type | Primary source | Supporting | Raw | Aggregated | Range | Polarity | Missing |
|---|---|---|---|---|---|---|---|---|---|
| 1 | `stress` | explicit | `daily_question_answers` (numeric, template `STRESS` v1) | — (xem §6.1 vi G3 schema khong map truc tiep) | `NUMERIC` 1-5 | `stress_score NUMERIC(4,3)` | 0.000-1.000 | `HIGHER_IS_WORSE` | `null / UNKNOWN` |
| 2 | `mood` | explicit | `daily_question_answers` (option, template `MOOD` v1) | — (xem §6.2) | VARCHAR option `1`..`5` | `mood_score NUMERIC(4,3)` | 0.000-1.000 | `HIGHER_IS_BETTER` | `null / UNKNOWN` |
| 3 | `energy` | explicit | `daily_question_answers` (numeric, template `ENERGY` v1) | `UNAVAILABLE` (G3 khong co energy_signal field) | `NUMERIC` 1-5 | `energy_score NUMERIC(4,3)` | 0.000-1.000 | `HIGHER_IS_BETTER` | `null / UNKNOWN` |
| 4 | `sleep` | explicit (duration only trong MVP) | `daily_question_answers` (numeric, template `SLEEP` v1) | `UNAVAILABLE` cho quality (SLEEP_QUALITY template chua seed) | `NUMERIC` 0-24 (hours) | `sleep_score NUMERIC(4,3)` (tu quality, hien khong tinh duoc) | 0.000-1.000 | `HIGHER_IS_BETTER` | `null / UNKNOWN` |
| 5 | `anxiety_signal` | inferred | `chat_analysis_results` (effective ACTIVE) | `safety_keyword_rules` matches | derived NUMERIC(4,3) | `anxiety_signal NUMERIC(4,3)` | 0.000-1.000 | `HIGHER_IS_WORSE` | `null / UNKNOWN` |
| 6 | `engagement` | behavioral | `behavioral_events` (CHAT_SESSION_STARTED, CHAT_MESSAGE_SENT, DAILY_CHECKIN_COMPLETED, DAILY_CHECKIN_SKIPPED) | — | counts + ratios | `engagement_score NUMERIC(4,3)` | 0.000-1.000 | `HIGHER_IS_MORE` | `null / UNKNOWN` |
| 7 | `exercise_completion` | behavioral (FUTURE) | G5 `exercise_*` (chua co) | — | completed/assigned ratio | `exercise_completion_ratio NUMERIC(5,4)` | 0.0000-1.0000 | `HIGHER_IS_BETTER` | `null / NOT_APPLICABLE` |
| 8 | `max_risk` | safety-derived | `risk_state_history.risk_level` | `safety_events.risk_level` (audit only) | `SMALLINT` 1-4 | `max_risk_level SMALLINT` | 1-4 | `HIGHER_IS_WORSE` | `null / UNKNOWN` |

---

## §6. Per-Feature Specification

Moi subsection duoi day la spec day du cho 1 feature. Tat ca cong thuc normalization duoc phe duyet cho `normalization_v1` (theo contract §13.2 — phep bien doi ky thuat, khong phai clinical threshold).
### 6.1 stress

#### 6.1.1 Source

| Layer | Source |
|---|---|
| Primary (explicit) | `daily_question_answers` join `daily_question_templates` where `code='STRESS' version=1`. Lay `numeric_value NUMERIC`. |
| Supporting (inferred) | `UNAVAILABLE trong G4 v1` — contract §5.1 cho phep dung Chat Analysis stress signal, nhung `chat_analysis_results` khong co field `stress_signal` rieng (chi co `signals JSONB` array of enum, khong phai score). G4 v1 KHONG map signals → stress score. (xem §10 B.2) |

#### 6.1.2 Raw value

| Field | Value |
|---|---|
| Type | NUMERIC |
| Range | 1-5 (tu `daily_question_templates.scale_min=1, scale_max=5` o V10 backfill) |
| Stored at | `daily_question_answers.numeric_value` |
| Contract divergence | Contract §8.1 ghi `Integer 1-10`; G2 seed la 1-5. T01 ghi `1-5` khop thuc te (xem §10 B.1). |

#### 6.1.3 Aggregated value

| Field | Value |
|---|---|
| Name | `stress_score` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_WORSE` |

**Cong thuc (`normalization_v1`)**:

```text
stress_score = (numeric_value - 1) / 4.0
```

#### 6.1.4 Missing semantics

| Situation | Value |
|---|---|
| `daily_question_answers` khong co row cho (user, STRESS, local_date) | `stress_score = null` (DB) → `"UNKNOWN"` (DTO) |
| User chua tra loi template STRESS | `stress_score = null` → `"UNKNOWN"` |
| Template STRESS bi RETIRED truoc assignment date | `stress_score = null` → `"UNKNOWN"` (giu lich su assignment cu, khong recompute) |

**KHONG BAO GIO** set `stress_score = 0` khi missing (xem §4.2).

#### 6.1.5 Companion fields (theo contract §8.1)

| Field | Required | Notes |
|---|---|---|
| `stress_raw_value` (mirror) | optional | Luu raw NUMERIC de recompute khi `stress_calculation_vN` bump |
| `stress_score_source` | required | enum: `DAILY_ANSWER` \\| `INFERRED` \\| `NONE` |
| `stress_score_calculation_version` | required | default `normalization_v1` |
| `stress_score_polarity` | required (denormalized) | `HIGHER_IS_WORSE` |

#### 6.1.6 Test case

```text
GIVEN  daily_question_answers row (user_id=U, assignment_id=A,
        numeric_value=4, template.code='STRESS' version=1)
        for assigned_for_date = D

WHEN   compute_daily_stress(U, D)

THEN   stress_score = (4 - 1) / 4.0 = 0.750
       stress_score_source = DAILY_ANSWER
       stress_score_calculation_version = normalization_v1
       stress_score_polarity = HIGHER_IS_WORSE
```

```text
GIVEN  no daily_question_answers row for (U, STRESS, D)

WHEN   compute_daily_stress(U, D)

THEN   stress_score = null (DB)
       DTO stress_score_value = "UNKNOWN"
       stress_score_source = NONE
```
### 6.2 mood

#### 6.2.1 Source

| Layer | Source |
|---|---|
| Primary (explicit) | `daily_question_answers` join `daily_question_templates` where `code='MOOD' version=1`. Lay `option_value VARCHAR(50)` thuoc `{'1','2','3','4','5'}`. |
| Supporting (inferred) | `UNAVAILABLE trong G4 v1` — contract §5.2 cho phep dung Chat Analysis mood/emotion signal, nhung contract cung cam "tu map emotion label sang mood score neu chua co mapping duoc duyet". G4 v1 KHONG map `chat_analysis_results.emotion` (enum) → `mood_score` (numeric). (xem §10 B.2) |

#### 6.2.2 Raw value

| Field | Value |
|---|---|
| Type | VARCHAR(50) — option value |
| Range | `'1'` (VERY_LOW) .. `'5'` (VERY_GOOD) |
| Mapping | `1=VERY_LOW, 2=LOW, 3=NEUTRAL, 4=GOOD, 5=VERY_GOOD` (G2 seed labels: 'Rat te','Te','Binh thuong','Tot','Rat tot') |
| Stored at | `daily_question_answers.option_value` |

#### 6.2.3 Aggregated value

| Field | Value |
|---|---|
| Name | `mood_score` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_BETTER` |

**Cong thuc (`normalization_v1`)**:

```text
mood_raw_score = parseInt(option_value)  -- 1..5
mood_score    = (mood_raw_score - 1) / 4.0
```

#### 6.2.4 Missing semantics

| Situation | Value |
|---|---|
| Khong co row Daily Answer MOOD | `mood_score = null` → `"UNKNOWN"` |
| `option_value` ngoai `1..5` (data loi) | `mood_score = null` → `"UNKNOWN"` (log warning, khong compute) |

**KHONG** set `mood_score = 0`.

#### 6.2.5 Companion fields

| Field | Required | Notes |
|---|---|---|
| `mood_raw_label` | optional | Enum mirror: `VERY_LOW`/`LOW`/`NEUTRAL`/`GOOD`/`VERY_GOOD` |
| `mood_score_source` | required | enum: `DAILY_ANSWER` \\| `INFERRED` \\| `NONE` |
| `mood_score_calculation_version` | required | default `normalization_v1` |

#### 6.2.6 Test case

```text
GIVEN  daily_question_answers row (U, A, option_value='4', template MOOD v1) for local_date D (4 = GOOD, label "Tot")

WHEN   compute_daily_mood(U, D)

THEN   mood_raw_score = 4
       mood_score = (4 - 1) / 4.0 = 0.750
       mood_score_source = DAILY_ANSWER
       mood_score_calculation_version = normalization_v1
```

```text
GIVEN  option_value='7' (invalid — outside 1..5)

WHEN   compute_daily_mood(U, D)

THEN   mood_score = null → "UNKNOWN"
       log warning: mood_raw_score_out_of_range
```
### 6.3 energy

#### 6.3.1 Source

| Layer | Source |
|---|---|
| Primary (explicit) | `daily_question_answers` join `daily_question_templates` where `code='ENERGY' version=1`. Lay `numeric_value NUMERIC`. |
| Inferred | `UNAVAILABLE` — contract §5.3 ghi ro "khong duoc tu bia field `energy_signal` hoac `fatigue_signal`"; G3 schema hien khong co field phu hop (xem §10 B.2). |

#### 6.3.2 Raw value

| Field | Value |
|---|---|
| Type | NUMERIC |
| Range | 1-5 (G2 seed `scale_min=1, scale_max=5`) |
| Stored at | `daily_question_answers.numeric_value` |

#### 6.3.3 Aggregated value

| Field | Value |
|---|---|
| Name | `energy_score` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_BETTER` |

**Cong thuc (`normalization_v1`)**:

```text
energy_score = (numeric_value - 1) / 4.0
```

#### 6.3.4 Missing semantics

| Situation | Value |
|---|---|
| Khong co Daily Answer ENERGY | `energy_score = null` → `"UNKNOWN"` |
| Inferred unavailable | `energy_score_source = NONE` (khong fallback) |

**KHONG** set `energy_score = 0`.

#### 6.3.5 Companion fields

| Field | Required | Notes |
|---|---|---|
| `energy_score_source` | required | enum: `DAILY_ANSWER` \\| `NONE` (INFERRED = UNAVAILABLE trong v1) |
| `energy_score_calculation_version` | required | `normalization_v1` |

#### 6.3.6 Test case

```text
GIVEN  numeric_value=3 (template ENERGY v1)

WHEN   compute_daily_energy(U, D)

THEN   energy_score = (3 - 1) / 4.0 = 0.500
       energy_score_source = DAILY_ANSWER
```

```text
GIVEN  no Daily Answer ENERGY

THEN   energy_score = null → "UNKNOWN"
       energy_score_source = NONE
```
### 6.4 sleep

#### 6.4.1 Source

| Layer | Source |
|---|---|
| Primary (explicit) | `daily_question_answers` join `daily_question_templates` where `code='SLEEP' version=1`. Lay `numeric_value NUMERIC` (don vi: gio). |
| Quality | `UNAVAILABLE trong G4 v1` — template `SLEEP_QUALITY` (SCALE 1-5) **chua duoc seed** trong MVP G2. (xem §10 B.3) |
| Supporting (inferred) | `UNAVAILABLE trong G4 v1` — contract §5.4 ghi ro "khong dung inferred signal thay the explicit duration" va "khong tu ket hop duration va quality thanh clinical sleep score". |

#### 6.4.2 Raw value

| Field | Value |
|---|---|
| Type | NUMERIC |
| Range | 0-24 (gio) |
| Stored at | `daily_question_answers.numeric_value` |
| Name in spec | `sleep_duration_hours` (mirror o `user_daily_features`) |

#### 6.4.3 Aggregated value

| Field | Value |
|---|---|
| Name | `sleep_score` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_BETTER` |
| Calculation version | `sleep_quality_v1` (chuan hoa tu quality 1-5) |

**Trang thai hien tai**: `sleep_score` **khong compute duoc** trong MVP vi thieu `SLEEP_QUALITY` template. Tat ca record tra ve `null / UNKNOWN` cho den khi:

1. Admin/expert seed `daily_question_templates.code='SLEEP_QUALITY' version=1 SCALE scale_min=1 scale_max=5`.
2. User tra loi ca SLEEP va SLEEP_QUALITY Daily Question.

**Cong thuc** (chi apply khi co quality):

```text
sleep_score = (sleep_quality_raw - 1) / 4.0
```

#### 6.4.4 Mirror raw fields

| Field | Type | Range | Nullable | Notes |
|---|---|---|---|---|
| `sleep_duration_hours` | NUMERIC(4,2) | 0.00-24.00 | yes | Mirror cua raw duration (chi de dashboard hien thi, KHONG dung de tinh `sleep_score`) |
| `sleep_quality_raw` | SMALLINT | 1-5 | yes | Hien **luon null** — SLEEP_QUALITY template chua seed |

#### 6.4.5 Missing semantics

| Situation | Value |
|---|---|
| Khong co Daily Answer SLEEP | `sleep_duration_hours = null`, `sleep_score = null` → `"UNKNOWN"` |
| Co SLEEP nhung chua co SLEEP_QUALITY | `sleep_duration_hours = 7.50`, `sleep_score = null` → `"UNKNOWN"` |
| SLEEP duration outside 0-24 (data loi) | log warning, `sleep_duration_hours = null` → `"UNKNOWN"` |

**KHONG** set `sleep_score = 0` khi missing.

#### 6.4.6 Companion fields

| Field | Required | Notes |
|---|---|---|
| `sleep_duration_hours` | required | mirror raw |
| `sleep_quality_raw` | optional | hien luon null |
| `sleep_score_calculation_version` | required | `sleep_quality_v1` (hoac UNKNOWN khi khong compute duoc) |

#### 6.4.7 Test case

```text
GIVEN  numeric_value=7.5 (SLEEP template v1)
       AND no SLEEP_QUALITY answer

WHEN   compute_daily_sleep(U, D)

THEN   sleep_duration_hours = 7.50
       sleep_quality_raw   = null
       sleep_score         = null → "UNKNOWN"
       sleep_score_calculation_version = sleep_quality_v1
       (note: DTO must expose sleep_duration_hours=7.50 so
        dashboard can show raw even when sleep_score is UNKNOWN)
```

```text
FUTURE CASE (when SLEEP_QUALITY template is seeded):
GIVEN  SLEEP_QUALITY answer = 4

WHEN   compute_daily_sleep(U, D)

THEN   sleep_quality_raw = 4
       sleep_score = (4 - 1) / 4.0 = 0.750
       sleep_score_calculation_version = sleep_quality_v1
```
### 6.5 anxiety_signal

#### 6.5.1 Source

| Layer | Source |
|---|---|
| Primary (inferred) | `chat_analysis_results` (effective ACTIVE row, versioned by `schema_version`). Derive tu: `signals JSONB`, `emotion VARCHAR`, `model_risk_level SMALLINT`, `confidence NUMERIC(4,3)`. |
| Supporting (inferred) | `safety_keyword_rules` matches (G3-T08). Dem so rule APPROVED match trong text → supporting evidence. |
| Trigger | Moi `chat_analysis_results` ACTIVE row trong local_date. |

#### 6.5.2 Raw value

Day la feature **derived** — khong co 1 raw scalar duy nhat. Raw inputs:

| Input | Source | Type |
|---|---|---|
| `signals` (array) | `chat_analysis_results.signals` JSONB | array of enum: `FATIGUE, SLEEP_DISRUPTION, ISOLATION, HOPELESSNESS, BURNOUT, SELF_HARM_RISK, CONFLICT, GRIEF, OTHER` |
| `emotion` | `chat_analysis_results.emotion` VARCHAR(20) | enum: `NEUTRAL, HAPPY, ANXIOUS, SAD, OVERWHELMED, DISTRESS, ANGRY` |
| `model_risk_level` | `chat_analysis_results.model_risk_level` SMALLINT | 1-4 |
| `confidence` | `chat_analysis_results.confidence` NUMERIC(4,3) | 0.000-1.000 |
| `keyword_match_count` | runtime tu `safety_keyword_rules` | integer |

#### 6.5.3 Aggregated value

| Field | Value |
|---|---|
| Name | `anxiety_signal` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_WORSE` |
| Aggregation scope | Daily = MAX of all ACTIVE chat_analysis_results in local_date; Window (T06) = MAX of daily values |

**Cong thuc** (chua duoc chuyen gia duyet):

```text
anxiety_signal =
    f(emotion, signals, model_risk_level, confidence, keyword_match_count)
    -- exact formula = CONFIG_PLACEHOLDER
    -- NOT IMPLEMENTED IN G4 v1 CODE — placeholder in calculator
```

Ly do placeholder:

- Contract §8.5 ghi `MIN_INFERRED_CONFIDENCE = TODO_EXPERT_REVIEW`.
- Cong thuc combine confidence + signals + emotion chua co chuyen gia duyet.
- Khong duoc hard-code cong thuc chua duyet trong code (rule 00 + contract §13).

G4 v1 implementation se:
- Aggregate max emotion/signal/model_risk_level per day.
- Tra `anxiety_signal = null` cho den khi `MIN_INFERRED_CONFIDENCE` va combine formula duoc chuyen gia duyet.
- Ghi `anxiety_signal_calculation_version = TODO_EXPERT_REVIEW` de de filter.

#### 6.5.4 Companion fields (theo contract §8.5)

| Field | Required | Notes |
|---|---|---|
| `anxiety_signal_confidence` | required | MAX(confidence) of contributing chat_analysis_results |
| `anxiety_signal_source` | required | enum: `CHAT_ANALYSIS`, `KEYWORD_PRE_FILTER`, `COMBINED`, `NONE` |
| `analysis_result_id` | required (nullable) | Latest ACTIVE chat_analysis_results.id contributing |
| `schema_version` | required | `chat_analysis_schema_v1` (tu `chat_analysis_results.schema_version`) |
| `model_version` | required (nullable) | Provider version (future — V16 chua luu) |
| `prompt_version` | required (nullable) | `chat_analysis_prompt_v1` (neu dung LLM) |
| `anxiety_signal_calculation_version` | required | `TODO_EXPERT_REVIEW` cho v1 |

#### 6.5.5 Missing semantics

| Situation | Value |
|---|---|
| Khong co ACTIVE chat_analysis_results trong ngay | `anxiety_signal = null` → `"UNKNOWN"` |
| Tat ca ACTIVE rows co `confidence < MIN_INFERRED_CONFIDENCE` | `anxiety_signal = null` → `"UNKNOWN"` (cho threshold) |
| G3 schema khong co field phu hop | `anxiety_signal = null` → `"UNKNOWN"` (da doi chieu — xem §10 B.2) |

**KHONG** set `anxiety_signal = 0` khi missing.

#### 6.5.6 Constraints (theo contract §8.5)

- **KHONG** dung boolean (`is_anxious: true/false`).
- **KHONG** dung scale 1-4.
- **KHONG** hieu la chan doan anxiety disorder (rule 00).
- `MIN_INFERRED_CONFIDENCE = TODO_EXPERT_REVIEW`.

#### 6.5.7 Test case

```text
GIVEN  3 ACTIVE chat_analysis_results trong local_date D:
        - row 1: emotion=NEUTRAL, signals=[], confidence=0.9
        - row 2: emotion=ANXIOUS, signals=[ISOLATION], confidence=0.85
        - row 3: emotion=NEUTRAL, signals=[], confidence=0.95
       AND MIN_INFERRED_CONFIDENCE = 0.80 (placeholder for test)

WHEN   compute_daily_anxiety_signal(U, D)

THEN   anxiety_signal_calculation_version = TODO_EXPERT_REVIEW
       anxiety_signal = null  -- formula not yet defined
       anxiety_signal_confidence = MAX(0.9, 0.85, 0.95) = 0.95
       anxiety_signal_source = CHAT_ANALYSIS
       analysis_result_id = id of row 3 (latest by created_at)
       DTO anxiety_signal_value = "UNKNOWN"
```

```text
GIVEN  no ACTIVE chat_analysis_results trong local_date D

WHEN   compute_daily_anxiety_signal(U, D)

THEN   anxiety_signal = null
       anxiety_signal_confidence = null
       anxiety_signal_source = NONE
       DTO = "UNKNOWN"
```
### 6.6 engagement

#### 6.6.1 Source

| Layer | Source |
|---|---|
| Primary (behavioral) | `behavioral_events` trong local_date D voi `event_type IN ('CHAT_SESSION_STARTED','CHAT_MESSAGE_SENT','DAILY_CHECKIN_COMPLETED','DAILY_CHECKIN_SKIPPED')`. |
| Excluded trong v1 | `EXERCISE_*`, `PROGRAM_*`, `RECOMMENDATION_*` — G5/G6 chua co. KHONG coi thieu exercise data la 0 (contract §5.6, §16 item 10). |

#### 6.6.2 Raw components

| Component | Source | Definition |
|---|---|---|
| `message_count` | COUNT `CHAT_MESSAGE_SENT` | So message trong ngay |
| `active_chat_session_count` | COUNT DISTINCT source_id cua `CHAT_SESSION_STARTED` | So session trong ngay |
| `active_chat_day_count` | derived | 1 neu `message_count > 0` ELSE 0 (chi dung o window aggregation) |
| `checkin_assigned_count` | COUNT `daily_question_assignments` for (U, D) | Tong assignment |
| `checkin_completed_count` | COUNT `DAILY_CHECKIN_COMPLETED` | Tong completed |
| `checkin_completion_ratio` | `checkin_completed_count / checkin_assigned_count` | Ratio, NULL khi `checkin_assigned_count = 0` |

**Quan trong**: raw components phai tinh **tren tat ca event** trong local_date theo `users.timezone`, KHONG dung `LocalDate.now()` / `ZoneId.systemDefault()` (contract §6.9).

#### 6.6.3 Aggregated value

| Field | Value |
|---|---|
| Name | `engagement_score` |
| Type | NUMERIC(4,3) |
| Range | 0.000-1.000 |
| Polarity | `HIGHER_IS_MORE` |
| Calculation version | `engagement_v1_chat_checkin` |

**Cong thuc** (`CONFIG_PLACEHOLDER`):

```text
engagement_score =
    w_message * normalize(message_count)
  + w_session * normalize(active_chat_session_count)
  + w_checkin_ratio * checkin_completion_ratio

Trong do:
  - w_message, w_session, w_checkin_ratio = CONFIG_PLACEHOLDER (T08 chot)
  - normalize(x) = CONFIG_PLACEHOLDER (cap hoac log — T08 chot)
  - checkin_completion_ratio = NULL khi checkin_assigned_count = 0
    → dung weight-adjusted handling = CONFIG_PLACEHOLDER
```

G4 v1 implementation:
- Persist raw components.
- `engagement_score = null` cho den khi T08 chot weights + normalize function.
- Ghi `engagement_score_calculation_version = engagement_v1_chat_checkin` (da dat ten, weights pending).

#### 6.6.4 Missing semantics

| Situation | Value |
|---|---|
| `behavioral_events` rong cho (U, D) | `engagement_score = null` → `"UNKNOWN"` (KHONG phai 0) |
| Co events nhung weights chua chot | `engagement_score = null` → `"UNKNOWN"` |

**KHONG** set `engagement_score = 0` khi missing (loi nghiem trong nhat o G4).

#### 6.6.5 Companion fields

| Field | Required | Notes |
|---|---|---|
| `message_count` | required | raw |
| `active_chat_session_count` | required | raw |
| `checkin_assigned_count` | required | raw |
| `checkin_completed_count` | required | raw |
| `checkin_completion_ratio` | required (nullable) | NULL khi assigned=0 |
| `engagement_score_calculation_version` | required | `engagement_v1_chat_checkin` |

#### 6.6.6 Test case

```text
GIVEN  behavioral_events trong (U, D):
        - CHAT_SESSION_STARTED x 1 (session_id=S)
        - CHAT_MESSAGE_SENT x 5 (distinct session_id=S)
        - DAILY_CHECKIN_COMPLETED x 3
        - DAILY_CHECKIN_SKIPPED x 0

WHEN   compute_daily_engagement(U, D)

THEN   raw components:
        message_count             = 5
        active_chat_session_count = 1
        checkin_assigned_count    = 3 (from daily_question_assignments for D)
        checkin_completed_count   = 3
        checkin_completion_ratio  = 3/3 = 1.000
       engagement_score = null  -- weights pending T08
       engagement_score_calculation_version = engagement_v1_chat_checkin
       DTO = "UNKNOWN"
```

```text
GIVEN  no behavioral_events for (U, D)

WHEN   compute_daily_engagement(U, D)

THEN   engagement_score = null → "UNKNOWN"
       (NOT 0)
```
### 6.7 exercise_completion

#### 6.7.1 Source

| Layer | Source |
|---|---|
| Primary (behavioral, FUTURE) | `behavioral_events.EXERCISE_ASSIGNED` + `EXERCISE_COMPLETED` + `EXERCISE_SKIPPED`. **G5 chua trien khai** → khong co source data. |

#### 6.7.2 Raw value

| Field | Value |
|---|---|
| Type | derived ratio |
| Inputs | `EXERCISE_COMPLETED count` / `EXERCISE_ASSIGNED count` |
| Status MVP | `UNAVAILABLE` |

#### 6.7.3 Aggregated value

| Field | Value |
|---|---|
| Name | `exercise_completion_ratio` |
| Type | NUMERIC(5,4) |
| Range | 0.0000-1.0000 |
| Polarity | `HIGHER_IS_BETTER` |
| Calculation version | `exercise_completion_v1` |

**Cong thuc** (khi G5 ship):

```text
exercise_completion_ratio =
    completed_required_exercises
  / assigned_required_exercises
```

Display:

```text
percentage = ratio * 100
```

#### 6.7.4 Missing semantics

| Situation | Value |
|---|---|
| G5 chua co assignment | `exercise_completion_ratio = null` (DB) → `"NOT_APPLICABLE"` (DTO) |
| Co assignment nhung user chua complete | `exercise_completion_ratio = 0.0000` (gia tri that) |
| `assigned_required_exercises = 0` | `exercise_completion_ratio = null` → `"NOT_APPLICABLE"` (khong tinh 0/0) |

**KHONG BAO GIO** set 0 khi G5 chua co assignment. Loi nay se keo G6 Program Matching di sai huong.

#### 6.7.5 Companion fields

| Field | Required | Notes |
|---|---|---|
| `exercise_completion_ratio` | required | `null` cho den khi G5 |
| `exercise_completion_calculation_version` | required | `exercise_completion_v1` |
| `exercise_completion_status` | required | enum: `NOT_APPLICABLE` \\| `COMPUTED` |

#### 6.7.6 Test case

```text
GIVEN  G5 not implemented (current state)

WHEN   compute_daily_exercise_completion(U, D)

THEN   exercise_completion_ratio = null
       exercise_completion_status = NOT_APPLICABLE
       DTO = "NOT_APPLICABLE"
```

```text
FUTURE CASE (when G5 ships):
GIVEN  EXERCISE_ASSIGNED = 10 (required)
        EXERCISE_COMPLETED = 7
        EXERCISE_SKIPPED = 2
        (remaining 1 not yet acted on)

WHEN   compute_daily_exercise_completion(U, D)

THEN   exercise_completion_ratio = 7 / 10 = 0.7000
       exercise_completion_status = COMPUTED
       DTO exercise_completion_value = 0.7000
       percentage display = 70.0
```
### 6.8 max_risk

#### 6.8.1 Source

| Layer | Source |
|---|---|
| Primary | `risk_state_history.risk_level` (append-only, resolved by G3 Safety Resolver) |
| Secondary (audit only) | `safety_events.risk_level` (denormalized snapshot) |

Contract §5.8 da chot: uu tien `risk_state_history.final_risk_level` (column name trong V14: `risk_level`). `safety_events.risk_level` chi dung audit, KHONG dung thay the tru khi G3 da dinh nghia ro la source final-risk day du (hien KHONG).

#### 6.8.2 Raw value

| Field | Value |
|---|---|
| Type | SMALLINT |
| Range | 1-4 |
| Definition | Risk level cuoi cung do Safety Resolver quyet dinh (per docs/04 §3) |
| Stored at | `risk_state_history.risk_level` |

#### 6.8.3 Aggregated value

| Field | Value |
|---|---|
| Name | `max_risk_level` |
| Type | SMALLINT |
| Range | 1-4 |
| Polarity | `HIGHER_IS_WORSE` |
| Calculation version | `max_risk_daily_v1` |

**Dinh nghia da chot (Phase 1 §H.1)**:

```text
max_risk_level =
    MAX(risk_state_history.risk_level)
    WHERE occurred_at::local_date = feature_date
```

Ly do chon MAX trong ngay (khong phai current-at-EOD):

- `risk_state_history` la append-only; moi event ghi 1 row.
- "Current tai EOD" phu thuoc `00:00:00 theo TZ user` — neu co level 3 event luc 23:55 thi "EOD snapshot" dung la 3, nhung neu scheduler chay luc 00:30 ngay hom sau va user khong co event moi, snapshot van pick up row 23:55 → MAX trong ngay D = 3 cung cho ket qua 3. Hai cach trung nhau trong happy path.
- MAX idempotent voi late-arriving data → fit T05 idempotency.
- Phu hop voi semantic "canh bao cao nhat trong ngay" (most-severe-wins).

**Window (T06 quyet dinh chi tiet)**:

```text
max_risk_7d  = MAX(max_risk_level) for last 7 local_dates
max_risk_30d = MAX(max_risk_level) for last 30 local_dates
```

#### 6.8.4 Missing semantics

| Situation | Value |
|---|---|
| Khong co `risk_state_history` row nao trong ngay | `max_risk_level = null` → `"UNKNOWN"` |
| User chua tung chat (chua trigger resolver) | `max_risk_level = null` → `"UNKNOWN"` (toan bo lich su = UNKNOWN) |

**KHONG BAO GIO** default ve level 1 khi khong co du lieu (contract §8.8). Day la loi nghiem trong vi "level 1 = binh thuong" la assumption sai khi khong co evidence.

**KHONG** normalize trong database.

#### 6.8.5 Companion fields

| Field | Required | Notes |
|---|---|---|
| `max_risk_level` | required | SMALLINT 1-4 hoac null |
| `max_risk_calculation_version` | required | `max_risk_daily_v1` |
| `risk_event_count` | required | COUNT contributing rows (for audit) |

#### 6.8.6 Test case

```text
GIVEN  risk_state_history rows in (U, D):
        - occurred_at=08:15 risk_level=1
        - occurred_at=14:30 risk_level=3
        - occurred_at=22:00 risk_level=2

WHEN   compute_daily_max_risk(U, D)

THEN   max_risk_level = 3
       risk_event_count = 3
       max_risk_calculation_version = max_risk_daily_v1
```

```text
GIVEN  no risk_state_history rows in (U, D)
        AND user has 1 row at occurred_at=08:15 yesterday (risk_level=2)

WHEN   compute_daily_max_risk(U, D)

THEN   max_risk_level = null → "UNKNOWN"
       (NOT 1, NOT 2 from yesterday)
```
---

## §7. Explicit vs Inferred Combination Policy

Theo contract §11. Ap dung cho `stress`, `mood`, `sleep` (co ca explicit primary + supporting inferred trong tuong lai). Hien tai khong co feature nao thuc su kich hoat inferred fallback (xem §6.1, §6.2 — inferred UNAVAILABLE trong v1), nhung policy phai duoc ghi tuong minh de T04 tham chieu.

### 7.1 Priority

```text
Explicit > Inferred
```

### 7.2 Initial rule

```text
IF explicit value exists for (user, local_date, feature):
    final value     = explicit value
    inferred value  = supporting evidence only (record separately)
ELSE IF inferred value exists AND confidence >= MIN_INFERRED_CONFIDENCE:
    final value     = inferred value
ELSE:
    final value     = null / UNKNOWN
```

### 7.3 Conflicting sources

Neu explicit va inferred lech dang ke (vd raw stress explicit = 3/5 nhung inferred signals ra level 3):

- Explicit van la final value.
- Luu `source_conflict = true` (flag trong row).
- KHONG tu average 2 nguon.
- KHONG tu giam confidence bang cong thuc chua duoc duyet.
- Cong thuc combine confidence de **T11 quyet dinh**.

### 7.4 Prohibited behaviors (theo contract §11.4)

- KHONG set `missing explicit = 0`.
- KHONG set `missing inferred = 0`.
- KHONG dung `explicit + inferred / 2` lam default.
- KHONG dung inferred thay explicit khi explicit ton tai.

---
## §8. Timezone & Local-Date Policy

Theo contract §6. Tai lieu nay KHONG dinh nghia lai policy; chi mirror cho implementation.

### 8.1 User timezone

- Moi user phai co timezone dang IANA Zone ID (`Asia/Ho_Chi_Minh`).
- **Source of truth**: `users.timezone` (column `VARCHAR(50) NOT NULL DEFAULT 'UTC'` o V7).
- Hien tai G2 default `'UTC'` cho existing rows. T01 **khong thay doi** default.
- Neu can `Asia/Ho_Chi_Minh` cho demo: phai update `users.timezone` qua profile API (G2-T05+) — KHONG tu default trong code.

### 8.2 Timestamp storage

- Tat ca `occurred_at`, `created_at`, `updated_at`, `submitted_at`: luu bang UTC `TIMESTAMPTZ`.
- Schema DB da conform (V2, V7, V11, V14, V16, V17 deu dung `TIMESTAMPTZ`).

### 8.3 Feature date

```text
feature_date =
    convert(occurred_at AT TIME ZONE users.timezone)
    → DATE
```

KHONG dung system default timezone.

### 8.4 Local-day boundary

```text
[local_date 00:00:00, next_local_date 00:00:00)
```
theo `users.timezone`.

### 8.5 Scheduler policy

- Scheduler co the chay theo UTC.
- Aggregation **PHAI** chay per-user per-local-date per-user-timezone.

### 8.6 Late-arriving data

- Du lieu den muon thuoc ve local_date thuc te theo `occurred_at`.
- KHONG bi chuyen sang ngay job chay.
- Recompute theo `user_id + local_date`.

### 8.7 Timezone change

- Khi user doi timezone: du lieu lich su giu `feature_date` va timezone da dung truoc do.
- KHONG tu recompute toan bo lich su.
- Backfill/recompute = task rieng.

### 8.8 Code constraints

KHONG dung truc tiep:

```java
LocalDate.now()
ZoneId.systemDefault()
```

trong business calculator. Phai truyen ro:

```java
Clock clock
ZoneId userTimezone
```

---
## §9. Late-Arriving Data Policy

Theo contract §6.7. Ap dung cho tat ca feature.

### 9.1 Nguyen tac

- Late-arriving data (su kien xay ra trong qua khu nhung duoc insert vao DB hom nay, vd user offline → sync) thuoc ve local_date thuc te cua `occurred_at`.
- KHONG bi chuyen sang local_date cua job chay.

### 9.2 Recompute contract

- Moi daily feature row phai co unique key `(user_id, feature_date)` cho phep recompute idempotent.
- T05 quyet dinh cach persist + idempotency key (T01 khong define).

### 9.3 Window feature recompute

- Neu late event vao ngay D, recompute:
  - `daily_feature[D]` (da idempotent)
  - `window_7d[D-6..D]` neu D nam trong window
  - `window_30d[D-29..D]` neu D nam trong window
- Window features ngay khac KHONG bi anh huong.

---

## §10. Open Expert Decisions

Theo contract §13. Bang nay liet ke TAT CA decisions chua co chuyen gia duyet. Cursor KHONG duoc tu y thay doi cac gia tri nay.

### 10.1 Bang decision table (mirror contract §13.1)

| Decision code | Noi dung | Trang thai | Ap dung feature |
|---|---|---|---|
| `MIN_INFERRED_CONFIDENCE` | Confidence toi thieu de dung inferred signal | `TODO_EXPERT_REVIEW` | `anxiety_signal` |
| `MIN_TOPIC_CONFIDENCE` | Confidence toi thieu de tinh dominant topics | `TODO_EXPERT_REVIEW` | (T08) |
| `MIN_TREND_COVERAGE` | Coverage toi thieu de ket luan trend | `TODO_EXPERT_REVIEW` | (T07) |
| `TREND_DELTA_THRESHOLD` | Nguong phan biet `UP/DOWN/STABLE` | `TODO_EXPERT_REVIEW` | (T07) |
| `ENGAGEMENT_COMPONENT_WEIGHTS` | Trong so Chat va Daily Check-in | `CONFIG_PLACEHOLDER` | `engagement` |
| `ENGAGEMENT_NORMALIZE_FUNCTION` | Ham normalize(raw count) | `CONFIG_PLACEHOLDER` | `engagement` |
| `DATA_QUALITY_THRESHOLDS` | Nguong `SUFFICIENT/LOW/INSUFFICIENT` | `TODO_EXPERT_REVIEW` | (T11) |
| `HIGH_STRESS_THRESHOLD` | Nguong dien giai stress cao | `TODO_EXPERT_REVIEW` | `stress` |
| `SLEEP_DURATION_POLICY` | Cach dien giai thoi luong ngu | `TODO_EXPERT_REVIEW` | `sleep` |
| `SLEEP_COMBINATION_FORMULA` | Cong thuc ket hop duration + quality | `TODO_EXPERT_REVIEW` | `sleep` |
| `CONFIDENCE_COMBINATION_FORMULA` | Cong thuc combine confidence | `TODO_EXPERT_REVIEW` | (T11) |
| `CLINICAL_INTERPRETATION_LABELS` | Cac nhan dien giai chuyen mon | `TODO_EXPERT_REVIEW` | (T06/T07/T08) |
| `ANXIETY_SIGNAL_FORMULA` | Cong thuc derive tu signals+emotion+model_risk | `CONFIG_PLACEHOLDER` | `anxiety_signal` |

### 10.2 Technical normalization (duoc duyet cho `normalization_v1`)

| Feature | Formula | Version |
|---|---|---|
| stress | `(raw - 1) / 4.0` | `normalization_v1` |
| mood | `(raw - 1) / 4.0` | `normalization_v1` |
| energy | `(raw - 1) / 4.0` | `normalization_v1` |
| sleep_quality | `(quality - 1) / 4.0` | `sleep_quality_v1` (chua ap dung — SLEEP_QUALITY template chua seed) |

Day la **phep bien doi ky thuat**, KHONG phai clinical threshold (theo contract §13.2). Co the hard-code trong calculator.

### 10.3 Contract divergence duoc chot o T01 (khong phai conflict)

Sau khi inspect actual G2/G3 source, T01 xac nhan:

#### B.1 — Stress raw scale

| | Contract §8.1 | DB that (V6 + V10) |
|---|---|---|
| Range | `1-10` | `1-5` |

**Quyet dinh**: T01 ghi `stress` raw = `NUMERIC 1-5` khop G2 seed. Ly do: khong pha schema G2 da dong, khong tu tao field. Neu tuong lai can 1-10, do la 1 task seed moi (`STRESS v2 SCALE 1-10`), khong thuoc T01.

He qua: cong thuc `(raw - 1) / 4.0` (khong phai `/ 9.0` nhu contract §8.1 goc). Khi bump range → bump `stress_calculation_v2`.

#### B.2 — anxiety_signal khong co field rieng trong G3

| | Contract §5.5 | DB that (V16 + `chat_analysis_v1.dictionary.md`) |
|---|---|---|
| Source | "Latest effective versioned Chat Analysis result" | `chat_analysis_results` co: `topic, emotion, intent, signals JSONB, evidence_spans JSONB, model_risk_level, confidence, schema_version` — **khong co `anxiety_signal`** |

**Quyet dinh**: `anxiety_signal` derived tu cac field tren voi cong thuc = `CONFIG_PLACEHOLDER` cho den khi `ANXIETY_SIGNAL_FORMULA` duoc chuyen gia duyet. G4 v1 calculator tra `null / UNKNOWN` cho anxiety_signal, **khong** tu suy ra cong thuc.

#### B.3 — sleep khong co quality template trong MVP

| | Contract §8.4 | DB that (V6 + V10) |
|---|---|---|
| Raw | `sleep_quality_raw SMALLINT 1-5` + `sleep_duration_hours NUMERIC(4,2)` | `SLEEP` template = `NUMBER 0-24` (duration only) |

**Quyet dinh**: G4 v1 chi mirror `sleep_duration_hours` (raw). `sleep_score` tra `null / UNKNOWN` den khi template `SLEEP_QUALITY v1 SCALE 1-5` duoc seed boi admin/expert. Khi seed → bump calculation version van la `sleep_quality_v1` (cong thuc khong doi), chi them data source.

---
## §11. Test Cases (tong hop)

Moi feature co ≥ 1 test case "happy path" + ≥ 1 test case "missing data". Test case la **mo ta input → output**, khong phai JUnit code (T01 la task tai lieu, khong phai test task).

### 11.1 Bang tong test cases

| # | Feature | Input | Expected | Missing variant |
|---|---|---|---|---|
| 1 | stress | numeric=4 | score=0.750, source=DAILY_ANSWER | no row → null / UNKNOWN |
| 2 | mood | option='4' | score=0.750, source=DAILY_ANSWER | option='7' → null / UNKNOWN + log warning |
| 3 | energy | numeric=3 | score=0.500, source=DAILY_ANSWER | no row → null / UNKNOWN |
| 4 | sleep | numeric=7.5 | duration=7.50, score=null / UNKNOWN | no row → null / UNKNOWN |
| 5 | anxiety_signal | 3 ACTIVE rows | score=null (formula pending), conf=0.95 | no rows → null / UNKNOWN |
| 6 | engagement | 5 msg + 1 session + 3 checkins completed | score=null (weights pending) | no events → null / UNKNOWN |
| 7 | exercise_completion | G5 not implemented | score=null / NOT_APPLICABLE | — |
| 8 | max_risk | rows L1, L3, L2 | score=3, count=3 | no rows in day → null / UNKNOWN (NOT 1) |

### 11.2 Test case chi tiet xem §6.1.6, §6.2.6, §6.3.6, §6.4.7, §6.5.7, §6.6.6, §6.7.6, §6.8.6

---

## §12. Acceptance Checklist

Mirror contract §17 (25 muc). Tick = pass acceptance.

### 12.1 Dictionary completeness

- [x] Da dinh nghia du 8 feature.
- [x] Moi feature co primary source.
- [x] Moi feature co supporting/fallback source neu ton tai (ghi ro `UNAVAILABLE` khi khong co).
- [x] Moi feature co unit va range.
- [x] Moi feature co polarity.
- [x] Moi feature co missing semantics.
- [x] Da phan biet raw va normalized value.
- [x] Da phan biet explicit, inferred va behavioral (va `safety-derived observable` cho `max_risk`).

### 12.2 Numeric constraints (mirror contract §17)

- [x] Stress dung raw scale `1-5` (khop G2 seed; contract §8.1 noi `1-10` nhung da doi chieu source — xem §10 B.1).
- [x] Mood dung enum/score `1-5`.
- [x] Energy dung score `1-5`.
- [x] Sleep duration va quality tach rieng (duration co, quality chua seed — ghi ro).
- [x] Anxiety signal dung range `0-1`.
- [x] Engagement dung normalized score `0-1`.
- [x] Exercise completion dung ratio `0-1`.
- [x] Max risk dung final G3 risk scale `1-4`.

### 12.3 Policy

- [x] Co timezone policy (mirror contract §6).
- [x] Co late-data policy (mirror contract §6.7).
- [x] Co Open Expert Decisions table (mirror contract §13.1).
- [x] Co `feature_dictionary_v1` version constant (§2).
- [x] Co initial calculation versions (§2.2).
- [x] Khong co clinical threshold tu bia (§10).

### 12.4 G4-T10 known gap

- [x] G4-T10 known gap da duoc ghi nhan o `docs/05_IMPLEMENTATION_STATUS.md` (1 dong theo contract §3, chen 2026-08-04).
- [x] G6 Program Matching deferred cho den khi G4-T10 ship immutable snapshot.

### 12.5 Source mapping verification

- [x] Cursor da inspect actual G2/G3 source truoc khi map (theo contract §15).
- [x] Conflicts da duoc report (B.1, B.2, B.3) va chot (b)/(b)/(b).
- [x] Khong tu tao field ngoai task.

### 12.6 Null/Unknown/Zero discipline

- [x] Bang §4 phan biet ro 4 gia tri (`null` / `UNKNOWN` / `0` / `NOT_APPLICABLE`).
- [x] Mandatory rule "NEVER convert null to 0" duoc nhac o §4 va moi feature §6.
- [x] Test case cho MOI feature co ≥ 1 variant missing.

---

## Phu luc A: Cross-reference DB schema

| Feature | Source | File | Columns used |
|---|---|---|---|
| stress | `daily_question_answers` | V9 + V10 backfill | `assignment_id`, `user_id`, `answer_type='NUMERIC'`, `numeric_value`; join `daily_question_templates` `code='STRESS' version=1` |
| mood | `daily_question_answers` | V9 | `option_value`; join MOOD v1 template |
| energy | `daily_question_answers` | V9 + V10 | `numeric_value`; join ENERGY v1 template |
| sleep | `daily_question_answers` | V9 + V10 | `numeric_value` (hours); join SLEEP v1 template |
| anxiety_signal | `chat_analysis_results` | V16 | `topic`, `emotion`, `signals JSONB`, `model_risk_level`, `confidence`, `analysis_status='ACTIVE'`, `schema_version` |
| engagement | `behavioral_events` | V11 | `event_type`, `user_id`, `occurred_at`, `local_date`, `timezone`; filter event_type IN (CHAT_SESSION_STARTED, CHAT_MESSAGE_SENT, DAILY_CHECKIN_COMPLETED, DAILY_CHECKIN_SKIPPED) |
| exercise_completion | (G5) — | chua co source |
| max_risk | `risk_state_history` | V14 | `user_id`, `risk_level`, `occurred_at`, `schema_version='V1'` |
| users.timezone | `users` | V7 | `timezone VARCHAR(50) NOT NULL DEFAULT 'UTC'` |

## Phu luc B: Cross-reference tai lieu khac

| Doc | Muc lien quan |
|---|---|
| `docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md` | Source of truth cho moi quyet dinh trong file nay |
| `docs/02_DATABASE_MVP.md` | DB scope + schema invariants |
| `docs/04_SAFETY_AND_CBT_RULES.md` | §3 risk level definitions; §5 risk field distinction |
| `docs/schemas/chat_analysis_v1.dictionary.md` | Schema fields cho `anxiety_signal` derivation |
| `docs/schemas/chat_analysis_v1.schema.json` | JSON Schema tuong ung |
| `docs/prompts/chat_analysis_prompt_v1.md` | Enum taxonomy cho signals/emotion/intent/topic |
- `.cursor/rules/00-project-core.mdc` | Project-wide rules (clinical safety, version bump, no diagnosis) |
- `.cursor/rules/30-database-ai-safety.mdc` | AI/Safety rules |
- `docs/05_IMPLEMENTATION_STATUS.md` | Known gap G4-T10 (chen 2026-08-04) |

---

## Phu luc C: Lich su thay doi

| Ngay | Version | Thay doi | Nguoi |
|---|---|---|---|
| 2026-08-04 | `feature_dictionary_v1` | Initial release sau khi chot 3 contract conflict (b)/(b)/(b) | G4-T01 |