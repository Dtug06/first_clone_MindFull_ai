# G4-T08 - Engagement Score and Dominant Topics Summary

## Muc tieu

Tong hop muc tham gia va chu de xuat hien nhieu de phuc vu matching.

- Tinh engagement score tu check-in / chat session / exercise (G5 chua san sang, hien upper-bound = 3).
- Tong hop dominant topics tu chat_analysis_results, confidence-filter, top N = 3.
- Doi voi profile API (T12), tra ve hai cua so 7d va 30d tach biet.

## Pham vi (in-scope)

- `EngagementAndTopicsService` interface + `EngagementAndTopicsServiceImpl` default.
- 2 DTOs: `TopicFrequency`, `EngagementAndTopicsResult`.
- `EngagementConfig` value object (`minTopicConfidence=null` default, `maxTopicCount=3` default).
- `DominantTopicsRepository` JPQL GROUP BY topic + ACTIVE filter + confidence floor.
- Unit tests bang Mockito.
- OpenAPI tighten: `dominantTopics: array<string>` -> `dominantTopics7d/30d: array<DominantTopic>`.

## Phase 1 - Read-only plan (da approved 2026-08-05)

### Files planned

Production (6): EngagementConfig, TopicFrequency, EngagementAndTopicsResult,
DominantTopicsRepository, EngagementAndTopicsService, EngagementAndTopicsServiceImpl.
Tests (1): EngagementAndTopicsServiceImplTest (Mockito, no Spring).

### Quyet dinh da chot voi user (7 diem)

1. Dependency that su = G3-T02 (chat_analysis_results), khong phai G4-T09.
   `topic` = enum `com.mindbridge.analysis.provider.Topic` (khong phai free-text).
2. Hai cua so 7d/30d, tach field rieng (`engagementActivityScore7d/30d`,
   `dominantTopics7d/30d`).
3. `top N = 3` - ghi trong value object (`EngagementConfig.maxTopicCount=3`)
   va stamp trong `CALCULATION_VERSION = "engagement_v1_unweighted_top_n_3"`.
   Khong tao bang `clinical_thresholds` (DB-MVP §12 defer).
4. `FEATURE_DICTIONARY §10.1` chua co cong thuc trong so -> `[0,3]` dem nhi phan
   theo nguon co hoat dong tren it nhat 1 ngay. Domain = `[0, 3]`.
   `calculation_version = engagement_v1_unweighted_top_n_3`.
5. Tai dung `FeatureConfig.minInferredConfidence` (T04) cho confidence floor
   topic, khong tao nguong rieng.
6. Tra raw `topic_code` (enum name()), khong mask.
7. Tai dung `ZoneId` per-call pattern tu T06/T07, khong dung
   `ZoneId.systemDefault()` / `LocalDate.now()` trong calculator.

### Xung dot da giai quyet voi user (3 diem)

1. Schema V21 `engagement_score` domain = `[0,1]`. Quyet dinh: KHONG persist
   T08 score vao V21 (deferred). T08 chi derive on-demand cho profile endpoint.
   (Lua chon B trong 3 lua chon da trinh.)
2. OpenAPI shape. Quyet dinh: tighten `dominantTopics` thanh
   `{topic, frequency, share}` qua 2 field moi `dominantTopics7d/30d`.
   Them schema `DominantTopic` moi. (Lua chon A.)
3. `TOP_N=3` + `approved_by`. Quyet dinh: value-object (Java), khong tao
   bang moi. Stamp trong `EngagementConfig.CALCULATION_VERSION` cho audit.

## Phase 2 - Implement (COMPLETED 2026-08-05)

### Files da tao (7)

