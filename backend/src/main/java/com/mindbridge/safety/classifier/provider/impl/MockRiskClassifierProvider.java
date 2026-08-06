package com.mindbridge.safety.classifier.provider.impl;

import com.mindbridge.safety.classifier.RiskClassifierInput;
import com.mindbridge.safety.classifier.RiskClassifierMockScenario;
import com.mindbridge.safety.classifier.RiskClassifierOutput;
import com.mindbridge.safety.classifier.RiskClassifierProvider;
import com.mindbridge.safety.classifier.exception.InvalidRiskClassifierOutputException;
import com.mindbridge.safety.classifier.exception.RiskClassifierTimeoutException;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deterministic, offline {@link RiskClassifierProvider} used in local
 * and test profiles. It maps each input to one of six
 * {@link RiskClassifierMockScenario}s based on a small set of
 * {@code DEMO_ONLY} keywords and optional sentinels, and produces a
 * fixed {@link RiskClassifierOutput}.
 *
 * <p>This implementation does not call any external service. It is the
 * MVP substitute for the real LLM risk classifier so the rest of the
 * Safety pipeline (Safety Resolver G3-T10, Safety Event G3-T11) can be
 * built and tested end-to-end without a network or an API key.
 *
 * <p><b>Keyword scope (intentionally minimal for G3-T09):</b> the
 * keyword table below is {@code DEMO_ONLY} and uses labels suffixed
 * {@code _DEMO} to make it obvious in any persisted row that these are
 * placeholder reason codes, not clinical categories. The real reason
 * code taxonomy is an expert-review item per
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} §7 and §1 ("Cursor không
 * được tự đặt threshold hoặc tự thay đổi các quy tắc").
 *
 * <p>Production keyword matching is the responsibility of the safety
 * rule engine (G3-T08 {@code SafetyPreFilterService}); this mock
 * provider's keyword table is intentionally separate from those rules
 * and serves only to drive end-to-end wiring tests for the Safety
 * Resolver.
 *
 * <p><b>Sent sentinels</b>: a content string that starts with
 * {@link #SENTINEL_PREFIX} forces the matching scenario regardless of
 * the keyword table. Useful for integration tests and ops smoke runs.
 * Sentinels are recognised by the {@code mock} provider only — a real
 * LLM provider does not see them.
 */
@Component
public class MockRiskClassifierProvider implements RiskClassifierProvider {

    /** Marker that forces a scenario by name. Example:
     * {@code "force:LEVEL_4_EMERGENCY: ..."} selects
     * {@link RiskClassifierMockScenario#LEVEL_4_EMERGENCY}.
     */
    public static final String SENTINEL_PREFIX = "force:";

    /** Synthetic latency for each scenario in milliseconds. Fixed so tests
     * can assert exact timing where it matters. Real provider latency
     * varies. */
    private static final Map<RiskClassifierMockScenario, Long> SCENARIO_LATENCY_MS = Map.of(
            RiskClassifierMockScenario.LEVEL_1_NORMAL,    15L,
            RiskClassifierMockScenario.LEVEL_2_FOLLOWUP,  25L,
            RiskClassifierMockScenario.LEVEL_3_HIGH_RISK, 35L,
            RiskClassifierMockScenario.LEVEL_4_EMERGENCY, 40L,
            RiskClassifierMockScenario.TIMEOUT,          5000L,
            RiskClassifierMockScenario.MALFORMED_JSON,   20L
    );

    // DEMO keywords only — see class JavaDoc. Replaced by real prompt +
    // taxonomy once expert review produces them.
    private static final Map<RiskClassifierMockScenario, String[]> DEMO_KEYWORDS = Map.of(
            RiskClassifierMockScenario.LEVEL_4_EMERGENCY, new String[]{
                    "tuyệt vọng", "không muốn sống", "tự tử", "kết thúc tất cả"
            },
            RiskClassifierMockScenario.LEVEL_3_HIGH_RISK, new String[]{
                    "mệt mỏi quá", "kiệt sức", "không chịu nổi", "burnout", "không còn hy vọng"
            },
            RiskClassifierMockScenario.LEVEL_2_FOLLOWUP, new String[]{
                    "lo lắng", "bất an", "căng thẳng", "áp lực", "mất ngủ"
            },
            RiskClassifierMockScenario.LEVEL_1_NORMAL, new String[]{
                    "vui", "tốt", "ổn"
            }
    );

    private final RiskClassifierMockScenario forceScenario;

    public MockRiskClassifierProvider() {
        this(null);
    }

    /**
     * Constructor used by tests and by the optional
     * {@code mindbridge.ai.risk-classifier.mock.force-scenario} property
     * override.
     *
     * @param forceScenarioName the {@link RiskClassifierMockScenario} name
     *                          to force, or {@code null}/blank to use
     *                          keyword-based resolution.
     */
    public MockRiskClassifierProvider(
            @Value("${mindbridge.ai.risk-classifier.mock.force-scenario:}") String forceScenarioName) {
        this.forceScenario = parseForceScenario(forceScenarioName);
    }

    private static RiskClassifierMockScenario parseForceScenario(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return RiskClassifierMockScenario.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Unknown mindbridge.ai.risk-classifier.mock.force-scenario value: '"
                            + name + "'", ex);
        }
    }

    @Override
    public RiskClassifierOutput classify(RiskClassifierInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        RiskClassifierMockScenario scenario = resolveScenario(input);
        long latency = SCENARIO_LATENCY_MS.getOrDefault(scenario, 10L);

        return switch (scenario) {
            case LEVEL_1_NORMAL,
                 LEVEL_2_FOLLOWUP,
                 LEVEL_3_HIGH_RISK,
                 LEVEL_4_EMERGENCY -> scenario.defaultOutput(latency);

            case TIMEOUT -> throw new RiskClassifierTimeoutException(
                    "Risk classifier provider did not respond within timeout");

            case MALFORMED_JSON -> throw new InvalidRiskClassifierOutputException(
                    "Risk classifier provider returned a payload that failed schema validation");
        };
    }

    private RiskClassifierMockScenario resolveScenario(RiskClassifierInput input) {
        if (forceScenario != null) {
            return forceScenario;
        }

        String content = input.content();
        if (content == null || content.isBlank()) {
            return RiskClassifierMockScenario.LEVEL_1_NORMAL;
        }

        // Sentinels: "force:SCENARIO_NAME" prefix.
        if (content.startsWith(SENTINEL_PREFIX)) {
            int firstColon = content.indexOf(':');
            int secondColon = content.indexOf(':', firstColon + 1);
            String scenarioPart = secondColon > 0
                    ? content.substring(firstColon + 1, secondColon)
                    : content.substring(firstColon + 1);
            try {
                return RiskClassifierMockScenario.valueOf(scenarioPart.trim());
            } catch (IllegalArgumentException ignored) {
                // fall through to keyword resolution if sentinel is malformed
            }
        }

        String lower = content.toLowerCase(Locale.ROOT);
        // Order matters: most severe first.
        for (RiskClassifierMockScenario scenario : new RiskClassifierMockScenario[]{
                RiskClassifierMockScenario.LEVEL_4_EMERGENCY,
                RiskClassifierMockScenario.LEVEL_3_HIGH_RISK,
                RiskClassifierMockScenario.LEVEL_2_FOLLOWUP,
                RiskClassifierMockScenario.LEVEL_1_NORMAL}) {
            for (String keyword : DEMO_KEYWORDS.getOrDefault(scenario, new String[0])) {
                if (lower.contains(keyword)) {
                    return scenario;
                }
            }
        }
        return RiskClassifierMockScenario.LEVEL_1_NORMAL;
    }
}
