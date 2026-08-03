# Chat Analysis Prompt — v1

> **Task**: G3-T03 — Thiết kế prompt phân tích chat
> **Status**: v1 — initial design
> **Owner**: ____________________

## How to read this file

This is a reviewable prompt artefact. The prompt below is the
**system prompt** that will be sent to a hosted LLM (G3-T06) when
analysing a user chat message. The accompanying examples and rules
define the contract. The content is **frozen** for v1 — any change
must create `chat_analysis_prompt_v2.md`.

The content hash at the bottom of this file is the SHA-256 hex digest
of the system prompt body (between the `__PROMPT_BEGIN__` and
`__PROMPT_END__` markers). When the real LLM provider runs this prompt, it will
compute the hash at runtime and persist it as
`ai_analysis_runs.prompt_version` together with the literal version
tag `v1` so that any later tampering or copy-paste error is caught
in audit.

## System prompt (v1)

```text
---BEGIN---
You are an extraction layer for a Vietnamese mental-health support app.

Your task is EXTRACTION ONLY. You must NOT:
- Diagnose any mental health condition.
- Recommend CBT programs, modules, exercises, or treatments.
- Generate a conversational response to the user.
- Decide the final action, final risk level, or whether to escalate.
- Choose program matching, safety actions, or follow-up steps.

Those responsibilities belong to other systems that consume your
output. You only produce structured data describing what is in the
message.

Extract the following fields and return ONLY a single JSON object —
no prose, no markdown fences, no commentary before or after:

  topic:        one of
                WORK_STRESS | RELATIONSHIP | FAMILY | HEALTH | FINANCE | SLEEP | OTHER
  emotion:      one of
                NEUTRAL | HAPPY | ANXIOUS | SAD | OVERWHELMED | DISTRESS | ANGRY
  intent:       one of
                VENT | ADVICE | INFO | SUPPORT
  signals:      list of zero or more of
                FATIGUE | SLEEP_DISRUPTION | ISOLATION | HOPELESSNESS |
                BURNOUT | SELF_HARM_RISK | CONFLICT | GRIEF | OTHER
  modelRiskLevel:  integer in [1, 4]
                1 = normal
                2 = follow-up needed
                3 = high risk (Safety pipeline will open a safety event)
                4 = emergency (Safety pipeline will use a fixed approved response)
  confidence:   float in [0.0, 1.0]
  evidenceSpans: list of zero or more objects:
                { "start": <int>, "end": <int>, "textHash": "<64-char-sha256-hex>" }
                Each span must reference a contiguous substring of the
                input message; textHash is the SHA-256 hex digest of
                the substring (lower-case, 64 hex chars). Use an empty
                list when no specific span justifies the output.
  latencyMs:    always 0. The provider tracks actual latency; do not infer it.
  errorCode:    always null on success. The provider sets this on failure.

Rules:
- Return ONLY the JSON object. No prose, no markdown fences.
- All fields are required. Use empty list / 0.0 / null where appropriate.
- modelRiskLevel is the model's own signal only — it is NOT the final risk.
  The Safety Resolver combines this with keyword/regex signals and the
  user's current risk state.
- Do NOT apply clinical thresholds, decision rules, or escalation
  policies. You describe; you do not decide.
- If the content is empty or not analysable, set modelRiskLevel=1,
  confidence=0.0, signals=[], evidenceSpans=[], errorCode=null.

The input you will receive:
- currentMessage:    the user's current message (required, non-blank)
- contextSummary:    an optional short summary of recent context (<=500 chars)
---END---
```

## Few-shot examples

### Example 1 — Level 2 (follow-up)

Input:
```text
currentMessage: "Hôm nay deadline dí quá, tôi lo lắng không yên."
contextSummary: "User đang làm việc ở công ty, deadline tháng."
```

Expected output:
```json
{
  "topic": "WORK_STRESS",
  "emotion": "ANXIOUS",
  "intent": "VENT",
  "signals": ["BURNOUT"],
  "modelRiskLevel": 2,
  "confidence": 0.78,
  "evidenceSpans": [
    { "start": 18, "end": 26, "textHash": "<sha256-of-'lo lắng '>" }
  ],
  "latencyMs": 0,
  "errorCode": null
}
```

### Example 2 — Level 4 (emergency)

Input:
```text
currentMessage: "Tôi không muốn sống nữa."
contextSummary: ""
```

Expected output:
```json
{
  "topic": "HEALTH",
  "emotion": "DISTRESS",
  "intent": "SUPPORT",
  "signals": ["SELF_HARM_RISK", "HOPELESSNESS"],
  "modelRiskLevel": 4,
  "confidence": 0.95,
  "evidenceSpans": [
    { "start": 7, "end": 22, "textHash": "<sha256-of-'không muốn sống '>" }
  ],
  "latencyMs": 0,
  "errorCode": null
}
```

### Example 3 — WRONG (do NOT produce this)

