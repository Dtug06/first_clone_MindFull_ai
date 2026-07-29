# `backend/` — MindBridge AI Spring Boot service

> **Status: SCAFFOLDED + DATABASE-WIRED (G1-T02 + G1-T03, 2026-07-30).**
> Spring Boot 3.3.5 project skeleton with Maven Wrapper, profile-based
> config and a single `@SpringBootApplication` entry point. Boots on
> profile `local` with embedded Tomcat on `http://localhost:8080/api/v1`.
> G1-T03 added the real PostgreSQL 17 datasource (via environment
> variables, no hard-coded secrets) — see § "Database setup" below for
> one-time install + DB creation steps. Flyway, security, Swagger UI and
> JPA entities are intentionally NOT added yet — each lands in a later
> `G1-T*` task.

## Quick start

```bash
# First-time only: Maven Wrapper downloads Maven 3.9.16 (~10 MB) into
# $USERPROFILE/.m2/wrapper/dists. Requires internet access on first run.
cd backend
./mvnw.cmd clean compile          # or:  .\mvnw.cmd clean compile
./mvnw.cmd spring-boot:run        # starts on http://localhost:8080/api/v1

# Smoke check
curl http://localhost:8080/api/v1/actuator/health
# -> {"status":"UP"}
```

On POSIX shells use `./mvnw` instead of `.\mvnw.cmd`.

## Stack

| Item | Value | Added in |
|---|---|---|
| Language | Java 21 | — |
| Framework | Spring Boot 3.3.5 | G1-T02 |
| Build tool | Maven + Maven Wrapper (3.3.4) | G1-T02 |
| Web | spring-boot-starter-web | G1-T02 |
| Validation | spring-boot-starter-validation | G1-T02 |
| Ops | spring-boot-starter-actuator (only `health` exposed) | G1-T02 |
| Database access | Spring Data JPA + PostgreSQL driver (H2 in tests) | G1-T03 |
| Auth | Spring Security + JWT (HS256) | G1-T06 |
| LLM | Hosted pretrained LLM (no training) | G3 |
| Docs API | springdoc-openapi-starter-webmvc-ui | G1-T10 |

## Module layout

Per [`docs/01_ARCHITECTURE.md` §3](../../docs/01_ARCHITECTURE.md), each
business module lives under `backend/src/main/java/com/mindbridge/<module>/`
with the following internal layering:

```text
<module>/
├── controller/    # Thin — HTTP only, no business logic
├── dto/           # Request/response DTOs (no JPA entities exposed)
├── service/       # Business logic + transaction boundary
├── domain/        # Pure domain types
├── repository/    # Persistence-only, returns entities
├── mapper/        # Entity ↔ DTO conversion only
└── exception/     # Module-specific exceptions and error codes
```

Module folders are NOT created in G1-T02. They will be created by the
first task that needs them (G1-T06 creates `auth/`, G1-T08 creates
`consent/`, etc.) — see "What will be added by which task" below.

Rule reference: [`.cursor/rules/10-backend.mdc`](../../.cursor/rules/10-backend.mdc).

## Spring profiles

`application.yml` is the base config. `spring.profiles.active: local`
is the default so `./mvnw spring-boot:run` works out of the box.

| Profile | File | Purpose |
|---|---|---|
| `local` | `application-local.yml` | Local dev. INFO logging. Reads datasource from env vars (see § "Database setup"). |
| `test` | `application-test.yml` | Automated tests. WARN logging. |
| `prod` | `application-prod.yml` | Production. WARN logging, file output. |

Set profile explicitly with:

```bash
./mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=test
```

## Environment variables

The backend reads configuration from environment variables. Local dev
loads them from `.env` (git-ignored) — see
[`.env.example`](../../.env.example) and the root
[`README.md`](../../README.md).

Variables required once the corresponding task lands:

| Variable | Used by | Required after |
|---|---|---|
| `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` | DataSource | G1-T03 |
| `JWT_SECRET`, `JWT_EXPIRATION` | Security | G1-T06 |
| `APP_CORS_ALLOWED_ORIGINS` | WebMvc CORS | G1-T10 |

G1-T02 adds NO new environment variables. Every profile in
`application-*.yml` works without any env var except `local` and `prod`,
which require `DATABASE_URL`, `DATABASE_USERNAME` and `DATABASE_PASSWORD`
to point at a running PostgreSQL 17 instance (see § "Database setup").
`test` uses an in-memory H2 and ignores those variables.

## What will be added by which task

