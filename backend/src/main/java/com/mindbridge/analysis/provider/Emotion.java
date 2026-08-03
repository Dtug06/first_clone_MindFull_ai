package com.mindbridge.analysis.provider;

/**
 * Closed taxonomy of dominant emotions the chat analysis model may detect
 * in a user message. Mirrors
 * {@code docs/prompts/chat_analysis_prompt_v1.md} §45-46 exactly.
 *
 * <p>See {@link Topic} for the rationale on using an enum vs magic
 * strings, and the versioning rule (add new value = non-breaking;
 * remove/rename = bump {@link AnalysisSchemaVersion#CURRENT_SCHEMA_VERSION}).
 *
 * <p><b>Anti-diagnosis guard.</b> These labels describe an emotional tone
 * observed in the message, never a clinical state. The model MUST NOT
 * promote any value here to a diagnostic category (rule 00
 * "Do not implement diagnosis"; docs/04 §2 "Không phải công cụ chẩn
 * đoán").
 */
public enum Emotion {

    /** Neutral — no strong emotional signal detected. */
    NEUTRAL,

    /** Positive affect, content tone. */
    HAPPY,

    /** Anxious tone: worry, nervousness, rumination. */
    ANXIOUS,

    /** Sad tone: low mood, grief, disappointment. */
    SAD,

    /** Overwhelmed tone: feeling unable to cope. */
    OVERWHELMED,

    /** Acute distress signal — safety-relevant (caller must route to Safety Resolver). */
    DISTRESS,

    /** Angry tone: frustration, irritation, resentment. */
    ANGRY
}
