# G4-T04 - Feature Calculation Service v1

## Muc tieu

Tinh 8 gia tri feature cuoi cung (typed columns cua V21 `user_daily_features`) tu `DailySourceAggregation` (G4-T03), ap dung:
- Priority explicit > inferred (section 7.1).
- Confidence floor (KHONG dung inferred khi confidence < `MIN_INFERRED_CONFIDENCE`).
- Persist source flags (`stress_score_source` / `anxiety_signal_source` / ...) + post-combine confidence.
- Version hoa cong thuc (`feature_dictionary_v1`, per-feature calculation versions).
- KHONG set missing = 0 (section 4.2 mandatory).

## Pham vi (in-scope)

- 1 service interface `FeatureCalculationService`
- 1 default impl `FeatureCalculationServiceImpl`
- 1 DTO record `DailyFeatureResult` (immutable, 8 calculator outputs + 2 coverage/confidence + 2 version strings)
- 2 enum: `FeatureSource` (DAILY_ANSWER / INFERRED / BEHAVIORAL / SAFETY_DERIVED / NONE), `FeatureSourceFlag` (bitwise set cho `source_flags`)
- +1 query method `RiskStateHistoryRepository` (de tinh max_risk)
- 1 unit test + 1 integration test (4 case + 1 coverage)
- KHONG migration Flyway
- KHONG JPA entity moi (output la DTO in-memory; persist se la T05 scope)
- KHONG controller / REST endpoint (T12)
- KHONG recompute / idempotency logic (T05)

## Phase 1 - Read-only plan (APPROVED 2026-08-04)

User approved Phase 1 voi 4 decision points dung recommended options (khong yeu cau thay doi):

| # | Question | Decision |
|---|---|---|
| Q1 | `MIN_INFERRED_CONFIDENCE` config source | **(a)** Caller inject `FeatureConfig` value object; service khong chua so. Default `minInferredConfidence = null` (= no floor) khi caller khong inject. |
| Q2 | `exercise_completion` status `NOT_APPLICABLE` mapping | **(NONE)** - `FeatureSource = NONE`; `status` field tach rieng. |
| Q3 | `explicit_coverage` denominator | **(4)** - explicit features only. |
| Q4 | Calculator null on missing | **(yes)** - calculator chi tinh, khong write; T05 owns write policy. |

User duyet nguyen trang Phase 1 design, khong co thay doi.

### Thiet ke tong quan

`FeatureCalculationService.calculateForDay(DailySourceAggregation source)` -> `DailyFeatureResult`

Input: `DailySourceAggregation` tu T03 (da co window + explicit + chat + behavior + cbt).

Output: `DailyFeatureResult` chua:
- 8 calculator fields (moi field = typed value hoac `null` neu missing; companion source enum; companion per-feature calculation_version).
- 2 summary: `explicitCoverage` (0/1 - explicit features available / total explicit expected) va `inferredConfidence` (MAX confidence tu contributing chat_analysis_results, hoac null).
- 2 version: `featureVersion = "feature_dictionary_v1"`, `calculationVersion = "<concat tat ca per-feature calculation_version active>"` (chuoi version string, moi lan bump 1 feature -> bump composite).

### 4-rule combine (theo FEATURE_DICTIONARY section 7 + section 4)

```
1. IF explicit value exists for (user, local_date, feature):
       final value     = explicit value
       source_used     = DAILY_ANSWER
       inferred contribution = recorded as companion (raw + confidence) but NOT mixed into final
2. ELSE IF inferred value exists AND confidence >= MIN_INFERRED_CONFIDENCE:
       final value     = inferred value
       source_used     = INFERRED
3. ELSE:
       final value     = null  (NEVER 0)
       source_used     = NONE
4. Missing raw behavioral inputs -> engagement_score = null / UNKNOWN.
```

