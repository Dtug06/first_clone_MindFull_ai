package com.mindbridge.safety.response;

/**
 * Lifecycle status of a {@code SafetyResponseTemplate}. Mirrors
 * {@code docs/04_SAFETY_AND_CBT_RULES.md} section 15 (CBT Content Status)
 * and the precedent set by {@code SafetyRuleStatus} (T08 pre-filter).
 *
 * <p>Only rows in status {@link #APPROVED} are eligible for the
 * {@code SHOW_TEMPLATE} executor to return to a user. DRAFT and
 * PENDING_REVIEW rows are never served; missing approved content yields a
 * {@code SKIPPED} action with an explicit reason (no invented crisis
 * wording per docs/04 section 27).
 */
public enum SafetyResponseTemplateStatus {

    /** Editable. Never served to users. */
    DRAFT,

    /** Awaiting expert review. Never served to users. */
    PENDING_REVIEW,

    /**
     * Approved by an EXPERT or ADMIN and active. At most one APPROVED row
     * per (code, locale, risk_reason) and at most one default APPROVED row
     * per locale (enforced by partial unique indexes in V18).
     */
    APPROVED,

    /** Previously approved, now disabled. Never served to users. */
    RETIRED
}
