# G4-T03 — Daily Source Aggregation Service

## Mục tiêu

Tập hợp Daily Answer, Chat Analysis (rerun-aware), Behavioral Event và CBT activity (G5) theo cùng local_date thành một DTO read-only, cung cấp raw inputs cho G4-T04 (calculator) + G4-T05 (persister).

## Phạm vi (in-scope)

- 1 service interface + 1 default impl
- 1 DTO record (immutable, 4 nested records)
- 1 enum `CbtAvailability`
- +1 query method trên `BehavioralEventRepository`
- +1 query method trên `DailyQuestionAnswerRepository`
- 2 file test (unit + integration)
- **Không** migration Flyway.
- **Không** entity JPA mới.
- **Không** controller / REST endpoint (G4-T12).

## Phase 1 — Read-only plan (đã duyệt 2026-08-04)

Xem conversation summary trước.

Quyết định conflict đã chốt (Q1/Q2/Q3):

| # | Câu hỏi | Quyết định |
|---|---|---|
| Q1 | Late-arriving answer: filter theo `answered_at` (UTC) hay `assignment.assignedForDate`? | **A (Recommended)**: filter theo `assignment.assignedForDate` (semantic correct per FD §8.6) |
| Q2 | CBT availability: hard-code NOT_SHIPPED hay runtime tableExists? | **A (Recommended)**: runtime check tại `@PostConstruct` (future-proof khi G5 ship) |
| Q3 | TZ change: caller TZ hay row stored TZ? | **A (Recommended)**: caller TZ (callers quyết định ngày local_date hiện tại) |

## Phase 2 — Implement (in progress)

### Files sẽ tạo / sửa

| Path | Action | Purpose |
|---|---|---|
| `backend/src/main/java/com/mindbridge/behavior/feature/DailySourceAggregationService.java` | CREATE | Interface (`aggregateForDay(userId, timezone, localDate)`) |
| `backend/src/main/java/com/mindbridge/behavior/feature/impl/DailySourceAggregationServiceImpl.java` | CREATE | Default impl |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/DailySourceAggregation.java` | CREATE | Outer record + `ExplicitAnswer`, `EffectiveChatAnalysis`, `BehavioralEventCounts`, `CbtAggregation` nested records |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/CbtAvailability.java` | CREATE | Enum: NOT_SHIPPED, NOT_APPLICABLE, COMPUTABLE |
| `backend/src/main/java/com/mindbridge/behavior/feature/dto/BehavioralEventCountsRow.java` | CREATE | JPA projection for repo aggregate query |
| `backend/src/main/java/com/mindbridge/behavior/repository/BehavioralEventRepository.java` | EDIT +1 method | `aggregateByUserAndDay(userId, fromUtc, toUtc)` |
| `backend/src/main/java/com/mindbridge/dailyquestion/repository/DailyQuestionAnswerRepository.java` | EDIT +1 method | `findWithAssignmentByUserIdAndAssignedForDate(userId, assignedForDate)` (JPQL join, eagerly fetches assignment) |
| `backend/src/test/java/com/mindbridge/behavior/feature/DailySourceAggregationServiceImplTest.java` | CREATE | Unit: TZ math, DTO mapping, cbtAvailability transitions, IllegalArgumentException guards |
| `backend/src/test/java/com/mindbridge/behavior/feature/DailySourceAggregationServiceImplIntegrationTest.java` | CREATE | Integration: 4 DoD scenarios |

### Quyết định thiết kế quan trọng

1. **Service transaction**: `@Transactional(readOnly = true)` trên `aggregateForDay`. Stateless; recompute safe.
2. **Null guards**: throw `IllegalArgumentException` cho null `userId` / `timezone` / `localDate`. Wrap `DateTimeException` (từ `ZoneId.of`) thành `IllegalArgumentException`.
3. **Window math**:
   - `windowStartUtc = localDate.atStartOfDay(ZoneId.of(timezone)).toOffsetDateTime()`
   - `windowEndUtc   = localDate.plusDays(1).atStartOfDay(ZoneId.of(timezone)).toOffsetDateTime()`
4. **Rerun-aware**: chỉ count `analysis_status = ACTIVE` rows từ `chat_analysis_results` (filter in service từ result đã query).
5. **Late-arriving (Q1=A)**: filter `daily_question_answers` bằng `assignment.assignedForDate = localDate`, KHÔNG filter `answered_at`.
6. **CBT runtime detection (Q2=A)**: tại `@PostConstruct`, chạy query `INFORMATION_SCHEMA.TABLES` qua `DataSource`:
   - If `EXERCISE_ASSIGNMENTS` exists → `CBT_SHIPPED = true`.
   - Else → `CBT_SHIPPED = false` (MVP current state).
   - Khi G5 ship, query trả về true → service tự động COMPUTABLE path, không cần đổi code.
7. **TZ change (Q3=A)**: cửa sổ UTC tính từ caller `timezone`. Row stored `timezone` KHÔNG ảnh hưởng day-bucket.
8. **Logging**: chỉ log counts, userId, localDate. KHÔNG log nội dung chat / answer text / option labels.

### Failure modes T03 KHÔNG handle (out-of-scope)

- Lỗi G3 (chat_analysis_results FAILED mà không có ACTIVE row) → trả về rỗng cho `effectiveChatAnalyses` (bình thường).
- User không có data → trả về DTO với list rỗng / count = 0 / null.
- Missing `users` row → caller phải đảm bảo user tồn tại; T03 KHÔNG re-check FK ownership (đã ở caller responsibility; T03 chỉ đọc theo userId literal).

## Phase 3 — Review (DONE 2026-08-04)

Verdict: **APPROVE WITH FINDINGS**.

- Đối chiếu 22/22 DoD check points (spec §Phạm vi + §Quyết định thiết kế quan trọng) → PASS.
- 20/20 tests PASS (16 unit + 4 integration), zero regressions.
- 4 findings (F-1..F-4): F-1..F-3 cosmetic/advisory; F-4 pre-existing carry-forward (ExpertReviewServiceIntegrationTest one-line corruption), OUT-OF-SCOPE T03.
- Next: G4-T04 (combine explicit/inferred + 8 calculator). Carry-forward to next available task: reformat `ExpertReviewServiceIntegrationTest.java` + `schema-expert-reviews.sql`.

Full evidence xem `docs/05_IMPLEMENTATION_STATUS.md` → "G4-T03 — Daily Source Aggregation Service (Phase 3 review PASS 2026-08-04)".