package com.mindbridge.safety.resolver;

/**
 * Source that triggered a Safety Resolver resolution. Recorded on every
 * {@link RiskStateHistory} row so audit trails can attribute which
 * signal pipeline produced the decision.
 *
 * <p>Per docs/04_SAFETY_AND_CBT_RULES.md §4 "Safety Input Sources" and
 * §5 "Safety Decision Components", every risk decision must be
 * attributable to at least one source. {@link #KEYWORD_PRE_FILTER} and
 * {@link #LLM_CLASSIFIER} are the two automatic sources wired in
 * G3-T08 and G3-T09; {@link #MANUAL_REVIEW} is reserved for G3-T13
 * (Expert Review) — at that point a reviewer can intentionally lower
 * a user's risk level (the only path that can downgrade risk, per the
 * G3-T10 Phase 1 decision Q3).
 */
public enum RiskStateSourceType {
    /** G3-T08 — keyword/regex pre-filter. */
    KEYWORD_PRE_FILTER,
    /** G3-T09 — LLM risk classifier. */
    LLM_CLASSIFIER,
    /** G3-T13 (future) — manual expert review. The only path that may
     *  downgrade risk. Not used by G3-T10. */
    MANUAL_REVIEW
}