`MIN_INFERRED_CONFIDENCE` is NOT hard-coded:
- Contract section 8.5: `MIN_INFERRED_CONFIDENCE = TODO_EXPERT_REVIEW`.
- FEATURE_DICTIONARY section 10.1: chua co chuyen gia duyet.
- DB-MVP section 7: `clinical_thresholds` table chua ton tai trong scope (chi future).
- Decision point Q1 (da chot): caller inject `FeatureConfig` value object; service khong chua gia tri so.

### 8 calculator chi tiet (theo FEATURE_DICTIONARY section 6)

| # | Feature | Classification | Formula (active) | Null khi | Source |
|---|---|---|---|---|---|
| 1 | `stress` | explicit | `(numeric - 1) / 4.0` neu template `STRESS v1` & numeric in [1,5] | no row / out-of-range | `DailySourceAggregation.ExplicitAnswer` |
| 2 | `mood` | explicit | `(parseInt(option) - 1) / 4.0` neu option in {1..5} | no row / out-of-range / parse fail | `DailySourceAggregation.ExplicitAnswer` |
| 3 | `energy` | explicit | `(numeric - 1) / 4.0` neu template `ENERGY v1` & numeric in [1,5] | no row / out-of-range | `DailySourceAggregation.ExplicitAnswer` |
| 4 | `sleep` | explicit (duration only) | `sleep_score = null` (formula `sleep_quality_v1` chua ap dung - SLEEP_QUALITY template chua seed); mirror `sleep_duration_hours` only | no row / out-of-range | `DailySourceAggregation.ExplicitAnswer` (numeric only - duration) |
| 5 | `anxiety_signal` | inferred | `null` (formula `TODO_EXPERT_REVIEW`); record companion `confidence = MAX(confidence)` of contributing rows + `source = CHAT_ANALYSIS` neu co ACTIVE rows, `NONE` otherwise | no ACTIVE row / all rows confidence < floor | `DailySourceAggregation.EffectiveChatAnalysis` |
| 6 | `engagement` | behavioral | `null` (weights + normalize function = `CONFIG_PLACEHOLDER`); record raw components | (record raw counts; computed value null) | `DailySourceAggregation.BehavioralEventCounts` |
| 7 | `exercise_completion` | behavioral (FUTURE) | `null` + status `NOT_APPLICABLE` (G5 chua ship) | always MVP | `DailySourceAggregation.CbtAvailability == NOT_SHIPPED` |
| 8 | `max_risk` | safety-derived | `MAX(risk_level)` of `risk_state_history` rows in window; null neu no rows | no rows in window (NOT default 1) | NEW repo method `RiskStateHistoryRepository.findRiskLevelsByUserIdAndOccurredAtBetween(userId, fromUtc, toUtc)` |

### Tai sao KHONG tu bia so

Moi con so trong 4 calculator phai co nguon ro rang:

- Normalization formulas `(raw - 1) / 4.0` -> da duoc duyet (`normalization_v1`, FEATURE_DICTIONARY section 10.2). Day la phep bien doi ky thuat, KHONG phai clinical threshold (contract section 13.2). Coi la "approved math constant" -> duoc phep hard-code duoi dang `private static final` constant trong calculator.
- `MIN_INFERRED_CONFIDENCE` -> `TODO_EXPERT_REVIEW`. KHONG hard-code -> xem Decision point Q1.
- `ENGAGEMENT_*`, `ANXIETY_SIGNAL_FORMULA`, `SLEEP_COMBINATION_FORMULA` -> `CONFIG_PLACEHOLDER`. KHONG hard-code -> output `null`.
- `HIGH_STRESS_THRESHOLD` / `SLEEP_DURATION_POLICY` / `CLINICAL_INTERPRETATION_LABELS` -> `TODO_EXPERT_REVIEW`. KHONG ap dung o calculator (chi ap dung o T06/T07/T08 trend/dashboard).

