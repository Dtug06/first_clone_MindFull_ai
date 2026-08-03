package com.mindbridge.analysis.provider.pipeline;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Verifies the G3-T07 fallback decision matrix cell-by-cell.
 *
 * <p>The matrix is locked in the Phase 1 plan (Â§7 "Fallback policy"):
 *
 * <ul>
 *   <li>retries NOT exhausted â†’ never fallback (next attempt still
 *       in scope).</li>
 *   <li>retries exhausted + fallback disabled â†’ never fallback.</li>
 *   <li>retries exhausted + fallback enabled + Level 4 last attempt â†’
 *       never fallback (safety rule "Do not silently downgrade a
 *       model risk signal").</li>
 *   <li>otherwise â†’ fallback.</li>
 * </ul>
 */
@DisplayName("FallbackDecision â€” policy table")
class FallbackDecisionTest {

    @Test
    @DisplayName("Cell 1: retries not exhausted â†’ no fallback")
    void retriesNotExhausted_neverFallback() {
        assertThat(FallbackDecision.shouldFallback(false, true, 1)).isFalse();
        assertThat(FallbackDecision.shouldFallback(false, true, 4)).isFalse();
        assertThat(FallbackDecision.shouldFallback(false, false, -1)).isFalse();
    }

    @Test
    @DisplayName("Cell 2: retries exhausted + fallback disabled â†’ no fallback")
    void fallbackDisabled_neverFallback() {
        assertThat(FallbackDecision.shouldFallback(true, false, 1)).isFalse();
        assertThat(FallbackDecision.shouldFallback(true, false, 2)).isFalse();
        assertThat(FallbackDecision.shouldFallback(true, false, -1)).isFalse();
    }

    @Test
    @DisplayName("Cell 3: Level 4 last attempt â†’ no fallback (safety guard)")
    void level4_neverFallback() {
        assertThat(FallbackDecision.shouldFallback(true, true, 4)).isFalse();
    }

    @Test
    @DisplayName("Cell 4: retries exhausted + fallback enabled + non-Level-4 â†’ fallback")
    void normalCase_fallback() {
        assertThat(FallbackDecision.shouldFallback(true, true, 1)).isTrue();
        assertThat(FallbackDecision.shouldFallback(true, true, 2)).isTrue();
        assertThat(FallbackDecision.shouldFallback(true, true, 3)).isTrue();
        assertThat(FallbackDecision.shouldFallback(true, true, -1)).isTrue();
    }

    @Test
    @DisplayName("Table sanity: 16-cell truth table exhaustive")
    void truthTable_exhaustive() {
        // 2 Ã— 2 Ã— 4 (retries Ã— enabled Ã— risk) = 16 cells.
        // Level 4 only flips to false when retries-exhausted AND fallback-enabled.
        // Non-Level 4 with retries-exhausted AND fallback-enabled â†’ true.
        // All other combinations â†’ false.
        int fallbackTrue = 0;
        for (boolean retries : new boolean[]{false, true}) {
            for (boolean enabled : new boolean[]{false, true}) {
                for (int risk : new int[]{-1, 1, 2, 4}) {
                    boolean result = FallbackDecision.shouldFallback(retries, enabled, risk);
                    boolean expected = retries && enabled && risk != 4;
                    assertThat(result)
                            .as("retries=%s enabled=%s risk=%d", retries, enabled, risk)
                            .isEqualTo(expected);
                    if (result) fallbackTrue++;
                }
            }
        }
        // Count: retries=true, enabled=true, risk in {-1, 1, 2} = 3 cells true.
        assertThat(fallbackTrue).isEqualTo(3);
    }
}
