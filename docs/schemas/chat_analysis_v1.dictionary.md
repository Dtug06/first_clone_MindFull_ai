# Chat Analysis Output — Field Dictionary (v1)

> **Status**: locked 2026-08-02 by G3-T02 (Phase 2 + Phase 3 review pending).
> **Single source of truth**: this file is referenced by both backend
> (`backend/src/main/java/com/mindbridge/analysis/provider/ChatAnalysisOutput.java`)
> and the future frontend consumer. Any change must be made here AND in
> the JSON Schema (`docs/schemas/chat_analysis_v1.schema.json`) AND in
> the Java enum — three places, one source.
>
> **Schema file**: [`chat_analysis_v1.schema.json`](./chat_analysis_v1.schema.json)
> (JSON Schema Draft 07).
>
> **Versioning policy** (G3-T02 Phase 1 decision A):
> - Adding or removing a **field** → bump `schemaVersion` (breaking
>   change) → create a new schema file.
> - Changing a **field type** → same as above.
> - Adding a new value to a **closed enum** (Topic/Emotion/Intent/Signal)
>   → NOT a breaking change → keep `schemaVersion`, update this file
>   and the JSON Schema only.
> - Renaming or removing an existing enum value → bump `schemaVersion`.

## Field table

| # | Field | Type | Required | Unit / Range | Enum | Description (VI) | Example | Since |
|---|---|---|---|---|---|---|---|---|
| 1 | `topic` | string | ✓ | enum | `WORK_STRESS \| RELATIONSHIP \| FAMILY \| HEALTH \| FINANCE \| SLEEP \| OTHER` | Chủ đề chính của tin nhắn. KHÔNG phải nhãn lâm sàng. | `"WORK_STRESS"` | V1 |
| 2 | `emotion` | string | ✓ | enum | `NEUTRAL \| HAPPY \| ANXIOUS \| SAD \| OVERWHELMED \| DISTRESS \| ANGRY` | Cảm xúc chủ đạo quan sát được. `DISTRESS` là tín hiệu safety-relevant. | `"ANXIOUS"` | V1 |
| 3 | `intent` | string | ✓ | enum | `VENT \| ADVICE \| INFO \| SUPPORT` | Người dùng dường như muốn gì từ cuộc trò chuyện. KHÔNG phải quyết định hành động cuối. | `"VENT"` | V1 |
| 4 | `signals` | string[] | ✓ | enum array, có thể rỗng | `FATIGUE \| SLEEP_DISRUPTION \| ISOLATION \| HOPELESSNESS \| BURNOUT \| SELF_HARM_RISK \| CONFLICT \| GRIEF \| OTHER` | Tag hành vi gắn vào tin nhắn. `SELF_HARM_RISK` và `HOPELESSNESS` là safety-relevant. | `["BURNOUT"]` | V1 |
| 5 | `modelRiskLevel` | integer | ✓ | 1..4 | — | Tín hiệu risk riêng của model. 1 = bình thường; 2 = theo dõi; 3 = cao (mở safety event); 4 = khẩn cấp (fixed response). KHÔNG phải risk cuối. | `2` | V1 |
| 6 | `confidence` | number | ✓ | [0.0, 1.0] | — | Độ tự tin của model. Tín hiệu thô, không phải xác suất hiệu chỉnh. | `0.78` | V1 |
| 7 | `evidenceSpans` | object[] | ✓ | có thể rỗng | — | Con trỏ vào message gốc (offset + SHA-256 hash). Dùng để kiểm tra offline mà không lưu raw text. | `[{start: 18, end: 26, textHash: "…"}]` | V1 |
| 8 | `latencyMs` | integer | ✓ | ≥ 0 | — | Thời gian thực tế provider chạy (ms). Model prompt ghi rõ emit `0`. Provider ghi đè khi chạy thật. | `123` | V1 |
| 9 | `errorCode` | string \| null | ✓ | null khi success | — | Mã lỗi ổn định cho output fail (e.g. `MALFORMED_JSON`). `null` khi thành công. | `null` | V1 |
| 10 | `schemaVersion` | string | ✓ | `const: "V1"` | — | Schema version. Luôn là `V1` trong bản này. | `"V1"` | V1 (added in G3-T02) |

## `evidenceSpans[i]` table

| # | Field | Type | Required | Range | Description (VI) | Example | Since |
|---|---|---|---|---|---|---|---|
| 7.1 | `start` | integer | ✓ | ≥ 0 | Offset ký tự inclusive (cùng quy ước với `String.substring`). | `18` | V1 |
| 7.2 | `end` | integer | ✓ | > start | Offset ký tự exclusive. | `26` | V1 |
| 7.3 | `textHash` | string | ✓ | SHA-256 hex (lowercase, 64 hex chars), regex `^[a-f0-9]{64}$` | Hash SHA-256 của substring `[start, end)`. KHÔNG BAO GIỜ chứa raw substring (xem `EvidenceSpan.java` JavaDoc + rule 30). | `"ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"` | V1 |

