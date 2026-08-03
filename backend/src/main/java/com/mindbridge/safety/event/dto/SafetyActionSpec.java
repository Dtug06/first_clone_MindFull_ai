package com.mindbridge.safety.event.dto;

import com.mindbridge.safety.event.SafetyActionType;

/**
 * Caller-supplied description of one action to attach to a
 * {@code SafetyEvent}. Used as input to
 * {@code SafetyEventService.recordLevel3Or4Event(...)}.
 *
 * <p>G3-T11 only persists the row with status {@code PENDING}. Runtime
 * execution is owned by the consuming module (T12 for
 * {@link SafetyActionType#SHOW_TEMPLATE}, G6 for {@link #BLOCK_MATCHING},
 * T13 for {@link #FLAG_REVIEW}, future CBT task for
 * {@link #PAUSE_PROGRAM}). One action failing MUST NOT block the
 * others (G3-T11 Phase 1 decision C7).
 *
 * @param actionType the type of the action. Never null.
 */
public record SafetyActionSpec(
        SafetyActionType actionType
) {
    public SafetyActionSpec {
        if (actionType == null) {
            throw new IllegalArgumentException("actionType must not be null");
        }
    }
}