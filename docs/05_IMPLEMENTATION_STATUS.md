# MindBridge AI — Implementation Status

## 1. Purpose

File này mô tả trạng thái hiện tại của source code.

Mục tiêu:

- Giúp hai developer biết phần nào đã hoàn thành.
- Giúp Cursor không tạo lại chức năng đã tồn tại.
- Theo dõi API, migration và frontend integration.
- Ghi nhận mock data, giới hạn và lỗi đã biết.
- Hỗ trợ chọn task tiếp theo.

File này phải phản ánh source code thực tế.

Không ghi một chức năng là `COMPLETED` nếu chưa kiểm tra source, build hoặc test.

---

# 2. Status Definitions

| Status | Meaning |
|---|---|
| NOT_STARTED | Chưa bắt đầu |
| READY | Đã đủ thông tin để triển khai |
| IN_PROGRESS | Đang triển khai |
| BLOCKED | Bị chặn bởi dependency hoặc vấn đề kỹ thuật |
| IN_REVIEW | Đã code, đang review hoặc test |
| COMPLETED | Đã merge và đạt Definition of Done |
| DEFERRED | Hoãn khỏi MVP |
| MOCK_ONLY | Chỉ có mock, chưa có backend hoặc dữ liệu thật |
| PARTIAL | Có một phần nhưng chưa hoàn thiện toàn bộ |

---

# 3. Project Summary

- Project: MindBridge AI
- Architecture: Modular Monolith
- Backend: Java 21 + Spring Boot
- Database: PostgreSQL + Flyway
- Frontend: React
- AI: Hosted pretrained LLM API
- Team size: 2 developers
- Current branch: `develop`
- Current release target: `MVP`
- Last updated: `YYYY-MM-DD`
- Updated by: `DEV_NAME`

---

# 4. Current Development Focus

```text
Current Group:
G1 / G2 / G3 / G4 / G5 / G6 / G7 / G8

Current Task:
Gx-Txx-task-name

Current Goal:
Mô tả ngắn mục tiêu đang thực hiện.

Current Blockers:
- NONE
```

Ví dụ:

```text
Current Group:
G1 — Backend Foundation, Auth and Consent

Current Task:
G1-T04-implement-jwt-login

Current Goal:
Frontend đăng nhập bằng API thật và truy cập được protected endpoint.

Current Blockers:
- NONE
```

---

# 5. Group Status

| Group | Name | Status | Owner | Notes |
|---|---|---|---|---|
| G1 | Backend Foundation, Auth and Consent | NOT_STARTED | UNASSIGNED | |
| G2 | Chat, Daily Check-in and Data Collection | NOT_STARTED | UNASSIGNED | |
| G3 | LLM Integration and Safety | NOT_STARTED | UNASSIGNED | |
| G4 | Behavior Analysis and User Profile | NOT_STARTED | UNASSIGNED | |
| G5 | CBT Catalog and Runtime | NOT_STARTED | UNASSIGNED | |
| G6 | Program Matching and Recommendation | NOT_STARTED | UNASSIGNED | |
| G7 | Frontend Integration, Dashboard and Admin | PARTIAL | UNASSIGNED | Frontend cơ bản đã tồn tại |
| G8 | Testing, Security, Deployment and Documentation | IN_PROGRESS | BOTH | Thực hiện xuyên suốt |

---

# 6. Completed Tasks

Chỉ ghi task đã đạt Definition of Done.

| Task ID | Task Name | Owner | Pull Request | Completed Date | Verification |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

Ví dụ:

| Task ID | Task Name | Owner | Pull Request | Completed Date | Verification |
|---|---|---|---|---|---|
| G1-T01 | Initialize Spring Boot backend | Dev A | #1 | 2026-08-01 | `./mvnw test` PASS |
| G1-T02 | Configure PostgreSQL and Flyway | Dev B | #2 | 2026-08-02 | Migration from empty DB PASS |

---

# 7. Tasks In Progress

| Task ID | Task Name | Owner | Started Date | Branch | Current State |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

Ví dụ:

| Task ID | Task Name | Owner | Started Date | Branch | Current State |
|---|---|---|---|---|---|
| G1-T04 | Implement JWT authentication | Dev A | 2026-08-03 | feature/G1-T04-jwt | Login works, ownership test pending |

---

# 8. Ready Tasks

Các task có thể bắt đầu ngay.

| Priority | Task ID | Task Name | Dependencies | Suggested Owner |
|---|---|---|---|---|
| MUST | — | — | — | — |

Chỉ nên có khoảng 5–10 task ở trạng thái READY.

---

# 9. Blocked Tasks

