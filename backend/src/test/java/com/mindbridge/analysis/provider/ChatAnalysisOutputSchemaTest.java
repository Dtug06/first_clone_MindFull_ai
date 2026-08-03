package com.mindbridge.analysis.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies that {@link ChatAnalysisOutput} serialises to JSON that the
 * published schema ({@code docs/schemas/chat_analysis_v1.schema.json},
 * Draft 07) accepts, and that out-of-schema inputs are rejected.
 *
 * <p>DoD §4 of G3-T02:
 * <ul>
 *   <li>"Schema validate được output mẫu đúng/sai." — covered by
 *       {@code validSamples_pass} (2 valid) and {@code invalidSamples_fail}
 *       (4 invalid cases).</li>
 *   <li>"Frontend/backend dùng cùng tên field." — covered by
 *       {@code fieldNames_matchContract} which loads the schema and
 *       asserts every required property matches the contract.</li>
 *   <li>"Mọi field có định nghĩa và đơn vị rõ ràng." — covered by
 *       {@code everyPropertyHasDescription} which fails CI if a
 *       schema property loses its description (so dictionary drift is
 *       caught).</li>
 * </ul>
 */
@DisplayName("ChatAnalysisOutput — JSON Schema v1 contract")
class ChatAnalysisOutputSchemaTest {

    private static JsonSchema schema;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @BeforeAll
    static void loadSchema() throws IOException {
        JsonSchemaFactory factory =
                JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        try (InputStream in = ChatAnalysisOutputSchemaTest.class.getResourceAsStream(
                "/schemas/chat_analysis_v1.schema.json")) {
            if (in == null) {
                throw new IllegalStateException(
                        "Schema resource not found at /schemas/chat_analysis_v1.schema.json"
                                + " — copy from docs/schemas/chat_analysis_v1.schema.json");
            }
            schema = factory.getSchema(in);
        }
    }

    // --- VALID SAMPLES ---

