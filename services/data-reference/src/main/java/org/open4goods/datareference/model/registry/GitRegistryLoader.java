package org.open4goods.datareference.model.registry;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;

import org.open4goods.datareference.serialization.DataReferenceJson;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads and validates the canonical registry checked into this module.
 *
 * <p>The loader has no write operation: Git resources are the authority, while
 * a later storage adapter may only atomically replace its runtime copy with the
 * returned immutable index.
 */
public final class GitRegistryLoader {

    /** Classpath resource containing the schema used by this loader. */
    public static final String SCHEMA_RESOURCE = "/registry/o4g-registry.schema.json";
    /** Classpath resource containing the authored registry. */
    public static final String REGISTRY_RESOURCE = "/registry/o4g-registry.json";

    private final ObjectMapper mapper;

    /**
     * Creates a loader using the contract's single JSON mapper.
     */
    public GitRegistryLoader() {
        this(DataReferenceJson.mapper());
    }

    /**
     * Creates a loader with an explicit mapper, primarily for controlled tests.
     *
     * @param mapper mapper configured to reject unknown contract properties
     */
    public GitRegistryLoader(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
    }

    /**
     * Loads the schema and registry resources bundled with this module.
     *
     * @return verified immutable runtime index
     * @throws IOException when a tracked resource cannot be read
     */
    public RegistryRuntimeIndex loadDefault() throws IOException {
        try (InputStream schema = GitRegistryLoader.class.getResourceAsStream(SCHEMA_RESOURCE);
                InputStream registry = GitRegistryLoader.class.getResourceAsStream(REGISTRY_RESOURCE)) {
            if (schema == null || registry == null) {
                throw new IOException("missing checked-in registry resource");
            }
            verifySchema(schema);
            return load(registry);
        }
    }

    /**
     * Validates and indexes a registry resource supplied by an importer.
     *
     * @param input exact Git resource bytes
     * @return verified immutable runtime index
     * @throws IOException when input cannot be parsed
     */
    public RegistryRuntimeIndex load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.readAllBytes();
        JsonNode tree = mapper.readTree(bytes);
        try {
            RegistryJsonSchemaValidator.validate(tree);
            RegistryDocument document = mapper.treeToValue(tree, RegistryDocument.class);
            InMemoryCanonicalRegistry registry = new InMemoryCanonicalRegistry(document);
            return new RegistryRuntimeIndex(registry, sha256(bytes), registry.classCount(), registry.attributeCount());
        } catch (RegistryValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new RegistryValidationException("registry semantic validation failed: " + exception.getMessage(), exception);
        }
    }

    private void verifySchema(InputStream input) throws IOException {
        JsonNode schema = mapper.readTree(input);
        if (!RegistryDocument.SCHEMA_VERSION.equals(schema.path("$id").asString())) {
            throw new RegistryValidationException("checked-in registry schema has an unexpected $id");
        }
    }

    private static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Java runtime lacks SHA-256", exception);
        }
    }
}