## Anti-diagnosis guard

Schema này cố tình **không chứa** bất kỳ trường nào mang tính chẩn đoán lâm sàng
(`diagnosis`, `disorder_type`, v.v.) theo:
- `docs/04_SAFETY_AND_CBT_RULES.md` §2 "Không phải công cụ chẩn đoán"
- `.cursor/rules/00-project-core.mdc` line 26 "Do not implement diagnosis"
- `.cursor/rules/00-project-core.mdc` line 27 "Do not claim clinical certainty"

Nếu tương lai cần mở rộng taxonomy, hãy tạo schema mới (v2) và giữ nguyên file
này làm lịch sử.

## Anti-PII guard

`evidenceSpans[].textHash` dùng SHA-256 hex thay vì raw substring:
1. Tránh duplicate dữ liệu nhạy cảm ở nhiều nơi (raw text đã có ở `conversation_messages`).
2. Theo rule 30 "Do not log unnecessary raw prompts or raw responses containing sensitive data".
3. Audit trail vẫn đảm bảo: ai cần verify có thể compute lại SHA-256 hex từ
   raw substring + so sánh với hash đã lưu.

## Cross-reference

| Spec / source | Mapping |
|---|---|
| `docs/prompts/chat_analysis_prompt_v1.md` §43-51 | Source of truth cho enum taxonomy (topic/emotion/intent/signals) |
| `docs/prompts/chat_analysis_prompt_v1.md` §64-65 | `latencyMs = 0`, `errorCode = null on success` |
| `docs/04_SAFETY_AND_CBT_RULES.md` §3 | Risk level 1..4 definitions |
| `docs/04_SAFETY_AND_CBT_RULES.md` §5 | "Phải phân biệt model_risk_level / rule_risk_level / final_risk_level" — schema này CHỈ chứa `modelRiskLevel` |
| `docs/04_SAFETY_AND_CBT_RULES.md` §7 | `reasonCodes[]` + `evidenceSpans[]` shape cho LLM safety output (taxonomy riêng — KHÔNG dùng cho chat analysis) |
| `docs/02_DATABASE_MVP.md` §5.2 `chat_analysis_results` columns | DB columns dùng **plural** (`topics`, `emotions`, `signals`) — khi persistence layer (G3-T04/T05) map từ schema v1 (singular) sang DB, sẽ wrap thành 1-element JSONB array. Schema v1 chỉ chứa **dominant** value/element của mỗi loại. |
| `.cursor/rules/30-database-ai-safety.mdc` AI Rules | "Store: ... schema version" — `schemaVersion` field satisfies this |
| `.cursor/rules/30-database-ai-safety.mdc` Safety Rules | "Do not silently downgrade a model risk signal" — `modelRiskLevel` chỉ là một input vào Safety Resolver, không bị ghi đè trong schema này |

## Adding a new enum value (non-breaking)

Ví dụ: thêm giá trị `WORKPLACE_CONFLICT` vào `Signal`.

1. Thêm constant vào enum Java:
   ```java
   public enum Signal {
       ...
       WORKPLACE_CONFLICT,
       OTHER;
   }
   ```

2. Thêm vào JSON Schema (mục `signals.items.enum`):
   ```json
   "enum": [
     "FATIGUE", "SLEEP_DISRUPTION", "ISOLATION", "HOPELESSNESS",
     "BURNOUT", "SELF_HARM_RISK", "CONFLICT", "GRIEF", "OTHER",
     "WORKPLACE_CONFLICT"
   ]
   ```

3. Thêm hàng vào bảng trong file này (mục `signals`).

4. **KHÔNG bump `schemaVersion`** — đây là thay đổi không phá vỡ (existing
   consumers chỉ thấy thêm option, không có option nào bị mất).

5. KHÔNG tạo file schema mới. KHÔNG bump `chat_analysis_prompt_vX.md`
   (prompt v1 cố định — extension taxonomy yêu cầu tạo `chat_analysis_prompt_v2.md`
   riêng; G3-T03 quyết định khi nào cần bump).

## Removing an enum value (breaking)

Ví dụ: bỏ `GRIEF` khỏi `Signal`.

1. **DỪNG triển khai** (rule 00 line 33 "When uncertain, stop and report").
2. Báo cáo conflict giữa spec và breaking change.
3. Chờ phê duyệt. Sau khi approve:
   - Tạo `chat_analysis_v2.schema.json` với enum đã bỏ.
   - Bump `AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION = "V2"`.
   - Tạo `chat_analysis_v2.dictionary.md`.
   - Giữ nguyên file v1 làm lịch sử (audit trail).
4. **KHÔNG sửa file v1** — đã frozen theo rule 30 "Never edit a migration that has been merged or applied" (analogy cho schema).
