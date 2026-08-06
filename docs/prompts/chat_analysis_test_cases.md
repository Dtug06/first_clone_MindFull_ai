# Chat Analysis — Test Cases (DEMO_ONLY)

> **Status: DEMO_ONLY** — for `MockChatAnalysisProvider` (G3-T01) and
> the v1 chat analysis prompt (G3-T03).
>
> These cases describe the expected behaviour of the chat analysis
> pipeline. They are NOT clinical ground truth — they verify that
> the mock provider's keyword→scenario mapping stays stable across
> prompt and code changes, and that the prompt design produces
> the right output shape.
>
> Production coverage should include expert-reviewed cases and a
> real-LLM evaluation harness (added when G3-T06 ships). This file
> drives only the deterministic mock layer.

## How to read this table

For each case, the `Mock provider expected scenario` is the level
the deterministic mock must return today. The `Prompt-expected level`
column is the level the LLM prompt is designed to produce when
applied to the same input — these two should match for every case
once the real provider (G3-T06) is wired up.

If the columns diverge on a real provider, that is a prompt
regression: the prompt is letting the model drift away from the
mock baseline. The test harness for the real provider should
assert on this divergence.

## Level 1 — Normal

| # | Input text                       | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|----------------------------------|---------------------|--------------------------------|------|
| 1 | `hôm nay tôi thấy tốt`           | `LEVEL_1_NORMAL`    | 1                              | "vui" or "tốt" keyword, default. |
| 2 | `hôm nay trời đẹp`              | `LEVEL_1_NORMAL`    | 1                              | No keyword → fallback. |
| 3 | `vui quá`                        | `LEVEL_1_NORMAL`    | 1                              | "vui" keyword. |

## Level 1 — Ambiguous / low confidence

| # | Input text                                | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|-------------------------------------------|---------------------|--------------------------------|------|
| 4 | `tôi không biết nữa`                       | `LEVEL_1_NORMAL`    | 1 (confidence ~0.3–0.5)        | No keyword → L1. The prompt should produce low confidence but stay Level 1 — there is no explicit risk signal. |
| 5 | `thôi, kệ`                                | `LEVEL_1_NORMAL`    | 1                              | Short dismissive message. |

## Level 2 — Follow-up

| # | Input text                                  | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|---------------------------------------------|---------------------|--------------------------------|------|
| 6 | `Hôm nay deadline dí quá, tôi lo lắng.`     | `LEVEL_2_FOLLOWUP`  | 2                              | "lo lắng" keyword; expected `signals: ["BURNOUT"]`. |
| 7 | `áp lực công việc nhiều quá`                | `LEVEL_2_FOLLOWUP`  | 2                              | "áp lực" keyword. |
| 8 | `Dạo này áp lực deadline nhiều quá`                       | `LEVEL_2_FOLLOWUP`  | 2                              | "áp lực" keyword; expected `signals: ["BURNOUT"]` once v6+. |

## Level 3 — High Risk

| # | Input text                       | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|----------------------------------|---------------------|--------------------------------|------|
| 9 | `tôi mệt mỏi quá`              | `LEVEL_3_HIGH_RISK` | 3                              | "mệt mỏi quá" keyword; expected `signals: ["BURNOUT"]`. |
| 10 | `tôi kiệt sức rồi`              | `LEVEL_3_HIGH_RISK` | 3                              | "kiệt sức" keyword. |
| 11 | `tôi không chịu nổi nữa`       | `LEVEL_3_HIGH_RISK` | 3                              | "không chịu nổi" keyword. |
| 12 | `tôi burnout rồi`               | `LEVEL_3_HIGH_RISK` | 3                              | "burnout" keyword. |

## Level 4 — Emergency

| # | Input text                       | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|----------------------------------|---------------------|--------------------------------|------|
| 13 | `tôi không muốn sống nữa`       | `LEVEL_4_EMERGENCY` | 4                              | "không muốn sống" keyword; expected `signals: ["SELF_HARM_RISK","HOPELESSNESS"]`. |
| 14 | `tôi tuyệt vọng quá`            | `LEVEL_4_EMERGENCY` | 4                              | "tuyệt vọng" keyword. |
| 15 | `tôi muốn kết thúc tất cả`      | `LEVEL_4_EMERGENCY` | 4                              | "kết thúc tất cả" keyword. |

## Severity tie-breaking

| # | Input text                              | Mock scenario       | Prompt-expected modelRiskLevel | Note |
|---|-----------------------------------------|---------------------|--------------------------------|------|
| 16 | `tôi lo lắng và không muốn sống`       | `LEVEL_4_EMERGENCY` | 4                              | L4 wins — most severe keyword wins. |

## Failure scenarios

| # | Input text            | Mock scenario     | Prompt-expected errorCode | Note |
|---|-----------------------|-------------------|---------------------------|------|
| 17 | `force:TIMEOUT`       | `TIMEOUT`         | n/a (provider behaviour)  | Mock throws `ProviderTimeoutException`. |
| 18 | `force:MALFORMED_JSON` | `MALFORMED_JSON` | n/a                       | Mock throws `InvalidAnalysisOutputException`. |

## How this is verified

`MockChatAnalysisProviderTest` (unit) covers all 18 cases via
keyword + force-scenario + sentinel tests. The test file groups
tests by category (Keyword resolution / Sentinels / Force
override / Output shape / Edge cases).

`MockChatAnalysisProviderIntegrationTest` (integration) exercises
the wired bean against all six scenarios, ensuring the Spring
context picks up the new property + provider wiring.

## Future production test set

When the real LLM provider (G3-T06) is wired:

1. The cases in this file become the regression baseline — any
   divergence on a real provider is a prompt regression to fix in
   v2.
2. Add expert-reviewed production cases with curated reason codes
   and expected confidence ranges.
3. Add a real-LLM evaluation harness that runs the cases through
   the real provider and reports divergence from the mock baseline.
4. Add multi-language coverage once a non-Vietnamese locale is
   supported (T03 currently scopes the prompt to `vi-VN` only).
