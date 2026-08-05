package com.mindbridge.behavior.feature.engagement.config;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * G4-T08 configuration value object for the engagement + dominant-topics service.
 *
 * <p>This config bundles three orthogonal knobs that all map to
 * {@code TODO_EXPERT_REVIEW} / unapproved placeholders in
 * {@code docs/analysis/FEATURE_DICTIONARY_v1.md §10.1} +
 * {@code docs/tasks/G4/Pre_G4_Implementation_Decisions_and_Feature_Contract.md §13.1}:
 * <ul>
 *   <li>{@code ENGAGEMENT_COMPONENT_WEIGHTS} — MVP is unweighted (binary
 *       presence/activity per source per day). All four component weights
 *       are forced equal (= 1) by the {@code v1-unweighted} formula; the
 *       final score is the integer count of distinct active sources in
 *       {@code [0, 3]}. Weights are NOT held here yet because the field is
 *       not approved. When experts approve weights, this config will gain
 *       {@code chatMessageWeight}, {@code chatSessionWeight},
 *       {@code checkinWeight}, {@code exerciseWeight} fields.</li>
 *   <li>{@code MIN_TOPIC_CONFIDENCE} — minimum confidence for a
 *       {@code chat_analysis_results} row to count toward dominant topics.
 *       MVP defaults to {@code null} (= no floor), matching G4-T04
 *       {@link com.mindbridge.behavior.feature.config.FeatureConfig}'s
 *       default. When experts approve a floor, callers inject it here AND
 *       must inject the same value into {@code FeatureConfig}.</li>
 *   <li>{@code TOPIC_TOP_N} — maximum number of topics returned per window.
 *       MVP default is {@code 3}, per the user-confirmed decision in
 *       {@code docs/tasks/G4/G4-T08-engagement-and-dominant-topics.md}
 *       Phase 1 Q3 (2026-08-05). There is no DB table yet for an
 *       {@code approved_by}-audited numeric threshold (DB-MVP §12 defers
 *       {@code clinical_thresholds}), so the value is held in this
 *       value-object and stamped into the calculation version string
 *       {@link #CALCULATION_VERSION} for audit traceability.</li>
 * </ul>
 *
 * <p><b>Fail-fast.</b> {@link #defaults()} returns {@code null} for the
 * two expert-review knobs so any caller that forgets to inject a value
 * blows up at runtime with a clear message rather than silently using a
 * fabricated threshold (rule 00: "khong tu bia clinical threshold").
 *
 * <p><b>Thread-safety.</b> Immutable; safe to share as a Spring bean.
 */
public final class EngagementConfig {

    /** Stable identifier of the calculation algorithm (for audit/UI display). */
    public static final String CALCULATION_VERSION = "engagement_v1_unweighted_top_n_3";

    /** MVP default for the maximum number of topics returned per window. */
    public static final int DEFAULT_TOPIC_TOP_N = 3;

    private final BigDecimal minTopicConfidence;
    private final int maxTopicCount;

    private EngagementConfig(BigDecimal minTopicConfidence, int maxTopicCount) {
        this.minTopicConfidence = minTopicConfidence;
        this.maxTopicCount = maxTopicCount;
    }

    /**
     * Build a config explicitly. Both values are optional in MVP:
     *
     * @param minTopicConfidence floor for chat_analysis_results.confidence;
     *                           {@code null} means no floor (matches
     *                           G4-T04 default); if non-null must be in [0, 1].
     * @param maxTopicCount      how many topics to return per window;
     *                           must be &gt;= 1 and &lt;= the closed
     *                           taxonomy size (7, see {@link com.mindbridge.analysis.provider.Topic}).
     */
    public static EngagementConfig of(BigDecimal minTopicConfidence, int maxTopicCount) {
        validateConfidence(minTopicConfidence);
        validateMaxTopicCount(maxTopicCount);
        return new EngagementConfig(minTopicConfidence, maxTopicCount);
    }

    /**
     * MVP defaults: no confidence floor, {@value #DEFAULT_TOPIC_TOP_N} topics.
     * Suitable for tests that do NOT exercise threshold-dependent behaviour.
     */
    public static EngagementConfig defaults() {
        return new EngagementConfig(null, DEFAULT_TOPIC_TOP_N);
    }

    private static void validateConfidence(BigDecimal value) {
        if (value != null && (value.signum() < 0 || value.compareTo(BigDecimal.ONE) > 0)) {
            throw new IllegalArgumentException(
                    "minTopicConfidence must be in [0, 1] or null; got " + value);
        }
    }

    private static void validateMaxTopicCount(int value) {
        if (value < 1 || value > 7) {
            throw new IllegalArgumentException(
                    "maxTopicCount must be in [1, 7] (closed taxonomy size); got " + value);
        }
    }

    public BigDecimal getMinTopicConfidence() {
        return minTopicConfidence;
    }

    public int getMaxTopicCount() {
        return maxTopicCount;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof EngagementConfig that)) return false;
        return this.maxTopicCount == that.maxTopicCount
                && Objects.equals(this.minTopicConfidence, that.minTopicConfidence);
    }

    @Override
    public int hashCode() {
        return Objects.hash(minTopicConfidence, maxTopicCount);
    }

    @Override
    public String toString() {
        return "EngagementConfig{minTopicConfidence=" + minTopicConfidence
                + ", maxTopicCount=" + maxTopicCount
                + ", calculationVersion=" + CALCULATION_VERSION + "}";
    }
}
