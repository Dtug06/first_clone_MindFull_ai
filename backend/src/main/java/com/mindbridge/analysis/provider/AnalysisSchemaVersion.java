package com.mindbridge.analysis.provider;

/**
 * Schema version constant for {@link ChatAnalysisOutput}.
 *
 * <p>Mirrors the pattern already established for the safety classifier
 * ({@code com.mindbridge.safety.classifier.RiskClassifierOutput.CURRENT_SCHEMA_VERSION = "V1"}):
 * every persisted AI analysis run records the schema version of the
 * DTO that produced it so audit can detect drift later (rule 30
 * "Store: ... schema version").
 *
 * <p><b>Versioning rule</b> (G3-T02 Phase 1 decision A):
 * <ul>
 *   <li>Adding or removing a field, or changing a field type, IS a
 *       breaking change → bump the constant and create a new JSON
 *       Schema file (e.g. {@code chat_analysis_v2.schema.json}).</li>
 *   <li>Adding a new value to {@link Topic}, {@link Emotion},
 *       {@link Intent}, or {@link Signal} is NOT a breaking change →
 *       keep the constant at the current version; update the JSON
 *       Schema and the dictionary file only.</li>
 *   <li>Renaming or removing an existing enum value IS a breaking
 *       change → bump the constant and create a new JSON Schema.</li>
 * </ul>
 *
 * <p>Current version {@code V1} matches the first structured output
 * contract introduced in G3-T01 / G3-T03. Subsequent versions must
 * add an {@code @since} tag to the relevant JavaDoc on the record
 * field that changed.
 */
public final class AnalysisSchemaVersion {

    /** Current schema version. See class JavaDoc for the bump rule. */
    public static final String CURRENT_SCHEMA_VERSION = "V1";

    private AnalysisSchemaVersion() {
        // No instances.
    }
}
