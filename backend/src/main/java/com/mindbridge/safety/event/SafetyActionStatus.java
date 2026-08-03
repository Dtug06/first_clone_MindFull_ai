package com.mindbridge.safety.event;

/**
 * Execution status of a {@code SafetyAction}. Maps 1-to-1 with the
 * CHECK constraint on {@code safety_actions.status} (V17).
 *
 * <p>Per G3-T11 Phase 1 decision C7 (one action failing MUST NOT
 * block the others), each action carries its own status. The set of
 * terminal states is {@link #SUCCEEDED}, {@link #FAILED},
 * {@link #SKIPPED}; the only non-terminal state is {@link #PENDING}.
 *
 * <p>The T11 chat pipeline ALWAYS creates actions in {@code PENDING}
 * (per the user-approved spec - runtime execution belongs to T12,
 * T13, G6, and the future CBT runtime task). A future task may add a
 * status transition method on {@code SafetyAction} entity; T11 ships
 * no transition code.
 */
public enum SafetyActionStatus {

    /** Row created by the safety pipeline; execution has not run yet. */
    PENDING,

    /** Action executed successfully. */
    SUCCEEDED,

    /** Action execution failed. {@code error_message} is populated. */
    FAILED,

    /**
     * Action was intentionally skipped (e.g. action type not applicable
     * to the event's risk level). Distinct from FAILED - the system
     * made a deliberate choice, not an error.
     */
    SKIPPED
}