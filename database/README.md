# `database/` — PostgreSQL schema and migrations

> **Status: NOT_STARTED.** This directory exists as a placeholder for the
> database scripts that will land in **G1-T03 — Thiết lập PostgreSQL và cấu hình môi trường**.

## Planned layout

```text
database/
├── README.md                # This file
├── init.sql                 # Local dev bootstrap: create role + db + extensions
└── migrations/              # Flyway-managed schema migrations (V1__...sql, V2__...sql, ...)
```

- `init.sql` is run **once** by the developer to bootstrap a local PostgreSQL
  instance. It is **not** part of the Flyway chain.
- `migrations/` is the only folder the Spring Boot backend points Flyway at.
  See [`docs/01_ARCHITECTURE.md` §15](../../docs/01_ARCHITECTURE.md) and
  [`docs/02_DATABASE_MVP.md` §13](../../docs/02_DATABASE_MVP.md).

## Database invariants (apply to every migration)

From [`docs/02_DATABASE_MVP.md`](../../docs/02_DATABASE_MVP.md) and
[`.cursor/rules/30-database-ai-safety.mdc`](../../.cursor/rules/30-database-ai-safety.mdc):

- Primary key is **UUID** unless a task explicitly requires otherwise.
- All timestamps are **`timestamptz`** and stored in **UTC**.
- Tables and columns use **snake_case**.
- **Flyway** is the only mechanism for schema changes.
- **Never edit** a migration that has already been merged or applied.
- **Do not create all tables in one migration** — one table (or one logical
  group) per migration.
- Add foreign keys, unique constraints and check constraints **deliberately**.
- Add indexes **only** for known query patterns.
- Use JSONB for flexible data, but **typed columns** for metrics queried often.

## Migration order (from `docs/02_DATABASE_MVP.md` §13)

1. PostgreSQL extensions (`pgcrypto`, `citext`, …).
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
16. RAG (deferred — only if time permits).

## Environment variables

Database connection parameters live in `.env` at the repo root; see
[`.env.example`](../../.env.example):

- `DATABASE_URL` (e.g. `jdbc:postgresql://localhost:5432/mindbridge`)
- `DATABASE_USERNAME`
- `DATABASE_PASSWORD`

## Ownership reminder

Every row in a user-scoped table must be reachable from `users.id` so that
ownership checks at the service layer are simple and correct. See
[`docs/02_DATABASE_MVP.md` §14](../../docs/02_DATABASE_MVP.md) and
[`.cursor/rules/30-database-ai-safety.mdc`](../../.cursor/rules/30-database-ai-safety.mdc).