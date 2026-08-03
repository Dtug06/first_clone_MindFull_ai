package com.mindbridge.analysis.provider.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Runtime JSON Schema validator for {@link com.mindbridge.analysis.provider.ChatAnalysisOutput}
 * payloads returned by external LLM providers.
 *
 * <p>Implementation note (G3-T07): the canonical schema lives at
 * {@code docs/schemas/chat_analysis_v1.schema.json} (Draft 07) and a
 * classpath copy at {@code src/main/resources/schemas/chat_analysis_v1.schema.json}
 * is loaded here at construction time so production code does not depend
 * on the filesystem at request time. The {@code com.networknt:json-schema-validator}
 * dependency was added in G3-T02 at test scope and is moved to compile
 * scope by this task.
 *
 * <p>The validator is invoked BEFORE any {@code ChatAnalysisOutput} is
 * constructed so a malformed payload never reaches the persistence
 * layer (rule "Invalid or incomplete output must not be stored as a
 * successful result" Ã¢â‚¬â€ {@code .cursor/rules/30-database-ai-safety.mdc}
 * Ã‚Â§AI Rules). Failure modes:
 *
 * <ul>
 *   <li>{@link #validate(String)} returns a non-empty set on JSON parse
 *       error or schema violation Ã¢â‚¬â€ the caller MUST treat this as
 *       {@code InvalidAnalysisOutputException}.</li>
 *   <li>The returned set is empty on success Ã¢â‚¬â€ caller can safely
 *       construct {@code ChatAnalysisOutput} from the parsed
 *       {@link JsonNode}.</li>
 * </ul>
 *
 * <p>This class never logs the raw payload (per rule 30
 * "Do not log unnecessary raw prompts or raw responses containing
 * sensitive data"). The exception message bubbles up to the redactor
 * in {@code AiAnalysisRunService}, which strips non-ASCII before
 * persistence.
 */
@Component
public class ChatAnalysisSchemaValidator {

    /** Classpath resource path. Keep in sync with the file under docs/schemas/. */
    public static final String SCHEMA_RESOURCE = "/schemas/chat_analysis_v1.schema.json";

    private final JsonSchema schema;

    public ChatAnalysisSchemaValidator() {
        this.schema = loadSchema();
    }

    private static JsonSchema loadSchema() {
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V7);
        try (InputStream in = ChatAnalysisSchemaValidator.class.getResourceAsStream(SCHEMA_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException(
                        "Schema resource not found at " + SCHEMA_RESOURCE
                                + " Ã¢â‚¬â€ copy from docs/schemas/chat_analysis_v1.schema.json");
            }
            return factory.getSchema(in);
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to load schema resource " + SCHEMA_RESOURCE, ex);
        }
    }

    /**
     * Validate a JSON payload string against the ChatAnalysisOutput v1 schema.
     *
     * @param json the raw JSON returned by the upstream provider.
     * @return the set of validation messages; empty on success.
     *         The set is mutable only inside this class Ã¢â‚¬â€ callers must
     *         treat it as read-only.
     */
    public Set<String> validate(String json) {
        if (json == null || json.isBlank()) {
            Set<String> errors = new LinkedHashSet<>();
            errors.add("payload is null or blank");
            return errors;
        }
        ObjectMapper mapper = new ObjectMapper();
        JsonNode node;
        try {
            node = mapper.readTree(json);
        } catch (IOException ex) {
            Set<String> errors = new LinkedHashSet<>();
            errors.add("payload is not valid JSON: " + ex.getClass().getSimpleName());
            return errors;
        }
        Set<ValidationMessage> messages = schema.validate(node);
        Set<String> out = new LinkedHashSet<>();
        for (ValidationMessage m : messages) {
            // The Message text does not contain the raw payload Ã¢â‚¬â€ only the
            // schema path and the violated rule Ã¢â‚¬â€ safe to surface verbatim.
            out.add(m.getMessage());
        }
        return out;
    }

    /**
     * Parse a validated JSON payload into a {@link JsonNode} for downstream
     * mapping into {@code ChatAnalysisOutput}. This helper is provided so
     * callers do not need their own {@link ObjectMapper}.
     */
    public JsonNode parse(String json) throws IOException {
        return new ObjectMapper().readTree(json);
    }
}