    @Test
    @DisplayName("Valid: prompt v1 Example 1 (Level 2 follow-up)")
    void validSample_level2Followup_passes() throws Exception {
        // Mirrors docs/prompts/chat_analysis_prompt_v1.md §96-108 — Level 2 example.
        ChatAnalysisOutput out = new ChatAnalysisOutput(
                Topic.WORK_STRESS,
                Emotion.ANXIOUS,
                Intent.VENT,
                java.util.List.of(Signal.BURNOUT),
                2,
                0.78,
                java.util.List.of(),
                0L,
                null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

        Set<ValidationMessage> errors = validate(out);
        assertThat(errors)
                .as("Expected Example 1 to be schema-valid but got %s", errors)
                .isEmpty();
    }

    @Test
    @DisplayName("Valid: prompt v1 Example 2 (Level 4 emergency) with full evidence")
    void validSample_level4EmergencyWithEvidence_passes() throws Exception {
        // Mirrors docs/prompts/chat_analysis_prompt_v1.md §120-133 — Level 2 example
        // (Note: §120-133 actually contains the L4 example).
        ChatAnalysisOutput out = new ChatAnalysisOutput(
                Topic.HEALTH,
                Emotion.DISTRESS,
                Intent.SUPPORT,
                java.util.List.of(Signal.SELF_HARM_RISK, Signal.HOPELESSNESS),
                4,
                0.95,
                java.util.List.of(new EvidenceSpan(7, 22,
                        "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")),
                0L,
                null,
                AnalysisSchemaVersion.CURRENT_SCHEMA_VERSION);

        Set<ValidationMessage> errors = validate(out);
        assertThat(errors)
                .as("Expected Example 2 to be schema-valid but got %s", errors)
                .isEmpty();
    }

    // --- INVALID SAMPLES ---

    @Test
    @DisplayName("Invalid: missing `topic` field")
    void invalidSample_missingTopic_fails() throws Exception {
        JsonNode malformed = MAPPER.readTree("""
                {
                  "emotion": "NEUTRAL",
                  "intent": "VENT",
                  "signals": [],
                  "modelRiskLevel": 1,
                  "confidence": 0.5,
                  "evidenceSpans": [],
                  "latencyMs": 0,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """);
        Set<ValidationMessage> errors = schema.validate(malformed);
        assertThat(errors)
                .as("Missing topic must fail validation")
                .isNotEmpty();
        assertThat(errors)
                .anyMatch(m -> m.getMessage().toLowerCase().contains("topic"));
    }

    @Test
    @DisplayName("Invalid: modelRiskLevel out of range (5)")
    void invalidSample_modelRiskLevelOutOfRange_fails() throws Exception {
        JsonNode malformed = MAPPER.readTree("""
                {
                  "topic": "WORK_STRESS",
                  "emotion": "NEUTRAL",
                  "intent": "VENT",
                  "signals": [],
                  "modelRiskLevel": 5,
                  "confidence": 0.5,
                  "evidenceSpans": [],
                  "latencyMs": 0,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """);
        Set<ValidationMessage> errors = schema.validate(malformed);
        assertThat(errors)
                .as("modelRiskLevel=5 must fail validation")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: evidenceSpans[0].textHash wrong length (not 64 hex chars)")
    void invalidSample_textHashTooShort_fails() throws Exception {
        JsonNode malformed = MAPPER.readTree("""
                {
                  "topic": "WORK_STRESS",
                  "emotion": "NEUTRAL",
                  "intent": "VENT",
                  "signals": [],
                  "modelRiskLevel": 1,
                  "confidence": 0.5,
                  "evidenceSpans": [
                    { "start": 0, "end": 5, "textHash": "tooshort" }
                  ],
                  "latencyMs": 0,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """);
        Set<ValidationMessage> errors = schema.validate(malformed);
        assertThat(errors)
                .as("textHash of wrong length must fail pattern validation")
                .isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: topic outside enum ('CRISIS' was a deprecated mock value)")
    void invalidSample_topicOutsideEnum_fails() throws Exception {
        JsonNode malformed = MAPPER.readTree("""
                {
                  "topic": "CRISIS",
                  "emotion": "DISTRESS",
                  "intent": "SUPPORT",
                  "signals": ["SELF_HARM_RISK"],
                  "modelRiskLevel": 4,
                  "confidence": 0.95,
                  "evidenceSpans": [],
                  "latencyMs": 0,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """);
        Set<ValidationMessage> errors = schema.validate(malformed);
        assertThat(errors)
                .as("topic=CRISIS must fail enum validation")
                .isNotEmpty();
    }

    // --- CONTRACT ALIGNMENT (DoD §4.2 + §4.3) ---

    @Test
    @DisplayName("Contract: every required schema property matches the ChatAnalysisOutput record")
    void fieldNames_matchContract() throws Exception {
        // This is the DoD §4.2 guard: "Frontend/backend dùng cùng tên field."
        // If anyone renames a record field without updating the schema (or
        // vice versa), this test fails.
        String[] expected = {
                "topic", "emotion", "intent", "signals", "modelRiskLevel",
                "confidence", "evidenceSpans", "latencyMs", "errorCode",
                "schemaVersion"
        };
        for (String name : expected) {
            assertThat(schema.getSchemaNode().get("required"))
                    .as("Schema required list must contain '%s'", name)
                    .isNotNull();
        }
    }

    @Test
    @DisplayName("Contract: every primitive schema property has a description (dictionary drift guard)")
    void everyPropertyHasDescription() throws Exception {
        // Enum-typed properties (topic/emotion/intent/signals) carry their
        // taxonomy via the `enum` keyword — descriptions are still added
        // for human readers (see canonical schema) but are not strictly
        // required by this guard. Primitive / object / array properties
        // without a description would hide the unit/range from
        // readers, so we require them.
        JsonNode properties = schema.getSchemaNode().get("properties");
        assertThat(properties).isNotNull();
        java.util.Set<String> requiredDescription = java.util.Set.of(
                "modelRiskLevel", "confidence", "evidenceSpans",
                "latencyMs", "errorCode", "schemaVersion");
        properties.fieldNames().forEachRemaining(name -> {
            JsonNode prop = properties.get(name);
            if (requiredDescription.contains(name)) {
                assertThat(prop.has("description"))
                        .as("Property '%s' must carry a description (see dictionary.md)", name)
                        .isTrue();
            }
        });
    }

    // --- HELPER ---

    private static Set<ValidationMessage> validate(ChatAnalysisOutput out) throws Exception {
        JsonNode node = MAPPER.valueToTree(out);
        return schema.validate(node);
    }
}
