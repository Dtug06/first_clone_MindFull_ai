package com.mindbridge.safety.event;

/**
 * Action type recorded against a {@code SafetyEvent}. Maps 1-to-1 with
 * the CHECK constraint on {@code safety_actions.action_type} (V17).
 *
 * <p>G3-T11 only persists the row with status {@code PENDING}; the
 * runtime execution is owned by the consuming module:
 * <ul>
 *   <li>{@link #SHOW_TEMPLATE} - owned by G3-T12 (Fixed Level 4
 *       Response). Resolves a {@code safety_response_templates} row
 *       and exposes it to the user via the assistant message
 *       channel.</li>
 *   <li>{@link #BLOCK_MATCHING} - owned by G6 (Program Matching
 *       Safety Gate). When the action is PENDING/SUCCEEDED, the
 *       matching service's {@code isUserBlocked} check returns true
 *       and matching decisions are forced to {@code SAFETY_BLOCKED}
 *       per docs/04 section 10.</li>
 *   <li>{@link #FLAG_REVIEW} - owned by G3-T13 (Expert Review).
 *       Marks the event for human review; no automated behavior
 *       today.</li>
 *   <li>{@link #PAUSE_PROGRAM} - owned by a future CBT-runtime task.
 *       Pauses an ACTIVE user program when safety escalates; not
 *       implemented in MVP.</li>
 * </ul>
 *
 * <p>One action failing MUST NOT block the others (G3-T11 Phase 1
 * decision C7): each action has its own {@code status} +
 * {@code error_message}. Execution isolation is enforced at the
 * consuming module, not by this enum.
 */
public enum SafetyActionType {

    /**
     * Render the fixed approved safety response template (Level 4 -
     * wired by G3-T12). For L3 events this action may also be
     * created when an approved template applies; T11 ships no
     * template catalog so the row stays PENDING until T12.
     */
    SHOW_TEMPLATE,

    /**
     * Block automated Program Matching for the user. The matching
     * service (G6) reads {@code SafetyEventService.isUserBlocked}
     * to decide. Always created at L3 and L4 (the chat pipeline
     * wires this in T11).
     */
    BLOCK_MATCHING,

    /**
     * Flag the event for human (expert) review. Created at L3 and
     * L4 (the chat pipeline wires this in T11). Execution owned by
     * G3-T13.
     */
    FLAG_REVIEW,

    /**
     * Pause an ACTIVE user program on safety escalation. Created by
     * future task - not wired in T11.
     */
    PAUSE_PROGRAM
}