The following is an example of output that would VIOLATE the contract.
If the model produces something like this, the consumer must treat it
as a failure (schema invalid, content rejected).

```text
❌ WRONG:
"The user appears to be experiencing work-related stress and signs of
burnout. Based on the severity, I would recommend that they consider
starting a CBT program focused on stress management. The system
should also check if the user qualifies for a Level 3 safety event..."
```

Why this is wrong:
- It contains prose instead of a JSON object.
- It recommends a CBT program ("recommend that they consider starting a
  CBT program") — that is the matching system's job, not the model's.
- It decides an action ("should also check if the user qualifies for a
  Level 3 safety event") — that is the Safety Resolver's job, not the
  model's.

## Context limits

The prompt accepts exactly two input fields:
- `currentMessage` — required, non-blank, max 10000 chars.
- `contextSummary` — optional, max 500 chars.

The mock and real providers do NOT accept a list of past messages.
Conversation history is summarised by the chat consumer into
`contextSummary` before being passed to the prompt. The summary is
intentionally a single bounded string to:
1. Keep the prompt size predictable across messages.
2. Avoid sending arbitrary amounts of prior context that could leak
   unrelated data into the model's view.
3. Avoid duplicating history in the analysis result.

This decision matches
`docs/tasks/G3/G3-T03-thiet-ke-prompt-phan-tich-chat.md` §2
("Giới hạn context gửi vào: message hiện tại và summary ngắn cần
thiết"). If a future task needs richer context, create
`chat_analysis_prompt_v2.md` rather than extending this one.

## Why no clinical thresholds in the prompt

`docs/04_SAFETY_AND_CBT_RULES.md` §1 explicitly forbids inventing
clinical thresholds, escalation policy, or crisis wording. The
prompt therefore tells the model to describe the message and let
the Safety Resolver (G3-T10) decide.

Specifically:
- The prompt does not say "if the user mentions self-harm more than
  3 times, set risk=4". The model produces a `signals` list; the
  Safety Resolver counts and combines signals deterministically.
- The prompt does not include any literal of the Level 4 fixed
  response. That text is owned by the Safety Resolver (G3-T10) and
  is itself expert-approved content (see
  `docs/04_SAFETY_AND_CBT_RULES.md` §12).
- The prompt does not include any reason-code taxonomy beyond the
  enumerated enums listed above. The taxonomy is intentionally
  minimal and intentionally open for extension under expert review.

## Structured output

When the real provider (G3-T06) supports structured output
(OpenAI JSON mode, Anthropic tool use, etc.), the prompt's
"return ONLY a JSON object" rule is enforced mechanically by the
provider. Until then, the prompt relies on model obedience +
client-side JSON validation.

In all cases, the client MUST validate the JSON against the
schema before persisting a successful run
(`docs/04_SAFETY_AND_CBT_RULES.md` §7: "JSON sai schema không được
coi là thành công").

## Content hash

The SHA-256 hex digest of the prompt body between the `---BEGIN---`
and `---END---` markers is computed locally before any test run and
stored alongside this file. When T06 wires up the real provider, the
hash MUST be computed at runtime from the loaded prompt and stored
in `ai_analysis_runs.prompt_version` together with the literal
version tag `v1`.

Format suggestion: `v1:<first-12-hex-chars>` — for example,
`v1:3a7f9c1b5e2d`. The full 64-char hash is stored alongside in
`ai_analysis_runs.error_code` only on hash-mismatch failures (so a
mismatch is auditable without leaking the wrong prompt into user
output).

> The local SHA-256 of the v1 prompt body is recorded at the end of
> this file in a fenced code block. Do not edit the prompt body
> without recomputing the hash.

```text
v1-prompt-body-sha256: 5363675e22fe77100908eaee6ab003207da57ba557e7c09d5d52671c1a9447e2
v1-prompt-body-sha256-short: v1:5363675e22fe
body-length-chars: 2732
computed-by: PowerShell [System.Security.Cryptography.SHA256] (2026-08-01)
```

## Versioning rules

- v1 is the initial, frozen version.
- Any change to the system prompt body, the rules, the field list,
  or the enum values creates v2 (a new file
  `chat_analysis_prompt_v2.md`). v1 stays in place for audit.
- The version string in `ai_analysis_runs.prompt_version` follows
  the format `v<major>:<hash-prefix>`.

## Related artefacts

- `docs/prompts/chat_analysis_test_cases.md` — fixed test set used
  to verify prompt behaviour via the Mock provider in CI.
- `backend/src/main/java/com/mindbridge/analysis/provider/ChatAnalysisOutput.java`
  — the output record this prompt must produce.
- `docs/04_SAFETY_AND_CBT_RULES.md` §7 — schema reference for
  LLM Safety Classification output (different DTO, different pipeline;
  see G3-T09).
- `docs/02_DATABASE_MVP.md` §5.2 — `chat_analysis_results` columns
  that the persisted run must populate.
