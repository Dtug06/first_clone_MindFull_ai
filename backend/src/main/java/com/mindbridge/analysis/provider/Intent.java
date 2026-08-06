package com.mindbridge.analysis.provider;

/**
 * What the user appears to want from the chat interaction, based on the
 * model's read of the message. Mirrors
 * {@code docs/prompts/chat_analysis_prompt_v1.md} §47-48 exactly.
 *
 * <p>The chat consumer uses this label to choose a conversational tone
 * and to decide whether to surface CBT-style reflection questions vs
 * informational content. <b>This is not a final action decision.</b> The
 * matching pipeline and the Safety Resolver remain the only
 * authoritative sources of action decisions (docs/04 §5; rule 00
 * "LLM không tự quyết định toàn bộ Safety Flow").
 *
 * <p>See {@link Topic} for the enum-vs-string rationale and the
 * versioning rule.
 */
public enum Intent {

    /** User is venting — they want to be heard, not given advice. */
    VENT,

    /** User is asking for guidance or suggestions. */
    ADVICE,

    /** User is asking for factual information. */
    INFO,

    /** User is looking for emotional support or reassurance. */
    SUPPORT
}
