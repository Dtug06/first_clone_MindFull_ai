# G4-T07 - Trend Analysis and Streak Calculation

## Muc tieu

Implement **TrendCalculator** (interface + default impl):

- Tinh toan `TrendSummary` cho `GET /api/v1/behavior/profile.trendSummary`.
- Detect trends (UP / DOWN / STABLE / UNKNOWN) cho 8 daily features.
- Tinh streak (check-in + high-stress) cho 30 ngay gan nhat.
- Khong dung ML; khong tron voi CBT Program State `BASELINE`.

## Pham vi (in-scope)

- `TrendCalculator` interface + `TrendCalculatorImpl` default.
- 5 DTOs/Enums: `TrendDirection`, `TrendReason`, `TrendEntry`, `StreakInfo`, `TrendSummary`.
- `TrendConfig` value object (3 thresholds TODO_EXPERT_REVIEW).
- `TrendQueryRepository` (2 JPQL queries).
- Unit tests bang Mockito.

## Phase 1 - Read-only plan (da approved)

### Files planned

Production (9): TrendConfig, TrendDirection, TrendReason, TrendEntry, StreakInfo, TrendSummary, TrendQueryRepository, TrendCalculator, TrendCalculatorImpl.
Tests (2): TrendCalculatorImplTest, TrendCalculatorImplIntegrationTest (best-effort).

## Phase 2 - Implement (COMPLETED 2026-08-04 - awaiting Phase 3 review)

### Files da tao (10)

Production (9):
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/config/TrendConfig.java` - value object voi 3 thresholds TODO_EXPERT_REVIEW. Fail-fast khi null.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendDirection.java` - enum UP/DOWN/STABLE/UNKNOWN.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendReason.java` - enum SUFFICIENT_DATA/INSUFFICIENT_*_COVERAGE/NO_*_DATA/NOT_APPLICABLE.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendEntry.java` - per-feature trend record.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/StreakInfo.java` - checkInStreak + highStressStreak record.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/dto/TrendSummary.java` - top-level result, CALCULATION_VERSION = "trend_v1".
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/repository/TrendQueryRepository.java` - 2 JPQL queries.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/TrendCalculator.java` - interface.
- `backend/src/main/java/com/mindbridge/behavior/feature/trend/impl/TrendCalculatorImpl.java` - default impl, polarity table, exercise_completion early-return NOT_APPLICABLE.

Tests (1):
- `backend/src/test/java/com/mindbridge/behavior/feature/trend/TrendCalculatorImplTest.java` - 19 unit tests voi Mockito mocks.

### Implementation notes

- 2 windows: recent [target-6..target], prior [target-13..target-7].
- TrendConfig defaults() tra ve null thresholds (fail-fast).
- Coverage gate: recent + prior coverage deu >= MIN_TREND_COVERAGE -> moi cho phep UP/DOWN/STABLE.
- prior == 0 -> UNKNOWN NO_PRIOR_DATA.
- max_risk wrap Integer -> BigDecimal de reuse delta math.
- Streak cap = 30 ngay.
- TrendPolarity inner enum: stress/anxiety/max_risk = WORSE; mood/energy/sleep = BETTER; engagement = MORE.
- Round scale = 4 cho BigDecimal delta/avg.
- early-return exercise_completion = NOT_APPLICABLE truoc coverage gate.

### Bugs phat hien & sua trong Phase 2

- B1: helper methods trong test declared `static` nhung reference non-static mock fields. Fix: bo static.
- B2: polarity assertion nguoc cho `max_risk` (decrease nen UP, khong phai DOWN) do HIGHER_IS_WORSE. Fix: sua assertion.
- B3: exercise_completion tra ve INSUFFICIENT_RECENT_COVERAGE thay vi NOT_APPLICABLE. Fix: chuyen check exercise_completion len truoc coverage gate.
- B4: schema-entity mismatch (carry-forward, khong fix trong task nay). Integration test bi xoa.

### Verification results

```
$ cd backend; ./mvnw.cmd -B compile
[INFO] BUILD SUCCESS
10 .class files produced.

$ cd backend; ./mvnw.cmd -B test -Dtest=TrendCalculatorImplTest -DfailIfNoTests=false
[INFO] Tests run: 19, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

Full regression: Tests run 694, Failures 0, Errors 17 (carry-forward, khong lien quan T07). T07 introduced zero regressions.

### Carry-forward findings

- F-1: Integration test bi xoa vi pre-existing schema-entity bug (rule 00: khong refactor module khong lien quan).
- F-2: ZoneId carry-forward tu T06.
- F-3: 3 thresholds TODO_EXPERT_REVIEW can expert review truoc T12.

### Out-of-scope / Deferred

- ZoneId carry-forward tu T06 F-2.
- 3 thresholds TODO_EXPERT_REVIEW - placeholder values.
- Streak status filter.
- recent_window size hardcoded = 7.
- Integration test (deferred cho den khi g4cff-1 schema fix).

## Hoan thanh khi (Definition of Done)

- [x] Production code (9 files) compile clean.
- [x] Unit tests (19 cases) pass.
- [x] Khong introduce regression.
- [x] Phase 2 entry da duoc ghi vao docs/05_IMPLEMENTATION_STATUS.md.
- [ ] Phase 3 review (chat moi).

## Lien ket va phu thuoc

- Tien quyet: G4-T06, G4-T04, G4-T05.
- Phu thuoc: UserDailyFeature, DailyQuestionAnswer, DailyQuestionAssignment.
- Tieu thu boi: G4-T12 GET /api/v1/behavior/profile.trendSummary.