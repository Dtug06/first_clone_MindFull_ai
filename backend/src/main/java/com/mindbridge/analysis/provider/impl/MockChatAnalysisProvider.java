package com.mindbridge.analysis.provider.impl;

import com.mindbridge.analysis.exception.InvalidAnalysisOutputException;
import com.mindbridge.analysis.exception.ProviderTimeoutException;
import com.mindbridge.analysis.provider.ChatAnalysisInput;
import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.provider.ChatAnalysisProvider;
import com.mindbridge.analysis.provider.MockScenario;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Deterministic, offline {@link ChatAnalysisProvider} used in local and
 * test profiles. It maps each input to one of six
 * {@link MockScenario}s based on a small set of DEMO keywords and
 * optional sentinels, and produces a fixed {@link ChatAnalysisOutput}.
 *
 * <p>This implementation does not call any external service. It is the
 * MVP substitute for the real LLM provider so the rest of the system
 * (chat consumer, daily features, safety resolver, matching) can be
 * built and tested end-to-end without a network or an API key.
 *
 * <p><b>Keyword scope (intentionally minimal for G3-T01):</b> the keyword
 * table below is DEMO only. Production keyword/symptom matching is the
 * responsibility of the safety rule engine (G3-T08) and will replace
 * these markers at that time. Do NOT ship this provider to production
 * with these keywords as the source of truth.
 *
 * <p><b>Sent sentinels</b>: a content string that starts with
 * {@link #SENTINEL_PREFIX} forces the matching scenario regardless of
 * the keyword table. Useful for integration tests and ops smoke runs.
 * Sentinels are recognised by the {@code mock} provider only — the
 * real LLM provider does not see them.
 */
@Component
public class MockChatAnalysisProvider implements ChatAnalysisProvider {

    /**
     * Marker that forces a scenario by name. Example:
     * {@code "force:LEVEL_4_EMERGENCY: ..."} selects {@link
     * MockScenario#LEVEL_4_EMERGENCY}.
     */
    public static final String SENTINEL_PREFIX = "force:";

    /**
     * Synthetic latency for each scenario in milliseconds. Values are
     * fixed so tests can assert exact {@code latencyMs} where it
     * matters. Real provider latency varies.
     */
    private static final Map<MockScenario, Long> SCENARIO_LATENCY_MS = Map.of(
            MockScenario.LEVEL_1_NORMAL,    15L,
            MockScenario.LEVEL_2_FOLLOWUP,  25L,
            MockScenario.LEVEL_3_HIGH_RISK, 35L,
            MockScenario.LEVEL_4_EMERGENCY, 40L,
            MockScenario.TIMEOUT,           5000L,
            MockScenario.MALFORMED_JSON,    20L
    );

    // DEMO keywords only — see class JavaDoc. Replaced by G3-T08 rule engine.
    private static final Map<MockScenario, String[]> DEMO_KEYWORDS = Map.of(
            MockScenario.LEVEL_4_EMERGENCY, new String[]{
                    "tuyệt vọng", "không muốn sống", "tự tử", "kết thúc tất cả"
            },
            MockScenario.LEVEL_3_HIGH_RISK, new String[]{
                    "mệt mỏi quá", "kiệt sức", "không chịu nổi", "burnout"
            },
            MockScenario.LEVEL_2_FOLLOWUP, new String[]{
                    "lo lắng", "bất an", "căng thẳng", "áp lực"
            },
            MockScenario.LEVEL_1_NORMAL, new String[]{
                    "vui", "tốt", "ổn"
            }
    );

    private final MockScenario forceScenario;

    public MockChatAnalysisProvider() {
        this(null);
    }

    /**
     * Constructor used by tests and by the optional
     * {@code mindbridge.ai.mock.force-scenario} property override.
     *
     * @param forceScenarioName the {@link MockScenario} name to force, or
     *                          {@code null}/blank to use keyword-based
     *                          resolution.
     */
    public MockChatAnalysisProvider(
            @Value("${mindbridge.ai.mock.force-scenario:}") String forceScenarioName) {
        this.forceScenario = parseForceScenario(forceScenarioName);
    }

    private static MockScenario parseForceScenario(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        try {
            return MockScenario.valueOf(name.trim());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "Unknown mindbridge.ai.mock.force-scenario value: '" + name + "'", ex);
        }
    }

    @Override
    public ChatAnalysisOutput analyze(ChatAnalysisInput input) {
        if (input == null) {
            throw new IllegalArgumentException("input must not be null");
        }

        MockScenario scenario = resolveScenario(input);
        long latency = SCENARIO_LATENCY_MS.getOrDefault(scenario, 10L);

        return switch (scenario) {
            case LEVEL_1_NORMAL,
                 LEVEL_2_FOLLOWUP,
                 LEVEL_3_HIGH_RISK,
                 LEVEL_4_EMERGENCY -> scenario.defaultOutput(latency);

            case TIMEOUT -> throw new ProviderTimeoutException(
                    "AI provider did not respond within timeout");

            case MALFORMED_JSON -> throw new InvalidAnalysisOutputException(
                    "AI provider returned a payload that failed schema validation");
        };
    }

    private MockScenario resolveScenario(ChatAnalysisInput input) {
        if (forceScenario != null) {
            return forceScenario;
        }

        String content = input.content();
        if (content == null || content.isBlank()) {
            return MockScenario.LEVEL_1_NORMAL;
        }

        // Sentinels: "force:SCENARIO_NAME" prefix.
        if (content.startsWith(SENTINEL_PREFIX)) {
            int firstColon = content.indexOf(':');
            int secondColon = content.indexOf(':', firstColon + 1);
            String scenarioPart = secondColon > 0
                    ? content.substring(firstColon + 1, secondColon)
                    : content.substring(firstColon + 1);
            try {
                return MockScenario.valueOf(scenarioPart.trim());
            } catch (IllegalArgumentException ignored) {
                // fall through to keyword resolution if sentinel is malformed
            }
        }

        String lower = content.toLowerCase(Locale.ROOT);
        // Order matters: most severe first.
        for (MockScenario scenario : new MockScenario[]{
                MockScenario.LEVEL_4_EMERGENCY,
                MockScenario.LEVEL_3_HIGH_RISK,
                MockScenario.LEVEL_2_FOLLOWUP,
                MockScenario.LEVEL_1_NORMAL}) {
            for (String keyword : DEMO_KEYWORDS.getOrDefault(scenario, new String[0])) {
                if (lower.contains(keyword)) {
                    return scenario;
                }
            }
        }
        return MockScenario.LEVEL_1_NORMAL;
    }
}