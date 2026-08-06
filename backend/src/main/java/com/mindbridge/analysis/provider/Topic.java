package com.mindbridge.analysis.provider;

/**
 * Closed taxonomy of topics the chat analysis model may extract from a user
 * message. Taxonomy mirrors {@code docs/prompts/chat_analysis_prompt_v1.md}
 * §43-44 exactly and is the single source of truth shared with the JSON
 * Schema in {@code docs/schemas/chat_analysis_v1.schema.json} and the
 * documentation in {@code docs/schemas/chat_analysis_v1.dictionary.md}.
 *
 * <p><b>Why an enum (not magic strings).</b> Type-safety: the
 * {@link ChatAnalysisOutput} record cannot be constructed with an
 * out-of-taxonomy value. Autocomplete at call sites. Refactor-safe: a
 * typo would not silently produce a value that bypasses schema
 * validation downstream.
 *
 * <p><b>Production rule.</b> Adding a new value here is a non-breaking
 * change to the schema contract (G3-T02 Phase 1 versioning decision
 * A — adding enum values does NOT bump {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION}).
 * Removing or renaming an existing value IS a breaking change and
 * requires a new schema version.
 *
 * <p>Mapping note: {@code BURNOUT} is intentionally NOT a topic — it is a
 * {@link Signal} (a behaviour-level tag the model attaches to the
 * message), not a top-level subject. Topics describe the subject the
 * user is talking about; signals describe how they seem to feel about
 * it.
 */
public enum Topic {

    /** Work-related stress: deadlines, workload, conflicts with colleagues. */
    WORK_STRESS,

    /** Romantic, friendship, or other interpersonal relationships. */
    RELATIONSHIP,

    /** Family dynamics, parenting, sibling or parent relationships. */
    FAMILY,

    /** Physical or mental health concerns (including general self-harm risk). */
    HEALTH,

    /** Money, debt, income, financial pressure. */
    FINANCE,

    /** Sleep quality, sleep disruption, insomnia. */
    SLEEP,

    /** Catch-all for topics that do not fit the categories above. */
    OTHER
}