| Task ID | Task Name | Blocked By | Required Action | Owner |
|---|---|---|---|---|
| — | — | — | — | — |

Ví dụ:

| Task ID | Task Name | Blocked By | Required Action | Owner |
|---|---|---|---|---|
| G3-T02 | Real LLM Provider | Chưa có API key | Cấu hình provider và quota | Dev A |

---

# 10. Deferred Tasks

| Task ID or Feature | Reason | Reconsider After |
|---|---|---|
| Multi-model embedding | Không cần cho MVP | Sau RAG MVP |
| Outbox events | Chưa có asynchronous integration | Trước production |
| Maintenance plans | Không ảnh hưởng demo chính | Sau CBT Runtime |

---

# 11. Backend Module Status

| Module | Status | Implemented Features | Missing Features |
|---|---|---|---|
| auth | NOT_STARTED | — | Register, Login, JWT |
| user | NOT_STARTED | — | User Entity, Current User |
| consent | NOT_STARTED | — | Grant, Revoke, Current State |
| chat | NOT_STARTED | — | Session, Message |
| checkin | NOT_STARTED | — | Template, Assignment, Answer |
| analysis | NOT_STARTED | — | Mock Provider, Real Provider |
| safety | NOT_STARTED | — | Risk Resolver, Safety Event |
| behavior | NOT_STARTED | — | Daily Feature, Profile |
| cbt | NOT_STARTED | — | Catalog, Runtime |
| matching | NOT_STARTED | — | Run, Candidate, Decision |
| recommendation | NOT_STARTED | — | Recommendation, Feedback |
| knowledge | DEFERRED | — | RAG |
| audit | NOT_STARTED | — | Basic Audit |

---

# 12. Frontend Status

## Existing Pages

| Page or Route | Exists | Uses Mock Data | Real API Connected | Notes |
|---|---|---|---|---|
| Login | UNKNOWN | UNKNOWN | NO | Audit required |
| Register | UNKNOWN | UNKNOWN | NO | Audit required |
| Chat | YES | YES | NO | Existing UI should be preserved |
| Daily Check-in | UNKNOWN | UNKNOWN | NO | |
| Dashboard | YES | YES | NO | |
| Program Detail | UNKNOWN | UNKNOWN | NO | |
| Exercise | UNKNOWN | UNKNOWN | NO | |
| Admin | UNKNOWN | UNKNOWN | NO | |

Sau audit repository, thay `UNKNOWN` bằng trạng thái thực tế.

## Frontend Infrastructure

| Item | Status | Notes |
|---|---|---|
| Shared API Client | UNKNOWN | |
| Auth Interceptor | UNKNOWN | |
| Protected Routes | UNKNOWN | |
| Error Handler | UNKNOWN | |
| Environment Configuration | UNKNOWN | |
| TypeScript API DTO | UNKNOWN | |
| Mock/Real API Switch | UNKNOWN | |

---

# 13. API Implementation Status

| Endpoint | Method | Backend | Frontend | Test | Notes |
|---|---|---|---|---|---|
| `/api/v1/health` | GET | NOT_STARTED | NOT_NEEDED | NOT_STARTED | |
| `/api/v1/auth/register` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/auth/login` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/users/me` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/consents` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/consents/current` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/chat/sessions/{id}/messages` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/daily-checkins/today` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/behavior/profile` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/matching/run` | POST | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |
| `/api/v1/user-programs` | GET | NOT_STARTED | NOT_CONNECTED | NOT_STARTED | |

Status backend:

```text
NOT_STARTED
PARTIAL
IMPLEMENTED
```

Status frontend:

```text
NOT_CONNECTED
MOCK_ONLY
CONNECTED
```

---

# 14. Database Migration Status

| Migration | Purpose | Task | Status | Applied Locally | Applied Test |
|---|---|---|---|---|---|
| — | — | — | — | — | — |

Ví dụ:

| Migration | Purpose | Task | Status | Applied Locally | Applied Test |
|---|---|---|---|---|---|
| V1__enable_extensions.sql | Enable pgcrypto and citext | G1-T02 | COMPLETED | YES | YES |
| V2__create_users.sql | Create users table | G1-T03 | COMPLETED | YES | YES |

Quy tắc:

- Không sửa migration đã ghi `COMPLETED`.
- Migration mới phải có tên mới.
- Hai developer phải tránh trùng version.

---

# 15. Implemented Database Tables

