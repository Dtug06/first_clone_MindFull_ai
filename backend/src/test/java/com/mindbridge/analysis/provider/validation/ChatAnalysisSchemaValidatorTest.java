package com.mindbridge.analysis.provider.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Runtime validator test for G3-T07. Mirrors the existing
 * {@code ChatAnalysisOutputSchemaTest} but exercises the
 * compile-scope {@link ChatAnalysisSchemaValidator} class that
 * production code uses (not the test-only manual schema loading).
 *
 * <p>DoD Â§4.1 of G3-T07:
 * <ul>
 *   <li>JSON thiáº¿u field khÃ´ng Ä‘Æ°á»£c lÆ°u nhÆ° result thÃ nh cÃ´ng â€” covered
 *       by {@code missingTopic_fails}, {@code modelRiskLevelOutOfRange_fails},
 *       {@code textHashTooShort_fails}, {@code topicOutsideEnum_fails}.</li>
 *   <li>Valid L2 + L4 payloads pass validation â€” covered by
 *       {@code validLevel2Followup_passes} and
 *       {@code validLevel4Emergency_passes}.</li>
 * </ul>
 */
@DisplayName("ChatAnalysisSchemaValidator â€” runtime JSON Schema v1")
class ChatAnalysisSchemaValidatorTest {

    private ChatAnalysisSchemaValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ChatAnalysisSchemaValidator();
    }

    @Test
    @DisplayName("Valid: Level 2 follow-up JSON passes")
    void validLevel2Followup_passes() {
        String json = """
                {
                  "topic": "WORK_STRESS",
                  "emotion": "ANXIOUS",
                  "intent": "VENT",
                  "signals": ["BURNOUT"],
                  "modelRiskLevel": 2,
                  "confidence": 0.78,
                  "evidenceSpans": [],
                  "latencyMs": 25,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("Valid L2 payload must have no errors but had %s", errors).isEmpty();
    }

    @Test
    @DisplayName("Valid: Level 4 emergency JSON with evidence passes")
    void validLevel4Emergency_passes() {
        String json = """
                {
                  "topic": "HEALTH",
                  "emotion": "DISTRESS",
                  "intent": "SUPPORT",
                  "signals": ["SELF_HARM_RISK", "HOPELESSNESS"],
                  "modelRiskLevel": 4,
                  "confidence": 0.95,
                  "evidenceSpans": [
                    {"start": 7, "end": 22,
                     "textHash": "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}
                  ],
                  "latencyMs": 40,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("Valid L4 payload must have no errors but had %s", errors).isEmpty();
    }

    @Test
    @DisplayName("Invalid: missing topic field â†’ empty error set non-empty")
    void missingTopic_fails() {
        String json = """
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
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("Missing topic must fail validation").isNotEmpty();
        assertThat(errors.toString().toLowerCase()).contains("topic");
    }

    @Test
    @DisplayName("Invalid: modelRiskLevel out of range (5) â†’ fails")
    void modelRiskLevelOutOfRange_fails() {
        String json = """
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
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("modelRiskLevel=5 must fail validation").isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: textHash of wrong length â†’ fails")
    void textHashTooShort_fails() {
        String json = """
                {
                  "topic": "WORK_STRESS",
                  "emotion": "NEUTRAL",
                  "intent": "VENT",
                  "signals": [],
                  "modelRiskLevel": 1,
                  "confidence": 0.5,
                  "evidenceSpans": [
                    {"start": 0, "end": 5, "textHash": "tooshort"}
                  ],
                  "latencyMs": 0,
                  "errorCode": null,
                  "schemaVersion": "V1"
                }
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("textHash of wrong length must fail pattern validation").isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: topic outside enum (CRISIS) â†’ fails")
    void topicOutsideEnum_fails() {
        String json = """
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
                """;
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("topic=CRISIS must fail enum validation").isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: malformed JSON (not parseable) â†’ fails")
    void malformedJson_fails() {
        String json = "{ not valid json at all ";
        Set<String> errors = validator.validate(json);
        assertThat(errors).as("Malformed JSON must fail").isNotEmpty();
    }

    @Test
    @DisplayName("Invalid: null payload â†’ fails")
    void nullPayload_fails() {
        Set<String> errors = validator.validate(null);
        assertThat(errors).isNotEmpty();
    }
}
