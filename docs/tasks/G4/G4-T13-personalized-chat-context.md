# G4-T13 — Personalized Chat Context

## Scope

Connect the authenticated user's approved account, Daily Check-in, and G4
behavior-profile data to ordinary AI chat responses without weakening the
independent Safety pipeline.

## Requirements

- Keep chat analysis and conversational response generation separate.
- Require current `PERSONALIZATION` consent before loading personalization data.
- Resolve identity from the authenticated principal; never accept a client `userId`.
- Include the user's display name so it is available across chat sessions.
- Include today's typed Daily Check-in answers, excluding free-text notes.
- Include the latest materialized G4 profile, data quality, dominant topics, and
  deterministic trend entries when available.
- Treat missing, stale, low-quality, and insufficient data explicitly; never invent data.
- Do not send passwords, email, raw audit data, raw Safety evidence, or raw Daily notes.
- Do not let LLM personalization replace Safety, ownership, consent, CBT, or matching rules.
- Chat must continue without personalization when consent or profile data is absent.

## Acceptance Criteria

1. A consented user can ask for their registered name in a new session.
2. A consented user can ask about today's Daily Check-in without repeating it in chat.
3. A consented user can discuss available G4 trends with uncertainty and data-quality context.
4. Revoked or absent `PERSONALIZATION` consent prevents all personalization context loading.
5. One user cannot receive another user's context.
6. Free-text Daily answers are not sent to the AI provider.
7. Level 3/4 Safety responses remain fixed and bypass the conversational provider.
8. Automated tests use mock/capturing providers and never call a real LLM.
