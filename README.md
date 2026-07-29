# MindBridge AI

A calmer bridge to your inner world — AI-powered mental health support platform for young Vietnamese users.

This repository contains three co-located areas of work:

| Area | Path | Status | Owner docs |
|---|---|---|---|
| Frontend prototype | `frontend/` | UI_PROTOTYPE — visual reference, not source of truth | [`frontend/README.md`](frontend/README.md) |
| Backend (planned) | `backend/` | NOT_STARTED — Spring Boot scaffold lands in G1-T02 | [`backend/README.md`](backend/README.md) |
| Database & seed (planned) | `database/`, `seed-data/` | NOT_STARTED — schema migrations land in G1-T04 | [`database/README.md`](database/README.md), [`seed-data/README.md`](seed-data/README.md) |
| Product docs | `docs/` | Living documents — task files are highest priority | [`docs/tasks/`](docs/tasks/) |

> **Important**: existing frontend pages are a UI prototype. They do **not** prove that backend business functions are implemented. See [`docs/05_IMPLEMENTATION_STATUS.md`](docs/05_IMPLEMENTATION_STATUS.md) for the real state of each feature.

---

## Prerequisites

- **Node.js 18+** and **npm** (for the frontend).
- **Java 21** and **Maven 3.9+** (will be required once G1-T02 lands).
- **PostgreSQL 14+** (will be required once G1-T03 lands).
- **Git 2.40+**.

---

## Quick start

```bash
# 1. Clone
git clone https://github.com/Dtug06/first_clone_MindFull_ai.git
cd first_clone_MindFull_ai

# 2. Frontend
cd frontend
npm install
npm run dev          # http://localhost:5173
npm run build        # production build into frontend/dist
npm run lint
cd ..

# 3. Backend (NOT_STARTED — placeholder only)
cd backend
# (no commands yet — see backend/README.md and G1-T02)
cd ..

# 4. Database (NOT_STARTED — placeholder only)
cd database
# (no commands yet — see database/README.md and G1-T03)
cd ..
```

---

## Project structure

```text
first_clone_MindFull_ai/
├── frontend/            # React 18 + TypeScript + Vite UI prototype
├── backend/             # Spring Boot (Java 21, Maven) — coming in G1-T02
├── database/            # PostgreSQL init scripts + Flyway migrations (G1-T04)
├── seed-data/           # Demo seed data for dev (populated per task)
├── docs/
│   ├── 00_PROJECT_SCOPE.md
│   ├── 01_ARCHITECTURE.md
│   ├── 02_DATABASE_MVP.md
│   ├── 03_API_CONTRACT.yaml
│   ├── 04_SAFETY_AND_CBT_RULES.md
│   ├── 05_IMPLEMENTATION_STATUS.md
│   ├── git-workflow.md
│   └── tasks/G1/ … G8/  # Per-group task files (one file per task)
├── .cursor/
│   ├── rules/           # Always-applied and globs-scoped project rules
│   └── commands/        # Cursor slash-commands (plan/implement/review)
├── .editorconfig
├── .env.example         # Copy to .env; never commit .env
├── .gitignore
├── CONTRIBUTING.md      # Branch + PR conventions for the 2-dev team
└── README.md            # This file
```

---

## Environment variables

Local secrets live in `.env` at the repository root. **`.env` is git-ignored**. A template lives at [`.env.example`](.env.example). Copy it once:

```bash
cp .env.example .env
# then edit .env with local values
```

The variables currently defined:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION`
- `APP_CORS_ALLOWED_ORIGINS`

---

## Documentation index

| Document | Purpose |
|---|---|
| [`docs/00_PROJECT_SCOPE.md`](docs/00_PROJECT_SCOPE.md) | Product scope, actors, MVP boundaries, DoD |
| [`docs/01_ARCHITECTURE.md`](docs/01_ARCHITECTURE.md) | Modular monolith, layering, transaction rules |
| [`docs/02_DATABASE_MVP.md`](docs/02_DATABASE_MVP.md) | Tables, invariants, migration order |
| [`docs/03_API_CONTRACT.yaml`](docs/03_API_CONTRACT.yaml) | OpenAPI 3.0.3 contract (single source of truth for endpoints) |
| [`docs/04_SAFETY_AND_CBT_RULES.md`](docs/04_SAFETY_AND_CBT_RULES.md) | Risk levels, CBT state machine, expert-review policy |
| [`docs/05_IMPLEMENTATION_STATUS.md`](docs/05_IMPLEMENTATION_STATUS.md) | Live status of every feature and migration |
| [`docs/git-workflow.md`](docs/git-workflow.md) | Branching and commit conventions |
| [`docs/tasks/`](docs/tasks/) | Per-task files — highest priority for implementation details |

---

## Git workflow

Branches:

- `main` — production-ready, **protected**. Only merged from `develop`.
- `develop` — default integration branch.
- `feature/Gx-Txx-short-name` — one task = one branch.
- `hotfix/...` — emergency fixes to `main`.

Commits follow [Conventional Commits](https://www.conventionalcommits.org/). Pull requests target `develop` and require one peer reviewer. See [`CONTRIBUTING.md`](CONTRIBUTING.md) and [`docs/git-workflow.md`](docs/git-workflow.md) for the full policy.

The current frontend prototype is frozen at the tag:

```text
v0.0.1-frontend-baseline
```

This tag points to commit `128eece` (the last frontend-only state before backend work begins). You can always return to the original UI with:

```bash
git checkout v0.0.1-frontend-baseline
```

---

## License

Proprietary — MindBridge AI Team 2026.