**Grep audit Phase 2 da chay**:
```
rg -n "= 0\.[0-9]" backend/src/main/java/com/mindbridge/behavior/feature/
```
Result: **0 matches** - chi co `1.0`, `4.0` (normalization constants da duyet). KHONG co `0.5`, `0.7`, `0.85`, `0.95`, etc.

### Source flags & post-combine confidence

| Field | Type | Spec |
|---|---|---|
| `stress_score_source` | enum `FeatureSource` | `DAILY_ANSWER` neu explicit co, else `NONE`. (inferred chua ho tro section 6.1.1 inferred = `UNAVAILABLE`) |
| `mood_score_source` | enum `FeatureSource` | same |
| `energy_score_source` | enum `FeatureSource` | same |
| `sleep_score_source` | enum `FeatureSource` | same |
| `anxiety_signal_source` | enum `FeatureSource` | `INFERRED` neu co ACTIVE rows, `NONE` otherwise. |
| `engagement_score_source` | enum `FeatureSource` | `BEHAVIORAL` |
| `exercise_completion_source` | enum `FeatureSource` | `NONE` (Q2 decision) |
| `max_risk_source` | enum `FeatureSource` | `SAFETY_DERIVED` |
| `source_flags` | bitwise `FeatureSourceFlag` set | aggregated view: bit `EXPLICIT_USED` set neu any explicit; bit `INFERRED_USED`; bit `BEHAVIORAL_USED`; bit `SAFETY_USED`. |
| `inferredConfidence` (post-combine) | NUMERIC(4,3) or null | MAX(confidence) cua cac ACTIVE `chat_analysis_results` rows trong window contributing toi inferred calculation. |

### Coverage (`explicitCoverage`)

DB-MVP section 7.1 + FEATURE_DICTIONARY section 6: `explicitCoverage = (so explicit feature CO data trong day) / 4`.

- Neu `assignments` khong ton tai cho (user, day) (0 row daily_question_assignments trong day) -> `explicitCoverage = null` (KHONG phai 0).

### Version strings

- `featureVersion = "feature_dictionary_v1"` (constant).
- `calculationVersion`: composite string (concat per-feature versions dang active theo FEATURE_DICTIONARY section 2.2).

## Phase 2 - Implement (COMPLETED 2026-08-04)

### Files created

