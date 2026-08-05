package com.mindbridge.behavior.feature.engagement.dto;

/**
 * G4-T08 DTO carrying a single dominant-topic summary row.
 *
 * <p>The record holds exactly three fields:
 * <ul>
 *   <li>{@code topic}: the raw enum {@code name()} from
 *       {@link com.mindbridge.analysis.provider.Topic}, e.g.
 *       {@code "WORK_STRESS"} (per Phase 1 Q6: no masking - schema G3-T02
 *       guarantees topics are non-diagnostic). Maximum length = 40 chars
 *       to match {@code chat_analysis_results.topic VARCHAR(40)} (V16).</li>
 *   <li>{@code frequency}: integer count of distinct ACTIVE
 *       {@code chat_analysis_results} rows with this topic in the
 *       time window. Rerun-aware (only ACTIVE rows count, never
 *       SUPERSEDED).</li>
 *   <li>{@code share}: the topic's fraction of total qualifying topic
 *       occurrences in the window, rounded to 4 decimals HALF_UP. Always
 *       in [0.0, 1.0]. Sum of {@code share} over one result list is
 *       approximately 1.0; rounding may make it 0.9999 or 1.0001 by &le; 1 ULP.
 *       Computed only across topics that pass the confidence floor - this
 *       matches the T03 / T04 invariant that null/filtered data is
 *       excluded from shares.</li>
 * </ul>
 *
 * <p>Equals / hashCode / toString are the record defaults.
 */
public record TopicFrequency(String topic, long frequency, double share) {

    public TopicFrequency {
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic must not be null or blank");
        }
        if (topic.length() > 40) {
            throw new IllegalArgumentException(
                    "topic exceeds 40 chars (chat_analysis_results.topic VARCHAR(40)): " + topic);
        }
        if (frequency < 0L) {
            throw new IllegalArgumentException("frequency must be >= 0; got " + frequency);
        }
        if (Double.isNaN(share) || Double.isInfinite(share) || share < 0.0 || share > 1.0) {
            throw new IllegalArgumentException("share must be in [0.0, 1.0]; got " + share);
        }
    }
}
