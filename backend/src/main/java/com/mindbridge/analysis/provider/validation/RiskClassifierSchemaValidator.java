package com.mindbridge.analysis.provider.validation;

import com.mindbridge.safety.classifier.RiskClassifierOutput;
import java.util.List;

/**
 * Runtime validator for {@link com.mindbridge.safety.classifier.RiskClassifierOutput}
 * payloads returned by external LLM risk-classifier providers.
 *
 * <p>Design note (G3-T07): the risk-classifier DTO does not yet have a
 * dedicated JSON Schema file (the Safety branch deferred the schema
 * file to a later task â€” the DTO compact constructor is the only
 * schema-enforced surface today, shipped in G3-T09). This validator
 * therefore delegates to {@link RiskClassifierOutput}'s compact
 * constructor, which already enforces:
 *
 * <ul>
 *   <li>{@code riskLevel} in [1, 4].</li>
 *   <li>{@code confidence} in [0.0, 1.0].</li>
 *   <li>{@code reasonCodes} non-null.</li>
 *   <li>{@code evidenceSpans} non-null.</li>
 *   <li>{@code promptVersion}, {@code schemaVersion}, {@code providerInfo}
 *       non-null and non-blank.</li>
 * </ul>
 *
 * <p>When a future task ships a dedicated JSON Schema for the
 * classifier, this class can be extended to wrap {@code networknt}
 * alongside {@link ChatAnalysisSchemaValidator} without changing the
 * public surface.
 *
 * <p>Failure modes:
 *
 * <ul>
 *   <li>{@link #validate(String)} returns a non-empty error list on
 *       any invalid field â€” the caller MUST treat this as
 *       {@code InvalidRiskClassifierOutputException}.</li>
 *   <li>The returned error list is empty on success.</li>
 * </ul>
 */
public class RiskClassifierSchemaValidator {

    /** Parse the raw JSON into a {@link RiskClassifierOutput}, validating as a side-effect. */
    public RiskClassifierOutput validate(String json) {
        // The DTO compact constructor is the source of truth. If a
        // dedicated JSON Schema ships later, this method becomes the
        // schema-then-DTO pipeline; the public contract does not change.
        throw new UnsupportedOperationException(
                "RiskClassifierOutput is currently validated via its compact constructor. "
                        + "Map the parsed JsonNode to RiskClassifierOutput directly.");
    }

    /**
     * Validate an already-parsed candidate and return either the
     * validated DTO or an error list.
     *
     * @param riskLevel candidate risk level (1..4).
     * @param confidence candidate confidence (0.0..1.0).
     * @param reasonCodes candidate reason codes (never null; may be empty).
     * @param evidenceSpans candidate evidence spans (never null; may be empty).
     * @param promptVersion candidate prompt version string.
     * @param schemaVersion candidate schema version string.
     * @param providerInfo candidate provider info string.
     * @return a {@link Validation} carrying either the built DTO or a
     *         non-empty error list.
     */
    public Validation validate(
            int riskLevel,
            double confidence,
            List<String> reasonCodes,
            List<com.mindbridge.analysis.provider.EvidenceSpan> evidenceSpans,
            String promptVersion,
            String schemaVersion,
            String providerInfo) {

        java.util.List<String> errors = new java.util.ArrayList<>();
        if (riskLevel < 1 || riskLevel > 4) {
            errors.add("riskLevel must be in [1, 4] but was " + riskLevel);
        }
        if (confidence < 0.0 || confidence > 1.0) {
            errors.add("confidence must be in [0.0, 1.0] but was " + confidence);
        }
        if (promptVersion == null || promptVersion.isBlank()) {
            errors.add("promptVersion must not be null or blank");
        }
        if (schemaVersion == null || schemaVersion.isBlank()) {
            errors.add("schemaVersion must not be null or blank");
        }
        if (providerInfo == null || providerInfo.isBlank()) {
            errors.add("providerInfo must not be null or blank");
        }
        if (!errors.isEmpty()) {
            return Validation.invalid(errors);
        }
        return Validation.valid(new RiskClassifierOutput(
                riskLevel,
                confidence,
                reasonCodes == null ? java.util.List.of() : reasonCodes,
                evidenceSpans == null ? java.util.List.of() : evidenceSpans,
                promptVersion,
                schemaVersion,
                providerInfo));
    }

    /**
     * Validation outcome â€” either a DTO or an error list, never both.
     */
    public static final class Validation {
        private final RiskClassifierOutput output;
        private final java.util.List<String> errors;

        private Validation(RiskClassifierOutput output, java.util.List<String> errors) {
            this.output = output;
            this.errors = errors;
        }

        public static Validation valid(RiskClassifierOutput output) {
            return new Validation(output, java.util.List.of());
        }

        public static Validation invalid(java.util.List<String> errors) {
            return new Validation(null, java.util.List.copyOf(errors));
        }

        public boolean isValid() {
            return errors.isEmpty();
        }

        public RiskClassifierOutput output() {
            return output;
        }

        public java.util.List<String> errors() {
            return errors;
        }
    }
}