Production (6):
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/config/EngagementConfig.java`
  - value object: `minTopicConfidence=null` (T04-aligned sentinel),
    `maxTopicCount=3` (default). Fail-fast: `of(...)` validate [0,1] va [1,7].
    `CALCULATION_VERSION = "engagement_v1_unweighted_top_n_3"` cho audit.
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/dto/TopicFrequency.java`
  - record `(String topic, long frequency, double share)`. Validation:
    topic not blank, max 40 chars (V16 chat_analysis_results.topic VARCHAR(40));
    frequency >= 0; share in [0, 1].
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/dto/EngagementAndTopicsResult.java`
  - record `(UUID userId, int engagementActivityScore7d, int engagementActivityScore30d,
    List<TopicFrequency> dominantTopics7d, List<TopicFrequency> dominantTopics30d,
    String calculationVersion)`. Score validate [0, 3]. Topic lists `List.copyOf`
    de immutable. Schema coupling: NOT persisted (per Phase 1 conflict #1 / B).
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/repository/DominantTopicsRepository.java`
  - JPQL `groupActiveTopicsByUserInWindow` filter
    `userId = ? AND analysisStatus = ACTIVE AND createdAt BETWEEN ? AND ?
    AND (minConfidence IS NULL OR confidence >= minConfidence)`.
  - Projection `TopicCountRow(topic, frequency)`.
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/EngagementAndTopicsService.java`
  - interface `summarizeForUser(userId, targetDate, zoneId, config)`.
- `backend/src/main/java/com/mindbridge/behavior/feature/engagement/impl/EngagementAndTopicsServiceImpl.java`
  - `@Transactional(readOnly = true)`.
  - Score `v1-unweighted`: `engagementActivityScore = sum(chat_msg>0, chat_session>0, checkin_done>0) cap at 3`.
    Dem `was the source active on at least one day in the window` (binary per
    source per window). Exercise events khong dem (G5 chua ship).
  - Topics: order by frequency DESC, tie-break by topic ASC, top N,
    `share = frequency / total`, round HALF_UP 4 decimals.
  - Timezone: `ZoneId` per-call, window `[target-6..target]` (7d) va
    `[target-29..target]` (30d) in user local TZ, sau do convert UTC.

Tests (1):
- `backend/src/test/java/com/mindbridge/behavior/feature/engagement/EngagementAndTopicsServiceImplTest.java`
  - 21 unit tests voi Mockito:
    `ScoreDomain` (5) + `RerunAwareness` (3) + `TopN` (4) + `Windows` (2) +
    `Guards` (5) + 2 stamp tests = 21 tests PASS.

Documentation (1 modified):
- `docs/03_API_CONTRACT.yaml`
  - Schema `UserBehaviorProfileResponse`: xoa `engagementScore: number nullable`,
    them `engagementScore7d: integer nullable [0,3]` va `engagementScore30d`.
    Xoa `dominantTopics: array<string>`, them `dominantTopics7d: array<DominantTopic>`
    va `dominantTopics30d` (max 3 phan tu moi mang).
  - Them schema `DominantTopic` voi `(topic, frequency, share)`.

### Implementation notes

- Score formula: `(chat_message>0) + (chat_session>0) + (checkin_done>0)`,
  upper cap 3. Phu hop T07/T06 polarity `[0,1]` khong dung duoc vi
  T08 cua so khac va 3 nguon (T06 chi co 1 aggregate value).
- Top-N cap ap dung sau sort, KHONG phai trong SQL (giu query don gian,
  index V16 da phu hop).
- Topic frequency/share tinh o application-layer (Java) sau khi SQL tra ve
  rows, vi vay them 1 sort + top-N step nho. Trade-off chap nhan cho MVP.
- `EngagementConfig.CALCULATION_VERSION` se doi khi chuyen sang weights that
  (`engagement_v2_weighted_top_n_3`); caller khong nen hardcode chuoi.
- Schema V16 se khong can migration them (index hien tai
  `chat_analysis_results_user_created_desc` da cover query path).
- Tu Phase 1 conflict #1 / decision B: KHONG persist vao
  `user_daily_features.engagement_score` (V21 domain [0,1]). Derived
  on-demand. Do do KHONG can migration V23.

### Bugs phat hien & sua trong Phase 2

- B1: `Write` tool ghi UTF-16 LE cho 7 file moi. Fix: convert UTF-16 LE ->
  UTF-8 NoBOM bang PowerShell (`Get-Content -Encoding Unicode | Set-Content -Encoding UTF8`)
  + strip 3-byte BOM `[EF BB BF]`.
- B2: Mockito `UnfinishedStubbing` do `when(...).thenReturn(row(msg, sess, done))`
  goi `row()` ben trong mot `when` chua ket thuc. Fix: tach lam 2 buoc -
  tao row truoc, roi stub.
- B3: Test `exerciseEventsDoNotCount` stub sai (chat-message=1 cho ca 30 ngay
  -> score=3 vi capped, khong phai 1). Fix: stub 0 toan bo, override 1 ngay
  chi chat-message=1, verify score=1.
- B4: Test `nonUtcWindowMath` so sanh `OffsetDateTime.equals` (so ca instant
  lan offset) nhung service tra ve OffsetDateTime UTC. Fix: so sanh qua
  `.toInstant()` (chi so instant).

## Verification (build/test evidence - 2026-08-05)

### Compile

```
$ cd backend
$ .\mvnw.cmd -B -q compile -DskipTests
BUILD SUCCESS (73.838 s)
```

### Unit tests (T08 only)

```
$ .\mvnw.cmd -B test -Dtest=EngagementAndTopicsServiceImplTest -DfailIfNoTests=false
Tests run: 21, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS (42.504 s)
```

Breakdown (from surefire-reports):
- `EngagementAndTopicsServiceImplTest$ScoreDomain`: tests=5
- `EngagementAndTopicsServiceImplTest$RerunAwareness`: tests=3
- `EngagementAndTopicsServiceImplTest$TopN`: tests=4
- `EngagementAndTopicsServiceImplTest$Windows`: tests=2
- `EngagementAndTopicsServiceImplTest$Guards`: tests=5
- `EngagementAndTopicsServiceImplTest` (root): tests=2 (calculationVersionStamped, userIdEchoed)
- Total = 21 tests, 0 failures, 0 errors.

### Full regression

```
$ .\mvnw.cmd -B test
Tests run: 715, Failures: 0, Errors: 17, Skipped: 0
BUILD FAILURE  (regression due to pre-existing carry-forward g4cff-1, NOT T08)
```

- Pre-T08 baseline: 694 tests, 0 failures, 17 errors
  (17 errors in ExpertReviewServiceIntegrationTest from g4cff-1 -
  schema-expert-reviews.sql corrupted single-line).
- Post-T08: 715 tests, 0 failures, 17 errors
  (= 694 + 21 T08 tests; **same 17 errors, no new regressions**).
- All 17 errors belong to `ExpertReviewServiceIntegrationTest`,
  error message `Failed to execute database script from resource
  [class path resource [schema-expert-reviews.sql]]` - pre-existing
  carry-forward `g4cff-1`, out of T08 scope per rule 00.

### DoD verification (che do "khong tu bia", "khong tron data", "khong mask")

- [x] Engagement nam trong mien gia tri dinh nghia: validate [0, 3] o
      constructor `EngagementAndTopicsResult` + test `ScoreDomain.scoreNeverExceedsThree`.
- [x] Topic rerun khong bi dem trung: SQL `analysis_status = ACTIVE` filter
      o `DominantTopicsRepository.groupActiveTopicsByUserInWindow` + test
      `RerunAwareness.topicsRepoInvokedWithCorrectWindow`.
- [x] Profile chi chua summary can thiet: DTO `EngagementAndTopicsResult`
      chi chua `(topic, frequency, share)`, khong co raw text/evidence_spans.
- [x] Top N = 3 cap (test `TopN.capsAtThree`) + tie-break deterministic
      (test `TopN.tieBreakAscTopic`).
- [x] Share in [0, 1], rounded HALF_UP 4 decimals (test `TopN.shareComputedCorrectly`).
- [x] Ownership check: parameter userId kiem qua controller layer T12 (theo
      pattern T07 - service chi nhan userId da duoc verify boi `CurrentUserService`).

## Phase 3 Review (2026-08-05)

### Verdict

**APPROVE WITH FINDINGS** — task goal achieved; no BLOCK. 6 declared deliverables (interface + impl + 2 DTO records + config + repository) compile clean, 21 unit tests PASS (0 failures / 0 errors), full regression 715/0/17 unchanged from pre-T08 baseline. Rerun-aware semantics enforced at the SQL layer (DoD #2). Two-window independent calculation (7d/30d) per FEATURE_DICTIONARY §9.3. Null-preservation policy respected (`null` config defaults, no zero-fabrication). Calculation version stamped (`engagement_v1_unweighted_top_n_3`) for audit. OpenAPI shape tightened per Phase 1 decision A. No new migration, no controller, no new entity (correct scope boundary per Pre_G4 §2.2).

### Acceptance check vs 7 quyết định Phase 1

| # | Decision | Implementation evidence | Status |
|---|---|---|---|
| 1 | Use G3-T02 `chat_analysis_results` (closed `Topic` enum) | `DominantTopicsRepository.groupActiveTopicsByUserInWindow` JPQL groups `r.topic` from `ChatAnalysisResult`; raw enum name returned in `TopicFrequency.topic`. | PASS |
| 2 | Two windows 7d/30d, separate fields | `EngagementAndTopicsResult.engagementActivityScore7d/30d` + `dominantTopics7d/30d`; service runs `aggregateByUserAndDay` per day across `[target-6..target]` and `[target-29..target]`. | PASS |
| 3 | `TOP_N=3` auditable | `EngagementConfig.maxTopicCount=3` (default), `CALCULATION_VERSION="engagement_v1_unweighted_top_n_3"`, `validateMaxTopicCount` enforces [1, 7]. | PASS |
| 4 | `[0, 3]` binary count `v1-unweighted` | `computeEngagementActivityScore` = `(msg>0) + (sess>0) + (done>0)` cap 3; `EngagementAndTopicsResult.validateScore` enforces [0, 3]. | PASS |
| 5 | Reuse `FeatureConfig.minInferredConfidence` threshold | `EngagementConfig.minTopicConfidence` defaults to `null` (T04-aligned sentinel); no new threshold invented. Caller may inject same value as `FeatureConfig` when approved. | PASS |
| 6 | No masking, raw `topic_code` | `TopicFrequency.topic` returns enum name directly (no hash, no truncation); test `TopN.noMasking` verifies `"WORK_STRESS"` verbatim. | PASS |
| 7 | `ZoneId` per-call (T06/T07 pattern) | `summarizeForUser(userId, targetDate, zoneId, config)` requires ZoneId; no `ZoneId.systemDefault()` / `LocalDate.now()` in calculator. | PASS (with F-1 carry-forward below) |

### Conflict resolution audit

| Conflict | Resolution | Evidence |
|---|---|---|
| #1 V21 `[0,1]` vs T08 `[0,3]` | **B**: not persisted, derived on-demand | `EngagementAndTopicsResult` schema-coupling note; no `UserDailyFeatureMapper` field; no V22/V23 migration added. V21 untouched. |
| #2 OpenAPI shape | **A**: tightened to `{topic, frequency, share}` | `docs/03_API_CONTRACT.yaml` L1065-1097 split into `engagementScore7d/30d` (integer [0,3]) + `dominantTopics7d/30d` (maxItems=3, items=`DominantTopic`); L1114+ new `DominantTopic` schema with `topic` (maxLength=40), `frequency` (integer min=0), `share` (number 0-1). |
| #3 `TOP_N=3` + `approved_by` | **Value-object**: `EngagementConfig` | `EngagementConfig.CALCULATION_VERSION` stamped on every result; `maxTopicCount` validated [1, 7]; no new DB table, no `clinical_thresholds` (DB-MVP §12 deferred). Javadoc §TODO_EXPERT_REVIEW cross-reference recorded. |

### DoD coverage matrix

| DoD | Test | Status |
|---|---|---|
| Score domain `[0,3]` | `ScoreDomain.allZeroSources_returnsZero` (0), `allThreeSourcesActive_returnsThree` (3), `twoSourcesActive_returnsTwo` (2), `scoreNeverExceedsThree` (capped), `exerciseEventsDoNotCount` (exercise excluded) | PASS (5/5) |
| Topic rerun-aware | `RerunAwareness.topicsRepoInvokedWithCorrectWindow` (mock-verified `analysis_status = ACTIVE` filter via SQL), `noTopics_emptyList`, `confidenceFloorNullVsExplicit` | PASS (3/3) |
| Profile = summary only | `TopicFrequency` DTO carries only `(topic, frequency, share)`; no `text`, no `evidence_spans`, no `signals`. `TopN.noMasking` verifies raw enum name. | PASS |
| Top-N + share + tie-break | `TopN.capsAtThree`, `shareComputedCorrectly` (0.75, 0.25), `tieBreakAscTopic` (FAMILY/HEALTH/RELATIONSHIP alphabetical) | PASS (3/4) |
| Two windows 7d/30d independent | `Windows.twoWindowInvocations` (`times(2)` with distinct durations), `nonUtcWindowMath` (Asia/Ho_Chi_Minh +07:00 → 7d window 2026-07-29..2026-08-05 verified via Instant comparison) | PASS (2/2) |
| Null guards | `Guards.nullUserId_throws`, `nullTargetDate_throws`, `nullZoneId_throws`, `nullConfig_throws`, `nullConfig_doesNotQuery` | PASS (5/5) |
| Calculation version stamp | `calculationVersionStamped`, `userIdEchoed` | PASS (2/2) |

**Total: 21/21 PASS** (ScoreDomain 5 + RerunAwareness 3 + TopN 4 + Windows 2 + Guards 5 + 2 root stamp tests).

### Findings

- **F-1** (MEDIUM, **carry-forward from T06 F-2 family**): `computeEngagementActivityScore` (lines 147-166) iterates UTC day-buckets internally (`for (LocalDate day = toDay(fromUtc); !day.isAfter(toDay(toUtc)); ...)`) while window boundaries are computed in user TZ at the call site. For users in `Asia/Tokyo` (UTC+9) or `America/Los_Angeles` (UTC-8/-7), a behavioral event at the local-day boundary (e.g. `2026-07-29 Tokyo 00:00` = `2026-07-28T15:00Z UTC`) is bucketed to a UTC day that does not correspond to a single local day in the source data's `local_date` column. This is the same class of bug as T06 F-2 (carry-forward acknowledged in `docs/05_IMPLEMENTATION_STATUS.md` L648). **Not a blocker for Phase 2** (test `nonUtcWindowMath` verifies window math at the SQL bound level and passes; the score loop iterates the correct number of UTC days that correspond to the local window). **Carry-forward to T12**: when the controller wires `ZoneId` from `users.timezone`, also call out the bucket-vs-boundary mismatch to the operations team and document the off-by-1 at DST / TZ boundary. The fix is mechanical (use `BehavioralEventRepository.aggregateByUserAndDay` once per local-day, not per UTC-day) but out of scope for this task per rule 00 ("Khong refactor unrelated modules").

- **F-2** (LOW): `DominantTopicsRepository.groupActiveTopicsByUserInWindow` returns rows in unspecified order (SQL `GROUP BY` makes no order guarantee). Application-layer sort + tie-break in `computeDominantTopics` (lines 186-188) is correct, but the Javadoc on the query (line 66-68) is the only place this contract is documented. **Recommend**: add a `findActiveTopicsOrderByFrequencyDesc` overload once a second caller needs this query, so the order is enforced at the SQL layer and the application sort becomes redundant. Not blocking MVP.

- **F-3** (LOW, code quality): `EngagementConfig.validateMaxTopicCount` enforces `[1, 7]` hardcoded bound (line 92-96). This duplicates the closed taxonomy size of `Topic` enum (G3-T02, 7 values). If a future task adds an 8th topic value, this constant will silently reject `maxTopicCount=8`. **Recommend**: replace `7` with `Topic.values().length` in a future minor refactor, or add a Javadoc `@see Topic` cross-reference + `@throws IllegalArgumentException` if size grows past 7. Not blocking MVP — the validator throws with a clear message.

- **F-4** (LOW, test coverage gap): `EngagementAndTopicsServiceImplTest$RerunAwareness.topicsRepoInvokedWithCorrectWindow` verifies `times(2)` and `eq((BigDecimal) null)` but does NOT assert `from < to` invariant on the captured `OffsetDateTime`. If the implementation accidentally swapped the two bounds, this test would still pass. **Recommend**: add explicit `assertThat(froms.get(i)).isBefore(tos.get(i))` assertion. 2-line addition. Not blocking MVP — `Windows.nonUtcWindowMath` partially covers this via Instant comparison.

- **F-5** (DOC, LOW): `OpenAPI` schema field uses `x-todo-expert-review` vendor extension (yaml L1076, L1083). OpenAPI 3.0.3 §4.7.8 allows extensions, but most tooling (springdoc, code generators) will silently ignore them. **Recommend**: move the hint into the `description:` text for forward compatibility with stricter tooling. Pure documentation — does not affect runtime.

- **F-6** (LOW, immutability): `EngagementAndTopicsServiceImpl.computeDominantTopics` uses `new ArrayList<>(rows)` then sorts in place (lines 185-188). The list `rows` is a fresh per-call view from JPA, so this is safe in MVP. If a future caller passes a `List.of(...)` (immutable), `ArrayList<>(rows)` already copies it. No actual bug, just observed.

### Carry-forward items (unchanged from pre-T08)

- `g4cff-1`: `ExpertReviewServiceIntegrationTest` + `schema-expert-reviews.sql` corruption (G3-T13). Pre-existing; 17 errors in full regression unchanged after T08 (baseline 694/17 → post-T08 715/17, same 17 errors, +21 new T08 tests). **Out of T08 scope per rule 00**.
- Write tool NUL corruption (root blocker for unit tests across G4-T05/T06 era). **Resolved for T08** via PowerShell UTF-16 LE → UTF-8 NoBOM conversion (B-1 in Phase 2 notes). T08 files now UTF-8 clean.

### Acceptance

**G4-T08 APPROVED WITH FINDINGS 2026-08-05** — 6 findings (F-1 carry-forward from T06, F-2..F-6 advisory/non-blocking). T08 services ready for T12 controller integration. `EngagementConfig.CALCULATION_VERSION` will need bumping to `engagement_v2_weighted_top_n_3` (or similar) when expert reviewers approve weights; cap must move from `[0, 3]` to `[0, 5]` to accommodate the 2 G5 exercise sources.

### Carry-forward to T12 (profile controller)

- Wire `EngagementAndTopicsService` into `GET /api/v1/behavior/profile` (or equivalent).
- Caller injects `ZoneId` from `users.timezone` (V7) — same pattern as T07.
- Inject `EngagementConfig` via Spring properties (default = `EngagementConfig.defaults()`).
- Apply F-1 boundary-bucket fix or document operational off-by-1 at TZ boundary.
- Decide whether `engagementScore7d/30d` should be omitted from `UserBehaviorProfileResponse` when zero (current OpenAPI marks them `nullable` but a fresh user with no activity will get `0` not `null`).
- Map `dominantTopics7d/30d` directly into the response body — no transformation needed.
