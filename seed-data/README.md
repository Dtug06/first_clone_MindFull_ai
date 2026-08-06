# `seed-data/` — Demo data for local development

> **Status: NOT_STARTED.** Seed data will be created **per task** when a
> feature requires realistic local fixtures. The first task likely to
> populate this folder is **G2-T09 — Seed dữ liệu và bộ kịch bản thu thập**.

## Purpose

Provide deterministic, demo-only data so that:

- Frontend developers can run the UI against realistic JSON without a live
  backend.
- Backend developers can populate a local database with users, CBT
  catalogs, daily question templates, etc., to develop and demo against.

## Important constraints

- All seed data must be clearly labeled **DEMO_ONLY**.
- Real-looking data must never be used to **prove** clinical, statistical
  or production claims. See [`docs/04_SAFETY_AND_CBT_RULES.md` §27](../../docs/04_SAFETY_AND_CBT_RULES.md).
- Never seed real passwords, real tokens or real personal information.
- Never seed clinical thresholds that have not been expert-approved — use
  the placeholders `TODO_EXPERT_REVIEW`, `CONFIG_PLACEHOLDER`, `DEMO_ONLY`.

## Planned structure

The shape will be defined by the task that creates each file. Likely
candidates:

```text
seed-data/
├── README.md
├── users.json              # demo accounts (password = DEMO_ONLY marker)
├── cbt-programs/           # CBT program/module/exercise versions
├── daily-questions/        # daily question templates
├── safety/                 # demo safety templates (DEMO_ONLY label required)
└── kb/                     # knowledge base (deferred — depends on RAG scope)
```

## Environment variables

Seed-data scripts (when present) may read connection details from `.env`:

- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`

`.env` is git-ignored; the template is [`.env.example`](../../.env.example).