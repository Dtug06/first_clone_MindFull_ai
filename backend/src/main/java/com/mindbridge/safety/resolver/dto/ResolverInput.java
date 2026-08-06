package com.mindbridge.safety.resolver.dto;

import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.dto.PreFilterResult;
import com.mindbridge.safety.resolver.RiskStateSourceType;
import java.util.UUID;

/**
 * Input to {@code SafetyResolverService.resolve(...)}. Bundles the two
 * safety signals (keyword pre-filter from G3-T08 and LLM risk
 * classifier from G3-T09) and the audit metadata
 * ({@code userId}, {@code sourceType}, {@code sourceId}).
 *
 * <p>Both signals are nullable. When a signal is null, the resolver
 * treats it as "no signal" — which means {@code risk level 1} for the
 * keyword side and {@code risk level 1} for the classifier side. This
 * matches the documented behaviour in
 * docs/04_SAFETY_AND_CBT_RULES.md §6 ("Keyword chỉ dùng để phát
 * hiện tín hiệu ban đầu") and §7 ("LLM Safety Output phải là
 * Structured JSON ... JSON sai schema không được coi là thành
 * công").
 *
 * <p>The {@code sourceType} describes which pipeline produced the
 * signal and is persisted verbatim on the resulting history row. For
 * G3-T10 only {@link RiskStateSourceType#KEYWORD_PRE_FILTER} and
 * {@link RiskStateSourceType#LLM_CLASSIFIER} are expected;
 * {@link RiskStateSourceType#MANUAL_REVIEW} is reserved for the
 * Expert Review task (G3-T13).
 *
 * @param userId          the user whose risk is being resolved. Never
 *                        trusted for identity here — ownership checks
 *                        happen at the consumer (the chat or
 *                        check-in service that wires the resolver in).
 * @param sourceType      which safety input source triggered this
 *                        resolution.
 * @param sourceId        optional id of the originating row (e.g. the
 *                        conversation message id, or the daily answer
 *                        id, or null when the caller has no source
 *                        row).
 * @param preFilterResult the G3-T08 result. Null is treated as "no
 *                        signal" (risk level 1).
 * @param classifierOutput the G3-T09 result. Null is treated as "no
 *                        signal" (risk level 1).
 */
public record ResolverInput(
        UUID userId,
        RiskStateSourceType sourceType,
        UUID sourceId,
        PreFilterResult preFilterResult,
        RiskClassifierOutput classifierOutput
) {
    public ResolverInput {
        if (userId == null) {
            throw new IllegalArgumentException("userId must not be null");
        }
        if (sourceType == null) {
            throw new IllegalArgumentException("sourceType must not be null");
        }
    }
}
