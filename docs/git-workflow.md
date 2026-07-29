# Git workflow

This document complements [`.cursor/rules/00-project-core.mdc`](../.cursor/rules/00-project-core.mdc)
and [`CONTRIBUTING.md`](../CONTRIBUTING.md). It defines the **branch** and
**commit** conventions used by the 2-developer MVP team.

## 1. Branches

```text
main                       # protected, production-ready
└── develop                # default integration branch
    ├── feature/G1-T01-... # one task = one branch
    ├── feature/G1-T02-...
    ├── …
    └── hotfix/short-name  # emergency fixes → merges back to main + develop
```

### Rules

- **`main` is protected.** No direct pushes. All changes enter via PR.
- **`develop` is the default target** for `feature/*` PRs.
- **One task = one branch = one PR.** Do not bundle unrelated tasks.
- Branch name must include the task ID: `feature/Gx-Txx-short-kebab-name`.
  Example: `feature/G1-T01-baseline-standardization`.
- Delete the branch on the remote after it is merged.

### Frontend baseline

Before backend work began, the frontend prototype was frozen at tag
`v0.0.1-frontend-baseline` (pointing at commit `128eece`). To return to
the original UI at any time:

```bash
git checkout v0.0.1-frontend-baseline
```

## 2. Commits

Use [Conventional Commits](https://www.conventionalcommits.org/).

```text
<type>(<scope>): <short imperative summary>

<longer body explaining WHY, not WHAT>

Refs: Gx-Txx
```

Allowed types:

| Type | When |
|---|---|
| `feat` | New user-visible feature |
| `fix` | Bug fix |
| `chore` | Repo hygiene, dependencies, config (no behavior change) |
| `docs` | Documentation only |
| `refactor` | Code change with no behavior change |
| `test` | Adding or fixing tests |
| `perf` | Performance improvement |

Subject line ≤ 72 characters, imperative mood, no trailing period.
Reference the task ID in the body or footer.

## 3. Pull request checklist

A PR is ready when:

- [ ] Branch follows `feature/Gx-Txx-...` convention.
- [ ] Title format `[Gx-Txx] <summary>`.
- [ ] One task only.
- [ ] Relevant build / test command was actually run; the result is pasted
      into the PR description.
- [ ] No secrets, JWTs or sensitive content in the diff or description.
- [ ] `docs/05_IMPLEMENTATION_STATUS.md` updated (or PR is followed by a
      doc-only commit).
- [ ] OpenAPI updated **before** the code if an endpoint or DTO changed.

## 4. Conflict resolution

- Always rebase your branch on top of the latest `develop` before requesting
  review (small teams → fast-forward keeps history linear).
- If two PRs touch the same file, the second author rebases.
- Do not force-push to `develop` or `main`.

## 5. Releases

The MVP uses simple tag-based releases:

- `vMAJOR.MINOR.PATCH` (e.g. `v0.1.0`).
- Tag is created on `main` after the release PR is merged.
- The tag is annotated and references the GitHub release notes.