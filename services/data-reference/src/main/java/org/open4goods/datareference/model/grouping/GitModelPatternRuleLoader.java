package org.open4goods.datareference.model.grouping;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.serialization.DataReferenceJson;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads and validates the Git-authored model-pattern-rule resource checked
 * into this module.
 *
 * <p>A rule cannot name an O4G class the registry does not declare: a pattern
 * rule is scoped by canonical brand and class, so an unknown class would
 * silently never match anything rather than failing loudly at authoring time.
 */
public final class GitModelPatternRuleLoader {

    /** Classpath resource containing the schema used by this loader. */
    public static final String SCHEMA_RESOURCE = "/registry/model-pattern-rules.schema.json";
    /** Classpath resource containing the authored, checked-in pattern rules. */
    public static final String RESOURCE = "/registry/model-pattern-rules.json";

    private final ObjectMapper mapper;
    private final CanonicalRegistryLookup registry;

    /**
     * Creates a loader using the contract's single JSON mapper.
     *
     * @param registry canonical registry used to reject an unknown class id
     */
    public GitModelPatternRuleLoader(CanonicalRegistryLookup registry) {
        this(DataReferenceJson.mapper(), registry);
    }

    /**
     * Creates a loader with an explicit mapper, primarily for controlled tests.
     *
     * @param mapper mapper configured to reject unknown contract properties
     * @param registry canonical registry used to reject an unknown class id
     */
    public GitModelPatternRuleLoader(ObjectMapper mapper, CanonicalRegistryLookup registry) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
    }

    /**
     * Loads the schema and pattern-rule resources bundled with this module.
     *
     * @return verified, compiled rule registry
     * @throws IOException when a tracked resource cannot be read
     */
    public ModelPatternRuleRegistry loadDefault() throws IOException {
        try (InputStream schema = GitModelPatternRuleLoader.class.getResourceAsStream(SCHEMA_RESOURCE);
                InputStream resource = GitModelPatternRuleLoader.class.getResourceAsStream(RESOURCE)) {
            if (schema == null || resource == null) {
                throw new IOException("missing checked-in model pattern rule resource");
            }
            verifySchema(schema);
            return load(resource);
        }
    }

    /**
     * Validates and compiles a pattern-rule resource supplied by an importer.
     *
     * @param input exact Git resource bytes
     * @return verified, compiled rule registry
     * @throws IOException when input cannot be parsed
     */
    public ModelPatternRuleRegistry load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.readAllBytes();
        JsonNode tree = mapper.readTree(bytes);
        try {
            ModelPatternRuleJsonSchemaValidator.validate(tree);
            ModelPatternRuleDocument document = mapper.treeToValue(tree, ModelPatternRuleDocument.class);
            document.rules().forEach(this::validateSemantics);
            return new ModelPatternRuleRegistry(document.rules());
        } catch (ModelPatternRuleValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ModelPatternRuleValidationException(
                    "model pattern rule semantic validation failed: " + exception.getMessage(), exception);
        }
    }

    private void validateSemantics(ModelPatternRule rule) {
        if (registry.findClass(rule.canonicalClass()).isEmpty()) {
            throw new ModelPatternRuleValidationException("unknown canonical class id: " + rule.canonicalClass());
        }
    }

    private void verifySchema(InputStream input) throws IOException {
        JsonNode schema = mapper.readTree(input);
        if (!ModelPatternRuleDocument.SCHEMA_VERSION.equals(schema.path("$id").asString())) {
            throw new ModelPatternRuleValidationException("checked-in model pattern rule schema has an unexpected $id");
        }
    }
}
