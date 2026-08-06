package com.mindbridge.analysis.run.dto;

import com.mindbridge.analysis.provider.ChatAnalysisOutput;
import com.mindbridge.analysis.run.domain.AiAnalysisRunStatus;

/**
 * Internal pipeline result containing both the persisted run snapshot and the
 * validated provider output. The structured output is deliberately not
 * exposed by a controller; it is consumed immediately to persist the
 * authoritative chat analysis result.
 */
public record AiAnalysisExecutionResult(
        AiRunSummary run,
        ChatAnalysisOutput output
) {
    public boolean succeeded() {
        return run != null
                && run.status() == AiAnalysisRunStatus.SUCCEEDED
                && output != null;
    }
}