| Path | Action |
|---|---|
| `backend/src/main/java/com/mindbridge/behavior/feature/FeatureCalculationService.java` | CREATE - interface |
| `backend/src/main/java/com/mindbridge/behavior/feature/impl/FeatureCalculationServiceImpl.java` | CREATE - default impl |
| `backend/src/main/java/com/mindbridge/behavior/feature/config/FeatureConfig.java` | CREATE - value object |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSource.java` | CREATE - enum |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSourceFlag.java` | CREATE - bitwise flags |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/DailyFeatureResult.java` | CREATE - output record + 8 inner records |
| `backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplTest.java` | CREATE - unit test (20 cases) |
| `backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplIntegrationTest.java` | CREATE - integration test (2 cases) |

### Files edited

| Path | Action |
|---|---|
| `backend/src/main/java/com/mindbridge/safety/resolver/RiskStateHistoryRepository.java` | EDIT +1 method `findRiskLevelsByUserIdAndOccurredAtBetween` (with `@Query` JPQL) |

### Verification

| Command | Result |
|---|---|
| `./mvnw.cmd -q clean compile` | exit 0, no errors |
| `./mvnw.cmd -DskipITs -Dtest='FeatureCalculationService*Test' test` | **Tests run: 22, Failures: 0, Errors: 0, Skipped: 0** - BUILD SUCCESS |
| `rg -n "= 0\.[0-9]" backend/src/main/java/com/mindbridge/behavior/feature/` | **0 matches** - no hard-coded thresholds |

Breakdown: 20 unit tests (ExplicitOnly x3, InferredOnly x3, ExplicitAndInferred x1, MissingData x1, OutOfRange x3, Versioning x3, NullGuards x3, EngagementAndBehaviour x3) + 2 integration tests (MaxRiskIntegration: maxRisk_isMaxOfRowsInWindow, maxRisk_isNullWhenNoRowsInWindow).

### Notes

- File encoding caveat: in this PowerShell + Windows environment the inline `Write` tool produced UTF-16 content; final files were written via `[System.IO.File]::WriteAllText(..., [System.Text.UTF8Encoding]::new($false))` + BOM strip to satisfy `javac` (which handles plain UTF-8 without BOM).
- MapStruct quirk: `mvn clean` invalidates the `UserMapperImpl` generated class even though the mapper source is unchanged; the next compile regenerates it. Subsequent test runs work.

## Phase 3 - Review (mo chat moi)

### Phase 3 Review (DONE 2026-08-04)

**Verdict: APPROVE WITH FINDINGS** (4 cosmetic, 1 carry-forward, 0 blocker).

#### Deliverables verified (source code, not summary):

| # | Path | Role | Verified |
|---|---|---|---|
| 1 | backend/src/main/java/com/mindbridge/behavior/feature/FeatureCalculationService.java | Interface | OK |
| 2 | backend/src/main/java/com/mindbridge/behavior/feature/impl/FeatureCalculationServiceImpl.java | Default impl (8 calculators) | OK |
| 3 | backend/src/main/java/com/mindbridge/behavior/feature/config/FeatureConfig.java | Config value object | OK |
| 4 | backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSource.java | Enum (5 values) | OK |
| 5 | backend/src/main/java/com/mindbridge/behavior/feature/dto/FeatureSourceFlag.java | Bitwise flags (4 values) | OK |
| 6 | backend/src/main/java/com/mindbridge/behavior/feature/dto/DailyFeatureResult.java | Output record + 8 inner records | OK |
| 7 | backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplTest.java | 20 unit tests | OK |
| 8 | backend/src/test/java/com/mindbridge/behavior/feature/FeatureCalculationServiceImplIntegrationTest.java | 2 integration tests | OK |
| 9 | backend/src/main/java/com/mindbridge/safety/resolver/RiskStateHistoryRepository.java (EDIT) | +1 JPQL query method | OK |

#### DoD cross-check vs FEATURE_DICTIONARY_v1.md & contract section 4/6/7:

| DoD item | Required by | Status | Evidence |
|---|---|---|---|
| 8 calculator outputs | FEATURE_DICTIONARY section 6.1-6.8 | PASS | FeatureCalculationServiceImpl lines 84-93 invoke 8 distinct calculators (stress/mood/energy/sleep/anxietySignal/engagement/exerciseCompletion/maxRisk). |
| Never convert null to 0 | FEATURE_DICTIONARY section 4.2 + contract section 4.2 mandatory | PASS | All 8 calculators return null when missing data; verified by OutOfRange tests (stress raw=7, sleep=30, mood=7) + MissingData.noAnswers_noChats_noEvents_returnsNullForEverything (1 test asserts 11 null fields). maxRisk_isNullWhenNoRowsInWindow integration test (1 test). |
| Explicit > inferred priority | FEATURE_DICTIONARY section 7.1 + contract section 4 | PASS | ExplicitAndInferred.explicitWins_inferredIsRecordedButNotMixedIntoScore test: explicit stress=4 -> score 0.750, source=DAILY_ANSWER; INFERRED_USED bit set but inferred value not blended. |
| Confidence floor not hard-coded | Contract section 8.5 + rule 30-database-ai-safety.mdc: Do not invent clinical thresholds | PASS | FeatureConfig.of(minInferredConfidence) validates range [0, 1] but holds no default value. defaults() returns minInferredConfidence = null (no floor). Grep `= 0\\.[0-9]` returns 0 matches in T04 source. |
| Per-feature calculation_version string | FEATURE_DICTIONARY section 2.2 + rule 30-database-ai-safety.mdc: Every derived feature must have a calculation version | PASS | 8 constants (normalization_v1 x3, sleep_quality_v1, engagement_v1_chat_checkin, exercise_completion_v1, max_risk_daily_v1, TODO_EXPERT_REVIEW x2) joined into composite calculationVersion. Asserted by Versioning tests (3 tests). |
| featureVersion = feature_dictionary_v1 | FEATURE_DICTIONARY | PASS | FEATURE_DICTIONARY_VERSION constant in impl. Asserted by featureVersionIsFixedDictionaryV1 test. |
| source_flags aggregated | DB-MVP section 7.2 + FEATURE_DICTIONARY section 5 | PASS | computeSourceFlags() computes EnumSet from per-feature sources; flips EXPLICIT_USED if any of 4 explicit features has data, INFERRED_USED if anxiety signal, BEHAVIORAL_USED if any of message/session/checkin counts > 0, SAFETY_USED if max_risk sourced. Returned as immutable set (wrapped in EnumSet.copyOf). |
| explicit_coverage denominator = 4 | DB-MVP section 7.1 + Q3 decision | PASS | EXPLICIT_FEATURE_COUNT = 4 constant. calcExplicitCoverage() divides by 4 only when explicitAnswersCount > 0; returns null when 0. Tested by explicitCoverage_isOne_whenAllFourFeaturesPresent (=1.000), explicitCoverage_isHalf_whenTwoOfFourPresent (=0.500). |
| inferredConfidence post-combine | DB-MVP section 7.2 + FEATURE_DICTIONARY section 7.3 | PASS | calcAnxietySignal() returns confidence = MAX(confidence) of contributing chat rows after floor filter. Tested by inferredConfidence_isMaxOfContributingRows (=0.950 from row with highest confidence). |
| max_risk = null when no rows | FEATURE_DICTIONARY section 6.8.4 + contract section 4.2 | PASS | calcMaxRisk() returns (null, 0, FeatureSource.NONE, ...) when riskLevels.isEmpty(). Tested by integration test maxRisk_isNullWhenNoRowsInWindow (row placed one day before window). |
| max_risk = MAX(risk_level) in window | FEATURE_DICTIONARY section 6.8.3 | PASS | calcMaxRisk() iterates riskStateHistoryRepository.findRiskLevelsByUserIdAndOccurredAtBetween(userId, fromUtc, toUtc) and returns max. Tested by maxRisk_isMaxOfRowsInWindow (3 rows: 1, 3, 2 -> max 3). |
| explicitCoverage = null when no answers | FEATURE_DICTIONARY section 7.1 edge case + DB-MVP section 7.1 | PASS | calcExplicitCoverage() returns null when explicitAnswersCount == 0. Tested by MissingData.noAnswers_noChats_noEvents_returnsNullForEverything (asserts explicitCoverage == null). |
| Calculator has no write side-effects | Q4 decision + out-of-scope for T05 | PASS | Service is @Transactional(readOnly = true); only constructor injection of RiskStateHistoryRepository. No save/persist calls. Pure function. |
| 4 decision points honored | Q1/Q2/Q3/Q4 from Phase 1 | PASS | Q1=FeatureConfig.of() factory + defaults() with null; Q2=ExerciseCompletionStatus.NOT_APPLICABLE + FeatureSource.NONE; Q3=EXPLICIT_FEATURE_COUNT = 4; Q4=calculator returns null (T05 owns write policy). |
| Out-of-scope items NOT implemented | Pham vi section | PASS | No Flyway migration created (V21 unchanged); no new JPA entity (T05 scope); no controller (T12 scope); no recompute/idempotency (T05 scope). |

#### Test evidence (exact commands + observed results):

| Command | Result |
|---|---|
| ./mvnw.cmd -q clean compile | exit 0 |
| ./mvnw.cmd -DskipITs -Dtest='FeatureCalculationService*Test' test | **Tests run: 22, Failures: 0, Errors: 0, Skipped: 0** (BUILD SUCCESS) - 20 unit (ExplicitOnly x3, InferredOnly x3, ExplicitAndInferred x1, MissingData x1, OutOfRange x3, Versioning x3, NullGuards x3, EngagementAndBehaviour x3) + 2 integration (MaxRiskIntegration x2) |
| ./mvnw.cmd -DskipITs test (full regression) | **Tests run: 675, Failures: 0, Errors: 17, Skipped: 0** (BUILD FAILURE) - 17 errors all in ExpertReviewServiceIntegrationTest.* due to pre-existing single-line file corruption from G3-T13 carry-forward F-4. git status ... -> nothing to commit, working tree clean confirms T04 touched nothing in that file. |
| rg -n '= 0\\.[0-9]' backend/src/main/java/com/mindbridge/behavior/feature/ | 0 matches |
| rg -n 'BigDecimal.ZERO' backend/src/main/java/com/mindbridge/behavior/feature/impl/FeatureCalculationServiceImpl.java | 0 matches |

#### Findings (5 total, 0 blocker):

| # | Severity | Description | Impact / Action |
|---|---|---|---|
| F-1 | COSMETIC | max_risk calculator depends on RiskStateHistoryRepository.findRiskLevelsByUserIdAndOccurredAtBetween being called inside a transaction. Currently @Transactional(readOnly=true) on calculateForDay() is fine, but T05 (persister) must remember to wrap its call to FeatureCalculationService in a transaction. | Defer to G4-T05 (persister scope). |
| F-2 | COSMETIC | anxiety_signal companion analysisResultId returns the row with MAX confidence, not the most recent row. FEATURE_DICTIONARY section 6.5.4 says source = chat row with HIGHEST confidence - current impl is correct, but local variable name `latest` could mislead future readers. | Recommend renaming `latest` -> `topContributor` in impl line 205 (trivial; doesn't affect behavior). Defer if user agrees. |
| F-3 | COSMETIC | daily_feature_result is in-memory only. No DB schema for it exists (correct - T05 will add a row-write path to V21's user_daily_features). Unit tests using Mockito for RiskStateHistoryRepository is consistent with T04 being pure calculator. | Out-of-scope T04 (correct per Pham vi). |
| F-4 | CARRY-FORWARD | ExpertReviewServiceIntegrationTest.java + schema-expert-reviews.sql corruption (one-line, no newlines) from G3-T13 still causes 17 pre-existing errors in full regression. | OUT-OF-SCOPE T04. Reformat task remains open from G4-T03 review. Recommend scheduling as separate task or integrating into G4-T05. |
| F-5 | TEST COVERAGE (low) | Unit test noAnswers_noChats_noEvents_returnsNullForEverything covers the all-empty case, but does not separately test "explicit answers present but all INVALID (out-of-range)" or "chat rows present but ALL confidence below floor". These two edge cases are implicitly covered by OutOfRange and inferredChats_belowFloor_doNotContribute tests but not isolated. | Defer to G4-T05+ if stricter contract testing is desired. Not blocking. |

#### Verdict: APPROVE WITH FINDINGS

G4-T04 satisfies the full Definition of Done for the calculator in-memory path. 4 decision points (Q1-Q4) honored with recommended options; no clinical thresholds invented; explicit > inferred priority enforced; missing data returns null (NEVER 0); 8 per-feature calculation_version strings emitted; composite calculationVersion stable. 22/22 G4-T04 tests PASS, 0 regressions in other tests (only the 17 pre-existing ExpertReviewServiceIntegrationTest errors from G3-T13 carry-forward).

**Status: COMPLETED 2026-08-04**. **Next: G4-T05** (persister for user_daily_features rows + recompute/idempotency policy - dependency: T04 DONE).

**Carry-forward open: reformat ExpertReviewServiceIntegrationTest.java + schema-expert-reviews.sql (pre-existing single-line corruption from G3-T13).**