| Task | What lands in `backend/` |
|---|---|
| G1-T02 | `pom.xml`, Maven Wrapper, Spring Boot main class, `application*.yml`, `logback-spring.xml`, profile config |
| G1-T03 | `spring-boot-starter-data-jpa`, `org.postgresql:postgresql`, H2 (test scope), `spring.datasource` per profile; § "Database setup" below |
| G1-T04 | Flyway configuration + `database/` migrations |
| G1-T05 | Shared `ErrorResponse` builder, validation, `ControllerAdvice` |
| G1-T06 | JWT login, `auth/` and `user/` modules |
| G1-T08 | `consent/` module |
| G1-T10 | Swagger / OpenAPI UI, CORS config |

## Database setup (PostgreSQL 17 — native install)

This project connects to a PostgreSQL 17 server running natively on the
dev machine. We do **not** ship a `docker-compose.yml`; the install path
must be reproducible by hand so the second dev (or a fresh clone)
follows the same steps.

### One-time install (Windows)

1. Download the PostgreSQL 17 installer from
   <https://www.postgresql.org/download/windows/>.
2. Run the installer. Use defaults: port `5432`, superuser `postgres`,
   service name `postgresql-x64-17`. **Save the password you set for
   `postgres`** — you will be prompted for it on every
   `psql -U postgres` unless you trust the local connection.
3. Verify the service is running. Any one of:

   ```powershell
   # (a) Windows service
   Get-Service postgresql-x64-17   # -> Status: Running

   # (b) TCP port
   Get-NetTCPConnection -LocalPort 5432 -State Listen   # -> row present

   # (c) pg_isready (qualified path — not on PATH by default)
   & "C:\Program Files\PostgreSQL\17\bin\pg_isready.exe" -h localhost -p 5432
   # -> "localhost:5432 - accepting connections"
   ```

### One-time database + user creation

The application connects as a **non-superuser** (`mindbridge_app`) to a
**dedicated database** (`mindbridge_dev`). The default installer user
`postgres` is for admin tasks only.

Open a PowerShell as Administrator and run (replace `MY_PASSWORD_HERE`
with a unique local dev password — do **not** commit it):

```powershell
# Add psql to PATH for this session (or just qualify full path).
$env:Path += ";C:\Program Files\PostgreSQL\17\bin"

# 1. Create the role (login user). Idempotent — safe to re-run.
psql -U postgres -d postgres -c "DO `$` BEGIN IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'mindbridge_app') THEN CREATE ROLE mindbridge_app WITH LOGIN PASSWORD 'MY_PASSWORD_HERE'; END IF; END `$`;"

# 2. Create the database owned by that role. Idempotent — safe to re-run.
psql -U postgres -d postgres -c "SELECT 'CREATE DATABASE mindbridge_dev OWNER mindbridge_app ENCODING ''UTF8''' WHERE NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'mindbridge_dev') \gexec"

# 3. Grant the minimum required. CONNECT + CRUD on public is enough for MVP.
psql -U postgres -d mindbridge_dev -c "GRANT CONNECT ON DATABASE mindbridge_dev TO mindbridge_app; GRANT USAGE, CREATE ON SCHEMA public TO mindbridge_app; ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO mindbridge_app;"
```

### Configure `.env`

Copy `.env.example` → `.env` (git-ignored) and fill in:

```text
DATABASE_URL=jdbc:postgresql://localhost:5432/mindbridge_dev
DATABASE_USERNAME=mindbridge_app
DATABASE_PASSWORD=MY_PASSWORD_HERE
```

### Smoke check

```powershell
cd backend
.\mvnw.cmd spring-boot:run
# In a second terminal:
curl http://localhost:8080/api/v1/actuator/health
# -> {"status":"UP"}
```

In the boot log you should see lines like:

```text
HikariPool-1 - Starting...
HikariPool-1 - Start completed.
o.s.j.d.DriverManagerDataSource  : Loaded JDBC driver: org.postgresql.Driver
```

### Timezone

PostgreSQL `timestamptz` stores absolute instants; the session timezone
only affects display. We enforce `UTC` at Hibernate via
`spring.jpa.properties.hibernate.jdbc.time_zone=UTC` so Hibernate
writes any `OffsetDateTime` / `@CreatedDate` in UTC regardless of host
TZ. The DB server timezone can stay whatever the OS prefers.

## Security reminders

- Never commit secrets. Use environment variables only.
- Never log JWT tokens, passwords, or full chat / exercise content.
- Never expose JPA entities directly through REST APIs — always go through DTOs.
- The current `application.yml` disables `server.error.include-stacktrace`,
  `include-exception`, `include-binding-errors`, and `include-message` to
  avoid leaking internal details to clients. A controlled `ErrorResponse`
  format will be added in G1-T05.