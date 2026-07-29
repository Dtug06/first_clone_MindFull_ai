# Contributing to MindBridge AI

> Audience: the **two developers** working on this MVP, plus any future
> maintainer. This document is intentionally short and opinionated.

## 1. Source of truth

Always resolve conflicts in this order:

1. The **current task file** in `docs/tasks/Gx/Gx-Txx-...md`.
2. `docs/03_API_CONTRACT.yaml`.
3. `docs/02_DATABASE_MVP.md`.
4. `docs/04_SAFETY_AND_CBT_RULES.md`.
5. `docs/01_ARCHITECTURE.md`.
6. `docs/00_PROJECT_SCOPE.md`.
7. The frontend prototype (UI reference only — not a business rule source).

If two documents disagree, **stop** and surface the conflict to your
co-developer. Do not silently pick one.

## 2. Branches

| Branch | Purpose |
|---|---|
| `main` | Production-ready, **protected**. |
| `develop` | Default integration branch. |
| `feature/Gx-Txx-short-name` | One task = one branch. |
| `hotfix/short-name` | Emergency fixes against `main`. |

Detailed rules live in [`docs/git-workflow.md`](docs/git-workflow.md).

## 3. Commits

Follow [Conventional Commits](https://www.conventionalcommits.org/).

```text
<type>(<scope>): <short summary>

<body — explain the why, not the what>

<footer — task ID, breaking changes, etc.>
```

Common types: `feat`, `fix`, `chore`, `docs`, `refactor`, `test`.

Examples:

```text
feat(chat): send message via REST API and persist raw content
chore(G1-T01): standardize repo, freeze frontend baseline, add .env.example
docs(G1-T05): document shared ErrorResponse format
```

Always reference the task ID in the footer or summary, e.g. `(G1-T01)`.

## 4. Pull requests

- One **task** = one PR. Do not bundle multiple tasks.
- Target branch: `develop` (only hotfix targets `main`).
- Title format: `[Gx-Txx] <short summary>`.
- Required checks:
  - The local Maven / npm command relevant to the change must be reported
    in the PR description with the actual result (not "should pass").
  - Do not claim a test passed unless the command was actually executed.
  - Secrets, JWTs and full chat / submission content must not appear in
    logs or PR descriptions.
- Reviewer: the other developer. Use the
  [`.cursor/commands/review-task.md`](.cursor/commands/review-task.md)
  template as a guide for review checklists.

## 5. Definition of Done

A task is **Done** only when **all** of these are true:

- [ ] Code is complete and matches the task file.
- [ ] `./mvnw …` (or relevant frontend command) runs and the actual result
      is recorded.
- [ ] Migrations (if any) run from an empty database.
- [ ] API matches `docs/03_API_CONTRACT.yaml` (or the change is documented
      there first).
- [ ] Validation, error handling, ownership check are present where the
      task requires them.
- [ ] No secrets committed; no sensitive content in logs.
- [ ] `docs/05_IMPLEMENTATION_STATUS.md` is updated.
- [ ] Reviewer approved.

## 6. Secrets — never commit

The following must never be tracked in Git:

- Real database credentials.
- Real JWT secrets or session tokens.
- API keys for LLM providers.
- Real personal data of any kind.

Always use environment variables. The template is [`.env.example`](.env.example);
copy it to `.env` locally (which is git-ignored).

## 7. Communication

- One chat thread per task, divided into three phases when working with
  the AI agent:
  1. **Read-only plan** — agent inspects the repo and proposes files.
  2. **Implement** — only after explicit approval of the plan.
  3. **Review** — a new chat to verify acceptance criteria.
- When you finish a task, update `docs/05_IMPLEMENTATION_STATUS.md` and
  the task checklist in `docs/tasks/Gx/Gx-Txx-...md`.