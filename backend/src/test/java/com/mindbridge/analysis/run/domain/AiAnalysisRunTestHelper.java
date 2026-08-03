package com.mindbridge.analysis.run.domain;

import java.time.OffsetDateTime;
import java.util.UUID;

final class AiAnalysisRunTestHelper {

    private AiAnalysisRunTestHelper() {
    }

    public static AiAnalysisRun succeededRun(
            UUID runId,
            UUID messageId,
            UUID userId,
            String provider,
            String model,
            String promptVersion,
            String inputHash,
            String outputHash,
            OffsetDateTime clock) {

        AiAnalysisRun run = AiAnalysisRun.createPending(
                runId, messageId, userId,
                provider, model, promptVersion,
                inputHash, clock);

        run.markRunning(clock);
        run.markSucceeded(outputHash, 10, null, null,
                (short) 1, java.math.BigDecimal.valueOf(0.72), clock);
        return run;
    }
}