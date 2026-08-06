# Risk Classifier — Test Cases (DEMO_ONLY)

> **Status: DEMO_ONLY** — for `MockRiskClassifierProvider` (G3-T09).
>
> These cases drive the deterministic keyword→scenario mapping in the
> mock provider. Production reason-code taxonomy is an
> expert-review item per
> `docs/04_SAFETY_AND_CBT_RULES.md` §7 ("Ví dụ schema" shows only
> `DISTRESS_SIGNAL` and `SLEEP_DISRUPTION` as examples) and §1
> ("Cursor không được tự đặt threshold hoặc tự thay đổi các quy
 * tắc"). When the real taxonomy is approved, this file is replaced
> or extended — labels here are intentionally suffixed `_DEMO`.

> **Scope**: this file covers the keyword mapping that the
> `MockRiskClassifierProvider` performs. The end-to-end safety
> decision (combining classifier output + keyword pre-filter +
> current risk state) lives in the Safety Resolver (G3-T10) — that
> is the right place to add a production test set covering all
> four levels end to end.

## Mapping rule

For each input, the mock provider scans the DEMO keyword table
(from most severe to least severe) and returns the first match. The
exact keyword lists are in
`backend/src/main/java/com/mindbridge/safety/classifier/provider/impl/MockRiskClassifierProvider.java`.

## Test cases

### Level 1 — Normal

| # | Input text                       | Expected scenario | Expected reason codes | Note |
|---|----------------------------------|-------------------|-----------------------|------|
| 1 | `hôm nay tôi thấy tốt`           | `LEVEL_1_NORMAL`   | `[]`                  | Default — no risk signal. |
| 2 | `vui quá`                        | `LEVEL_1_NORMAL`   | `[]`                  | "vui" matches L1 keywords. |
| 3 | `hôm nay trời đẹp`              | `LEVEL_1_NORMAL`   | `[]`                  | No keyword match → L1 fallback. |

### Level 2 — Follow-up

| # | Input text                  | Expected scenario    | Expected reason codes                            |
|---|-----------------------------|----------------------|--------------------------------------------------|
| 4 | `tôi lo lắng quá`           | `LEVEL_2_FOLLOWUP`   | `["DISTRESS_SIGNAL_DEMO", "SLEEP_DISRUPTION_DEMO"]` |
| 5 | `mất ngủ mấy đêm`          | `LEVEL_2_FOLLOWUP`   | `["DISTRESS_SIGNAL_DEMO", "SLEEP_DISRUPTION_DEMO"]` |
| 6 | `áp lực công việc nhiều`    | `LEVEL_2_FOLLOWUP`   | `["DISTRESS_SIGNAL_DEMO", "SLEEP_DISRUPTION_DEMO"]` |

### Level 3 — High Risk

| #  | Input text                       | Expected scenario     | Expected reason codes                       |
|----|----------------------------------|-----------------------|--------------------------------------------|
| 7  | `tôi mệt mỏi quá`               | `LEVEL_3_HIGH_RISK`   | `["BURNOUT_DEMO", "HOPELESSNESS_DEMO"]`    |
| 8  | `tôi kiệt sức rồi`              | `LEVEL_3_HIGH_RISK`   | `["BURNOUT_DEMO", "HOPELESSNESS_DEMO"]`    |
| 9  | `tôi không còn hy vọng`         | `LEVEL_3_HIGH_RISK`   | `["BURNOUT_DEMO", "HOPELESSNESS_DEMO"]`    |
| 10 | `tôi không chịu nổi nữa`        | `LEVEL_3_HIGH_RISK`   | `["BURNOUT_DEMO", "HOPELESSNESS_DEMO"]`    |

### Level 4 — Emergency

| #  | Input text                                | Expected scenario     | Expected reason codes             |
|----|-------------------------------------------|-----------------------|-----------------------------------|
| 11 | `tôi tuyệt vọng quá`                      | `LEVEL_4_EMERGENCY`   | `["SELF_HARM_RISK_DEMO"]`         |
| 12 | `tôi không muốn sống nữa`                 | `LEVEL_4_EMERGENCY`   | `["SELF_HARM_RISK_DEMO"]`         |
| 13 | `tôi muốn kết thúc tất cả`                | `LEVEL_4_EMERGENCY`   | `["SELF_HARM_RISK_DEMO"]`         |

### Failure scenarios (sentinel forced)

| #  | Input text          | Expected exception                            |
|----|---------------------|-----------------------------------------------|
| 14 | `force:TIMEOUT`     | `RiskClassifierTimeoutException`              |
| 15 | `force:MALFORMED_JSON` | `InvalidRiskClassifierOutputException`     |

### Severity tie-breaking

| #  | Input text                                          | Expected scenario     | Note |
|----|-----------------------------------------------------|-----------------------|------|
| 16 | `tôi lo lắng và không muốn sống`                   | `LEVEL_4_EMERGENCY`   | L4 wins over L2 — most-severe keyword wins. |

## Why this is `_DEMO` only

- The keyword lists in the mock provider are intentionally minimal —
  they exist to wire the Safety Resolver end-to-end, not to model
  clinical risk.
- The reason codes (`DISTRESS_SIGNAL_DEMO`, `BURNOUT_DEMO`, ...) are
  illustrative — `docs/04_SAFETY_AND_CBT_RULES.md` §7 only gives
  `DISTRESS_SIGNAL` and `SLEEP_DISRUPTION` as examples and §1
  forbids inventing clinical categories.
- The confidence values (`0.78`, `0.85`, `0.95`) are arbitrary
  constants chosen so tests can assert exact ranges; they are NOT
  calibrated probabilities.

## How this is verified

`MockRiskClassifierProviderTest` (unit) covers cases 1–13 and 14–16
via keyword + force-scenario + sentinel tests.
`MockRiskClassifierProviderIntegrationTest` exercises the wired
bean against all four levels + the two sentinels.

## Future production test set

When the real risk-classifier prompt + taxonomy is approved, a new
file (e.g. `risk_classifier_test_cases_production.md`) should be
added in this folder. It should:

1. Cover all four risk levels end to end through the Safety
   Resolver (G3-T10) — not just through the classifier provider.
2. Use only expert-approved reason codes and keywords.
3. Cover the keyword pre-filter (G3-T08) interaction.
4. Be reviewed and signed off by an expert before being added to
   CI.
