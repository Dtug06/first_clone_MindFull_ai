# Logging and Audit Conventions

> MindBridge AI — G1-T09 baseline rules.
> Status: **Approved for foundation phase (G1)**. Final retention policy
> requires explicit sign-off before any automated deletion is enabled.

## 1. Application logs (stdout / file)

### 1.1 What goes in application logs
- `requestId` (UUID) — propagated automatically by `LoggingRequestContextFilter`.
- `userId` (UUID) — when the request is authenticated. Never log a user email
  here; use the userId and look up the email through the admin tooling.
- Class + log message + structured context.

### 1.2 What MUST NOT go in application logs
- Passwords (plain or hashed).
- JWTs, refresh tokens, session cookies.
- Raw chat content, raw prompt/response from AI providers.
- API keys, encryption keys, OAuth secrets.
- Personally identifying information beyond userId.

The `[requestId]` token in the logback pattern is what links log lines to
the `X-Request-Id` response header that the same request produced.

## 2. Request tracing

`LoggingRequestContextFilter` (registered with `@Order(1)`) does:
1. Read `X-Request-Id` from the inbound request; if absent or blank, generate a UUID.
2. Store it in MDC under `requestId`.
3. Echo it on the response as `X-Request-Id`.
4. Clear MDC in a `finally` block.

This lets any failed request be traced end-to-end:
- Client receives `X-Request-Id: <uuid>` even on error responses.
- Operator greps logs for that UUID and sees every line emitted by the
  request, including the audit row that mentions `request_id = <uuid>`.

## 3. Audit logs (`audit_logs` table)

### 3.1 Events recorded in G1

| Event | Category | Action | Actor | Subject |
|---|---|---|---|---|
| Login failed (bad password / unknown user / suspended) | AUTH | `LOGIN_FAILED` | ANONYMOUS | `subject_type = user`, no subjectId, `metadata.emailHash = SHA-256(email)` |
| User grants a consent | CONSENT | `CONSENT_GRANTED` | USER (current userId) | `subject_type = consent_event`, subjectId = new event id |
| User revokes a consent | CONSENT | `CONSENT_REVOKED` | USER | `subject_type = consent_event` |
| Role change (future admin endpoint) | USER | `ROLE_CHANGED` | ADMIN (actor) | target userId as subjectId |
| Generic admin action (future) | ADMIN | `ADMIN_ACTION` | ADMIN | per action |

The `AuditService` interface is reserved for ROLE_CHANGED and ADMIN_ACTION
even though no endpoints that produce those events exist in G1. Adding
endpoints in later groups must use the same service.

### 3.2 What MUST NOT go in `audit_logs`
- Plain-text email address. Use `LogSanitizer.sha256Hex(email)` instead.
- Raw IP addresses (currently not collected).
- Passwords, tokens, chat content, prompts, responses.

### 3.3 Retention
- Application logs: demo environment retains them for **7 days** on the local
  filesystem. Production retention will be set by ops team.
- `audit_logs` table: **no automated deletion** is implemented. Hard-deleting
  audit history requires an approved retention rule (per
  `.cursor/rules/30-database-ai-safety.mdc`). Truncation would break
  traceability and is therefore forbidden until policy is approved.

## 4. Operator runbook (cheat sheet)

- **Find every audit row for one HTTP request**:
  `SELECT * FROM audit_logs WHERE request_id = '<X-Request-Id>';`
- **Find every login failure from the same user over the last day**:
  `SELECT * FROM audit_logs WHERE category = 'AUTH' AND action = 'LOGIN_FAILED'
   AND metadata->>'emailHash' = '<sha256-of-email>';`
- **Find every consent event a user emitted**:
  `SELECT * FROM audit_logs WHERE subject_type = 'consent_event' AND actor_id = '<userId>'
   ORDER BY created_at DESC;`