| Table | Migration | Task | Status | Notes |
|---|---|---|---|---|
| users | — | — | NOT_STARTED | |
| consent_events | — | — | NOT_STARTED | |
| audit_logs | — | — | NOT_STARTED | |
| chat_sessions | — | — | NOT_STARTED | |
| conversation_messages | — | — | NOT_STARTED | |
| daily_question_templates | — | — | NOT_STARTED | |
| daily_question_options | — | — | NOT_STARTED | |
| daily_question_assignments | — | — | NOT_STARTED | |
| daily_question_answers | — | — | NOT_STARTED | |
| behavioral_events | — | — | NOT_STARTED | |
| ai_analysis_runs | — | — | NOT_STARTED | |
| chat_analysis_results | — | — | NOT_STARTED | |
| risk_state_history | — | — | NOT_STARTED | |
| safety_events | — | — | NOT_STARTED | |
| safety_event_sources | — | — | NOT_STARTED | |
| user_daily_features | — | — | NOT_STARTED | |
| user_behavior_profiles | — | — | NOT_STARTED | |
| user_behavior_profile_snapshots | — | — | NOT_STARTED | |

Không cần liệt kê toàn bộ bảng chưa gần triển khai. Có thể bổ sung theo từng group.

---

# 16. AI Provider Status

| Item | Status | Notes |
|---|---|---|
| ChatAnalysisProvider Interface | NOT_STARTED | |
| MockChatAnalysisProvider | NOT_STARTED | |
| RealLlmChatAnalysisProvider | NOT_STARTED | |
| Structured Output Schema | NOT_STARTED | |
| JSON Validation | NOT_STARTED | |
| Timeout | NOT_STARTED | |
| Retry | NOT_STARTED | |
| Error Persistence | NOT_STARTED | |
| Token Usage Tracking | NOT_STARTED | |

Current Provider:

```text
NONE
```

Environment Variable Names:

```text
TODO
```

Không ghi API key thật vào file này.

---

# 17. Safety Status

| Feature | Status | Notes |
|---|---|---|
| Keyword/Regex Pre-filter | NOT_STARTED | |
| LLM Risk Classification | NOT_STARTED | |
| Final Risk Resolver | NOT_STARTED | |
| Risk State History | NOT_STARTED | |
| Safety Event | NOT_STARTED | |
| Safety Event Source | NOT_STARTED | |
| Level 3 Matching Block | NOT_STARTED | |
| Level 4 Fixed Response | NOT_STARTED | |
| Expert Review | NOT_STARTED | |

Approved values still missing:

```text
- Production keyword list
- Production thresholds
- Level 4 response text
- Expert escalation workflow
```

Status:

```text
TODO_EXPERT_REVIEW
```

---

# 18. Behavior Analysis Status

| Feature | Status | Calculation Version | Notes |
|---|---|---|---|
| stress_score | NOT_STARTED | — | |
| mood_score | NOT_STARTED | — | |
| energy_score | NOT_STARTED | — | |
| sleep_hours | NOT_STARTED | — | |
| anxiety_signal | NOT_STARTED | — | |
| engagement_score | NOT_STARTED | — | |
| exercise_completion_rate | NOT_STARTED | — | |
| max_risk_level | NOT_STARTED | — | |
| 7-day average | NOT_STARTED | — | |
| 30-day average | NOT_STARTED | — | |
| trend | NOT_STARTED | — | |
| coverage | NOT_STARTED | — | |
| confidence | NOT_STARTED | — | |
| profile snapshot | NOT_STARTED | — | |

---

# 19. CBT Status

## CBT Catalog

| Component | Status | Notes |
|---|---|---|
| Program Logical Entity | NOT_STARTED | |
| Program Version | NOT_STARTED | |
| Module Logical Entity | NOT_STARTED | |
| Module Version | NOT_STARTED | |
| Exercise Logical Entity | NOT_STARTED | |
| Exercise Version | NOT_STARTED | |
| Version Immutability Test | NOT_STARTED | |

## Programs

| Program Code | Content Status | Database Status | Notes |
|---|---|---|---|
| CBT-STRESS | DEMO_ONLY | NOT_STARTED | |
| CBT-MOOD | DEMO_ONLY | NOT_STARTED | |
| CBT-WORRY | DEMO_ONLY | NOT_STARTED | |
| CBT-SLEEP | DEMO_ONLY | NOT_STARTED | |

## CBT Runtime

| Component | Status |
|---|---|
| User Program | NOT_STARTED |
| Program State Transition | NOT_STARTED |
| Module Progress | NOT_STARTED |
| Exercise Assignment | NOT_STARTED |
| Exercise Submission | NOT_STARTED |
| Baseline Assessment | NOT_STARTED |
| Weekly Assessment | NOT_STARTED |
| Final Assessment | NOT_STARTED |

---

# 20. Program Matching Status

