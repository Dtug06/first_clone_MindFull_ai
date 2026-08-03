package com.mindbridge.analysis.provider;

/**
 * The single global prompt version identifier that the chat analysis
 * runtime uses. The value mirrors the SHA-256 short hash in
 * {@code docs/prompts/chat_analysis_prompt_v1.md} (line 232):
 * <pre>
 * v1-prompt-body-sha256:         5363675e22fe77100908eaee6ab003207da57ba557e7c09d5d52671c1a9447e2
 * v1-prompt-body-sha256-short:   v1:5363675e22fe
 * </pre>
 *
 * <p>This constant is captured into the {@code ai_analysis_runs.prompt_version}
 * column at run-creation time so audit can reconstruct "which prompt was
 * used to produce this run?" — per rule 30-database-ai-safety.mdc "Store:
 * ... prompt version".
 *
 * <p>If the prompt file is updated, change this constant in lockstep
 * with the prompt file's {@code v1-prompt-body-sha256} line. A future
 * task may add a test that recomputes the SHA at build time and
 * fails if the constant drifts.
 */
public final class ChatAnalysisPromptVersion {

    /**
     * The current prompt version. Identical to the
     * {@code v1-prompt-body-sha256-short} recorded in
     * {@code docs/prompts/chat_analysis_prompt_v1.md}.
     */
    public static final String CURRENT = "v1:5363675e22fe";

    private ChatAnalysisPromptVersion() {
        // No instances.
    }
}