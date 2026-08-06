package com.mindbridge.analysis.provider;

import java.util.List;

/**
 * Closed taxonomy of behaviour-level tags the chat analysis model may
 * attach to a message. Mirrors
 * {@code docs/prompts/chat_analysis_prompt_v1.md} §49-51 exactly.
 *
 * <p>A signal is a short label describing an observed pattern in the
 * message; signals are independent of the {@link Topic} (subject) and
 * {@link Emotion} (tone) and can be combined freely. The Safety
 * Resolver (G3-T10) consumes the {@code signals} list as one input to
 * its max-wins decision.
 *
 * <p><b>Sensitive codes carry a {@code _DEMO} suffix</b> per
 * {@code docs/04 §1} "Cursor không được tự đặt threshold" and
 * {@code docs/safety/risk_classifier_test_cases.md} "labels here are
 * intentionally suffixed {@code _DEMO}". The full production taxonomy
 * is an expert-review item.
 *
 * <p>See {@link Topic} for the enum-vs-string rationale and the
 * versioning rule.
 */
public enum Signal {

    /** Persistent tiredness, low energy. */
    FATIGUE,

    /** Trouble sleeping, fragmented sleep, insomnia. */
    SLEEP_DISRUPTION,

    /** Withdrawal from social contact, feeling alone. */
    ISOLATION,

    /** Expressions of giving up, no way out (safety-relevant). */
    HOPELESSNESS,

    /** Long-term exhaustion from chronic stress. */
    BURNOUT,

    /** Self-harm ideation (safety-relevant; Safety Resolver must see this). */
    SELF_HARM_RISK,

    /** Active interpersonal conflict. */
    CONFLICT,

    /** Recent loss, bereavement. */
    GRIEF,

    /** Catch-all for signals that do not fit the categories above. */
    OTHER;

    /**
     * Convenience for callers that need a defensive {@code List.copyOf}.
     * Currently unused at the public API but kept here so that future
     * helper methods on the enum have a consistent null-safety story.
     */
    public static List<Signal> empty() {
        return List.of();
    }
}
