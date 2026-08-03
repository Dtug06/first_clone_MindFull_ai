-- V19  G3-T12: Audit template_version on safety_actions
--
-- Adds two nullable columns to safety_actions so the SHOW_TEMPLATE executor
-- can record exactly which expert-approved Safety response template was
-- shown to the user (per docs/04 §3.4 "Sử dụng fixed approved Safety
-- Response" + the G3-T12 acceptance criteria "audit sau này biết đã show
-- đúng bản duyệt nào"). Both columns are NULLABLE on purpose:
--
--   * PENDING rows (just inserted by the chat pipeline) have no template
--     yet, so both fields are NULL until the executor transitions the row.
--   * SKIPPED rows (no APPROVED template found) intentionally stay NULL -
--     recording templateVersion = null on a SKIPPED row means "we could
--     not serve a response at all" which is itself the audit signal.
--   * The executor only writes these fields via the controlled transition
--     methods on SafetyAction (see SafetyAction.markSucceeded/markFailed/
--     markSkipped overloads); there is no public setter.
--
-- Cascade policy on the FK:
--   * template_id REFERENCES safety_response_templates(id) ON DELETE
--     SET NULL. If an admin retires a template (status -> RETIRED but
--     the row stays) the FK survives. If the row is later hard-deleted
--     (future retention task), the action row keeps the template_version
--     label for audit and the FK dangles harmlessly. This mirrors the
--     pattern used by risk_state_history (V14) for rule_id.
--
-- No seed rows. V19 is purely additive; existing PENDING action rows
-- remain valid with both new columns NULL.
--
-- Index: a (template_id) index helps audit queries that ask "which
-- actions referenced this template row?"  rare in MVP but cheap to add
-- here while the table is small. Composite indexes are not needed -
-- per-action queries are dominated by safety_event_id which already has
-- an index (V17 safety_actions_event_idx).

ALTER TABLE safety_actions
    ADD COLUMN template_id      UUID         NULL
        REFERENCES safety_response_templates(id) ON DELETE SET NULL,
    ADD COLUMN template_version VARCHAR(50)  NULL;

-- Cheap audit-lookup index. Partial index would be ideal (status !=
-- 'PENDING') but H2 in the test mirror does not honour partial indexes
-- here, so we keep a plain index and accept the small extra extra cost on
-- PENDING rows.
CREATE INDEX safety_actions_template_idx
    ON safety_actions (template_id);