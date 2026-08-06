# G3-T13 — Expert Review và giao diện Safety cơ bản

| Field | Value |
|---|---|
| Group | G3 — Tích hợp LLM và Safety |
| Priority | SHOULD |
| Tags | Full-stack/Safety |
| Status | **Phase 3 APPROVE WITH FINDINGS** (2026-08-03, re-verified 2026-08-03 22:46 local) |
| Owner | Cursor Agent |
| Source | `docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx` (v1.0, 29/07/2026) |

## 1. Mục tiêu

Cho phép reviewer xem và xử lý event mà không cần truy cập DB trực tiếp.

## 2. Công việc chi tiết

- Tạo expert_reviews với reviewer, decision, note và timestamp.
- Tạo API danh sách/chi tiết event cho EXPERT/ADMIN.
- Tạo UI cơ bản lọc theo trạng thái và risk level.
- Không expose raw content ngoài phạm vi quyền.
- Ghi audit khi reviewer mở hoặc quyết định event.

## 3. Đầu ra cần bàn giao

Expert review API/UI cơ bản.

## 4. Hoàn thành khi (Definition of Done)

- [x] USER không truy cập được trang review (via @PreAuthorize("hasAnyRole('EXPERT', 'ADMIN')"))
- [x] Reviewer cập nhật được decision hợp lệ (POST /expert-reviews/events/{eventId}/review)
- [x] Mọi thao tác review có audit (EXPERT_REVIEW_OPENED, EXPERT_REVIEW_DECIDED)

## 5. Liên kết và phụ thuộc

- Tài liệu gốc: [`docs/MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx`](../../MindBridge_AI_Danh_muc_Task_G1_G8_Cho_2_Dev.docx)
- API contract: [`docs/03_API_CONTRACT.yaml`](../../03_API_CONTRACT.yaml)
- Database: [`docs/02_DATABASE_MVP.md`](../../02_DATABASE_MVP.md)
- Architecture: [`docs/01_ARCHITECTURE.md`](../../01_ARCHITECTURE.md)
- Safety và CBT rules: [`docs/04_SAFETY_AND_CBT_RULES.md`](../../04_SAFETY_AND_CBT_RULES.md)
- Trạng thái triển khai: [`docs/05_IMPLEMENTATION_STATUS.md`](../../05_IMPLEMENTATION_STATUS.md)
- Quy tắc dự án: [`.cursor/rules/00-project-core.mdc`](../../../.cursor/rules/00-project-core.mdc)

## 6. Mẫu giao cho Cursor (3 phase)

> **Task ID: G3-T13** — Read the relevant project rules and documents first.
> **Phase 1 - Read-only plan**: inspect existing code, list files to change, migration/API/test plan. Do not edit files. Wait for approval.
> **Phase 2 - Implement**: only the approved task. Do not refactor unrelated modules. Run actual build/tests and report commands plus results.
> **Phase 3 - Review**: open a new review chat to check acceptance criteria, security, database integrity and frontend compatibility.

---

## 7. Phase 2 Implementation Summary (2026-08-03)

### Files Created/Fixed

| File | Status |
|------|--------|
| `safety/review/ExpertReviewDecision.java` | Created |
| `safety/review/ExpertReview.java` | Created |
| `safety/review/ExpertReviewRepository.java` | Created |
| `safety/review/ExpertReviewService.java` | Created |
| `safety/review/ExpertReviewController.java` | Created |
| `safety/review/dto/SubmitReviewRequest.java` | Created |
| `safety/review/dto/ExpertReviewResponse.java` | Created |
| `safety/review/dto/SafetyEventSummaryResponse.java` | Created |
| `safety/review/dto/SafetyEventDetailResponse.java` | Created |
| `safety/event/domain/RiskStateRow.java` | Created (interface) |
| `safety/event/domain/SafetyEvent.java` | Fixed (encoding) |
| `safety/event/service/SafetyEventService.java` | Fixed (encoding) |
| `resolver/RiskStateHistory.java` | Updated (implements RiskStateRow) |
| `db/migration/V20__create_expert_reviews.sql` | Verified (already existed) |
| `test/resources/schema-expert-reviews.sql` | Created |

### API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/expert-reviews/events` | List safety events (EXPERT/ADMIN only) |
| GET | `/expert-reviews/events/{eventId}` | Get event detail |
| POST | `/expert-reviews/events/{eventId}/review` | Submit review decision |

### Build Status

- `mvn compile`: **SUCCESS**
- `mvn test-compile`: **SUCCESS**

### Phase 3 Full Review (re-verified 2026-08-03 22:46 local)

User-requested re-review to attempt promotion to **Phase 3 PASS**. Full test command:

```bash
cd backend
.\mvnw.cmd -B test "-Dtest=ExpertReviewServiceTest,ExpertReviewTest,ExpertReviewServiceIntegrationTest"
```

Observed result:

- `ExpertReviewServiceTest` 15/15 PASS (`Submit` 8/8 + `Detail` 2/2 + `ListEvents` 5/5)
- `ExpertReviewTest` 6/6 PASS
- `ExpertReviewServiceIntegrationTest` 0/17 PASS — 17 errors at `@SpringBootTest` context-load phase

Root cause (confirmed via stack trace): `Not a managed type: class com.mindbridge.safety.event.domain.SafetyEventSource`. `SafetyEventSource.java` is a plain Java POJO (no `@Entity @Table(...)` annotation), but `SafetyEventSourceRepository extends JpaRepository<SafetyEventSource, UUID>`. JPA cannot materialise the proxy → every `@SpringBootTest` in the project fails at component-scan time during `safetyEventService` → `safetyEventSourceRepository` dependency injection. This is **the pre-existing G2 baseline regression L-env-1** recorded earlier — it blocks ALL 216 `@SpringBootTest` in the project, not just T13.

**Decision: verdict stays APPROVE WITH FINDINGS (cannot promote to PASS).** Rationale:

1. The T13 deliverable (REST + UI + audit) is self-consistent: 21/21 unit tests PASS, all 3 DoD criteria are verified at the unit level, security/DB integrity/frontend compatibility all PASS.
2. The integration gap is **environmental** — `SafetyEventSource` is owned by the T11 event-source block, not T13. Project rule `00-project-core.mdc` says "Do not refactor unrelated modules" and "Report conflicts instead of silently choosing one source". Applying the 5-line `@Entity` hotfix from inside T13 would cross module boundaries.
3. The same regression was first documented in the G3-T11 Phase 3 review for the broader G2 acceptance run. It is not a T13 regression.

**Recommended fix (out of scope for T13)**: add `@Entity @Table(name="safety_event_sources")` to `SafetyEventSource.java:1` and re-run the integration suite. Expected: 17/17 PASS, promoting T13 to **Phase 3 PASS**. Estimated effort: 5 lines + 1 re-run (~30s). Should be scheduled as a separate hotfix commit before any T11 caller wires in (G6 program matching hot path needs the integration suite green).

### Phase 3 Verification Command + Observed Result

- Unit-only: `mvnw.cmd -B test "-Dtest=ExpertReviewServiceTest,ExpertReviewTest"` → **21/21 PASS, BUILD SUCCESS** (~85s, 2026-08-03 22:18 local).
- Unit + Integration: `mvnw.cmd -B test "-Dtest=ExpertReviewServiceTest,ExpertReviewTest,ExpertReviewServiceIntegrationTest"` → **21/21 unit PASS, 0/17 integration (blocked by L-env-1)** (~87s, 2026-08-03 22:46 local).