| Feature | Status | Notes |
|---|---|---|
| Safety Gate | NOT_STARTED | |
| Matching Run | NOT_STARTED | |
| Eligibility Check | NOT_STARTED | |
| Exclusion Check | NOT_STARTED | |
| Candidate Score | NOT_STARTED | |
| Candidate Persistence | NOT_STARTED | |
| Matching Decision | NOT_STARTED | |
| Reason Codes | NOT_STARTED | |
| User Confirmation | NOT_STARTED | |

Current rule version:

```text
NONE
```

Current threshold bundle:

```text
NONE
```

Demo matching configuration:

```text
NOT_CREATED
```

---

# 21. Testing Status

| Test Area | Status | Latest Result |
|---|---|---|
| Backend Build | NOT_RUN | |
| Backend Unit Tests | NOT_RUN | |
| Backend Integration Tests | NOT_RUN | |
| Flyway Migration Test | NOT_RUN | |
| Frontend Type Check | NOT_RUN | |
| Frontend Build | NOT_RUN | |
| End-to-End Test | NOT_RUN | |
| Safety Tests | NOT_RUN | |
| CBT State Tests | NOT_RUN | |
| Matching Tests | NOT_RUN | |

Không ghi `PASS` nếu không có command đã chạy.

---

# 22. Deployment Status

| Component | Status | Environment |
|---|---|---|
| Backend | NOT_DEPLOYED | |
| Frontend | NOT_DEPLOYED | |
| PostgreSQL | NOT_DEPLOYED | |
| LLM Configuration | NOT_CONFIGURED | |
| Health Check | NOT_IMPLEMENTED | |
| Seed Data | NOT_CREATED | |

---

# 23. Known Issues

| ID | Severity | Description | Owner | Status |
|---|---|---|---|---|
| — | — | — | — | — |

Severity:

```text
BLOCKING
HIGH
MEDIUM
LOW
```

Ví dụ:

| ID | Severity | Description | Owner | Status |
|---|---|---|---|---|
| ISSUE-001 | HIGH | Frontend ChatPage vẫn sử dụng mock data | Dev B | OPEN |
| ISSUE-002 | MEDIUM | Chưa có refresh token | Dev A | DEFERRED |

---

# 24. Technical Decisions

Ghi lại các quyết định quan trọng đã chốt.

| Decision ID | Decision | Date | Reason |
|---|---|---|---|
| ADR-001 | Use Modular Monolith | YYYY-MM-DD | Team size 2 |
| ADR-002 | Use hosted LLM, no training | YYYY-MM-DD | Reduce complexity |
| ADR-003 | Use immutable CBT versions | YYYY-MM-DD | Preserve historical content |

---

# 25. Environment and Setup Notes

## Backend

```text
Java version:
Spring Boot version:
Maven version:
Backend port:
```

## Frontend

```text
Node version:
Package manager:
Frontend port:
```

## Database

```text
PostgreSQL version:
Database name:
Required extensions:
```

## Required Environment Variables

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD
JWT_SECRET
LLM_PROVIDER
LLM_API_KEY
```

Không ghi giá trị secret thật.

---

# 26. Next Recommended Tasks

Thứ tự task tiếp theo:

1. `TASK_ID`
2. `TASK_ID`
3. `TASK_ID`

Lý do:

```text
Mô tả dependency và mức ưu tiên.
```

Ví dụ:

```text
1. G1-T01 — Audit current repository
2. G1-T02 — Configure backend and database
3. G1-T03 — Implement user domain
4. G1-T04 — Implement authentication
```

---

# 27. Update Checklist

Sau mỗi Pull Request:

- [ ] Cập nhật Completed Tasks.
- [ ] Xóa task khỏi In Progress.
- [ ] Cập nhật Group Status.
- [ ] Cập nhật API Status.
- [ ] Cập nhật Migration Status.
- [ ] Cập nhật Database Table Status.
- [ ] Cập nhật Known Issues.
- [ ] Ghi command test thực tế.
- [ ] Cập nhật Last Updated.
- [ ] Cập nhật Next Recommended Tasks.

---

# 28. Cursor Usage Note

Trước mỗi task, Cursor phải đọc:

```text
docs/05_IMPLEMENTATION_STATUS.md
```

Cursor phải:

1. Kiểm tra trạng thái source hiện tại.
2. Không tạo lại chức năng đã completed.
3. Không giả định migration chưa có.
4. Báo nếu file status không khớp source.
5. Ưu tiên source code và migration thực tế khi phát hiện status đã lỗi thời.
6. Cập nhật file này sau khi task hoàn thành và được xác nhận.

File này không thay thế việc đọc source code.