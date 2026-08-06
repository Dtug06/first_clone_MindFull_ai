package com.mindbridge.behavior.feature.dto;

/**
 * G4-T03: CBT runtime availability for daily aggregation.
 *
 * <p>Three states:
 * <ul>
 *   <li>{@link #NOT_SHIPPED} - G5 CBT runtime (programs, modules, exercises,
 *       exercise_assignments, exercise_submissions) has not shipped.
 *       {@code exercise_assignments} table does not exist in the DB.
 *       This is the MVP baseline as of 2026-08-04.</li>
 *   <li>{@link #NOT_APPLICABLE} - CBT runtime shipped but the user has no
 *       CBT activity on the requested local date (no assignments, no submissions).
 *       Future state; not used in MVP because G5 is not shipped.</li>
 *   <li>{@link #COMPUTABLE} - CBT runtime shipped and there is at least one
 *       exercise assignment for the requested local date.
 *       Future state; not used in MVP.</li>
 * </ul>
 *
 * <p>Detection happens once at service start-up via a table-existence
 * check on {@code exercise_assignments}. When G5 ships and the migration
 * creates that table, the service automatically transitions to
 * {@code COMPUTABLE} without code change.
 */
public enum CbtAvailability {
    NOT_SHIPPED,
    NOT_APPLICABLE,
    COMPUTABLE
}