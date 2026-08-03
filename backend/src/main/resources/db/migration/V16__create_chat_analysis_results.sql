-- V16: Create chat_analysis_results table.
-- G3-T05: stores versioned chat analysis outputs.
-- Only model_risk_level lives here; rule_risk_level and final_risk_level
-- belong to risk_state_history (§6.1).

CREATE TABLE chat_analysis_results (
    id                      UUID          NOT NULL PRIMARY KEY,
    analysis_run_id         UUID          NOT NULL,
    conversation_message_id UUID          NOT NULL,
    user_id                 UUID          NOT NULL,
    topic                   VARCHAR(40)   NOT NULL,
    emotion                 VARCHAR(20)   NOT NULL,
    intent                  VARCHAR(20)   NOT NULL,
    signals                 JSONB         NOT NULL DEFAULT ''[]''::jsonb,
    evidence_spans          JSONB         NOT NULL DEFAULT ''[]''::jsonb,
    model_risk_level        SMALLINT      NOT NULL,
    confidence              NUMERIC(4,3)  NOT NULL,
    analysis_status         VARCHAR(20)   NOT NULL DEFAULT ''ACTIVE'',
    supersedes_id           UUID,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT NOW(),

    CONSTRAINT chat_analysis_results_status_chk
        CHECK (analysis_status IN (''ACTIVE'', ''SUPERSEDED'', ''INVALIDATED'')),
    CONSTRAINT chat_analysis_results_model_risk_level_chk
        CHECK (model_risk_level BETWEEN 1 AND 4),
    CONSTRAINT chat_analysis_results_confidence_chk
        CHECK (confidence BETWEEN 0 AND 1),
    CONSTRAINT chat_analysis_results_supersedes_not_self_chk
        CHECK (supersedes_id IS NULL OR supersedes_id <> id),
    CONSTRAINT chat_analysis_results_run_fk
        FOREIGN KEY (analysis_run_id) REFERENCES ai_analysis_runs(id),
    CONSTRAINT chat_analysis_results_message_fk
        FOREIGN KEY (conversation_message_id) REFERENCES conversation_messages(id)
);

CREATE INDEX chat_analysis_results_message_active_created_desc
    ON chat_analysis_results (conversation_message_id, created_at DESC)
    WHERE analysis_status = ''ACTIVE'';

CREATE INDEX chat_analysis_results_user_created_desc
    ON chat_analysis_results (user_id, created_at DESC);

CREATE INDEX chat_analysis_results_supersedes_idx
    ON chat_analysis_results (supersedes_id);

-- Trigger: at most one ACTIVE row per conversation_message_id
CREATE OR REPLACE FUNCTION chat_analysis_results_one_active_per_message()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = ''UPDATE'' AND OLD.analysis_status = ''ACTIVE'' AND NEW.analysis_status = ''ACTIVE'') THEN
        RETURN NEW;
    END IF;

    IF (NEW.analysis_status = ''ACTIVE'') THEN
        IF EXISTS (
            SELECT 1 FROM chat_analysis_results
            WHERE conversation_message_id = NEW.conversation_message_id
              AND analysis_status = ''ACTIVE''
              AND id <> NEW.id
        ) THEN
            RAISE EXCEPTION ''Only one ACTIVE row allowed per conversation_message_id (existing ACTIVE row conflicts with %)'', NEW.id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER chat_analysis_results_one_active_per_message_trg
    BEFORE INSERT OR UPDATE ON chat_analysis_results
    FOR EACH ROW EXECUTE FUNCTION chat_analysis_results_one_active_per_message();

-- Trigger: supersedes chain integrity
CREATE OR REPLACE FUNCTION chat_analysis_results_supersedes_chain_integrity()
RETURNS TRIGGER AS $$
DECLARE
    parent_status VARCHAR(20);
BEGIN
    IF (NEW.supersedes_id IS NULL) THEN
        RETURN NEW;
    END IF;

    SELECT analysis_status INTO parent_status
    FROM chat_analysis_results WHERE id = NEW.supersedes_id;

    IF parent_status IS NULL THEN
        RAISE EXCEPTION ''supersedes_id % does not exist in chat_analysis_results'', NEW.supersedes_id;
    END IF;

    IF parent_status = ''ACTIVE'' THEN
        RAISE EXCEPTION ''supersedes_id % must reference a SUPERSEDED or INVALIDATED row (current status=ACTIVE)'', NEW.supersedes_id;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER chat_analysis_results_supersedes_chain_trg
    BEFORE INSERT ON chat_analysis_results
    FOR EACH ROW EXECUTE FUNCTION chat_analysis_results_supersedes_chain_integrity();