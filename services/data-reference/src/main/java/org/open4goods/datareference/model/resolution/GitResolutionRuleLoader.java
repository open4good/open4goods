package org.open4goods.datareference.model.resolution;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceUsagePolicyRegistry;
import org.open4goods.datareference.port.CanonicalRegistryLookup;
import org.open4goods.datareference.serialization.DataReferenceJson;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Loads and validates the Git-authored resolution-rule resource checked into
 * this module.
 *
 * <p>A rule cannot pre-authorise a source: every ranked source must already
 * carry a reviewed-or-not usage-policy entry, so a resolution rule can never be
 * the first place a source is introduced to the system.
 *
 * <p>A merchant-feed source (AC5) never ranks for a canonical reference
 * attribute: it stays authoritative for its own current offer and price only,
 * through {@code OfferHead}, and cannot override a regulatory or technical
 * reference value resolved here.
 */
public final class GitResolutionRuleLoader {

    /** Classpath resource containing the schema used by this loader. */
    public static final String SCHEMA_RESOURCE = "/registry/resolution-rules.schema.json";
    /** Classpath resource containing the authored, checked-in resolution rules. */
    public static final String RESOURCE = "/registry/resolution-rules.json";
    /** Source id prefix reserved for per-network merchant feed sources. */
    private static final String MERCHANT_FEED_PREFIX = "merchant-feed.";

    private final ObjectMapper mapper;
    private final CanonicalRegistryLookup registry;
    private final SourceUsagePolicyRegistry policies;

    /**
     * Creates a loader using the contract's single JSON mapper.
     *
     * @param registry canonical registry used to reject an unknown attribute id
     * @param policies usage-policy registry used to reject an unauthorised source
     */
    public GitResolutionRuleLoader(CanonicalRegistryLookup registry, SourceUsagePolicyRegistry policies) {
        this(DataReferenceJson.mapper(), registry, policies);
    }

    /**
     * Creates a loader with an explicit mapper, primarily for controlled tests.
     *
     * @param mapper mapper configured to reject unknown contract properties
     * @param registry canonical registry used to reject an unknown attribute id
     * @param policies usage-policy registry used to reject an unauthorised source
     */
    public GitResolutionRuleLoader(ObjectMapper mapper, CanonicalRegistryLookup registry, SourceUsagePolicyRegistry policies) {
        this.mapper = Objects.requireNonNull(mapper, "mapper must not be null");
        this.registry = Objects.requireNonNull(registry, "registry must not be null");
        this.policies = Objects.requireNonNull(policies, "policies must not be null");
    }

    /**
     * Loads the schema and resolution-rule resources bundled with this module.
     *
     * @return verified immutable rule registry
     * @throws IOException when a tracked resource cannot be read
     */
    public ResolutionRuleRegistry loadDefault() throws IOException {
        try (InputStream schema = GitResolutionRuleLoader.class.getResourceAsStream(SCHEMA_RESOURCE);
                InputStream resource = GitResolutionRuleLoader.class.getResourceAsStream(RESOURCE)) {
            if (schema == null || resource == null) {
                throw new IOException("missing checked-in resolution rule resource");
            }
            verifySchema(schema);
            return load(resource);
        }
    }

    /**
     * Validates and indexes a resolution-rule resource supplied by an importer.
     *
     * @param input exact Git resource bytes
     * @return verified immutable rule registry
     * @throws IOException when input cannot be parsed
     */
    public ResolutionRuleRegistry load(InputStream input) throws IOException {
        Objects.requireNonNull(input, "input must not be null");
        byte[] bytes = input.readAllBytes();
        JsonNode tree = mapper.readTree(bytes);
        try {
            ResolutionRuleJsonSchemaValidator.validate(tree);
            ResolutionRuleDocument document = mapper.treeToValue(tree, ResolutionRuleDocument.class);
            document.rules().forEach(this::validateSemantics);
            return new ResolutionRuleRegistry(document.rules());
        } catch (ResolutionRuleValidationException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new ResolutionRuleValidationException(
                    "resolution rule semantic validation failed: " + exception.getMessage(), exception);
        }
    }

    private void validateSemantics(ResolutionRule rule) {
        if (registry.findAttribute(rule.attribute()).isEmpty()) {
            throw new ResolutionRuleValidationException("unknown canonical attribute id: " + rule.attribute());
        }
        for (SourceId source : rule.rankedSources()) {
            if (policies.policies().stream().noneMatch(policy -> policy.sourceId().equals(source))) {
                throw new ResolutionRuleValidationException(
                        "resolution rule names a source with no usage-policy entry: " + source);
            }
            if (source.value().startsWith(MERCHANT_FEED_PREFIX)) {
                throw new ResolutionRuleValidationException(
                        "resolution rule must not rank a merchant-feed source for a reference attribute: " + source);
            }
        }
    }

    private void verifySchema(InputStream input) throws IOException {
        JsonNode schema = mapper.readTree(input);
        if (!ResolutionRuleDocument.SCHEMA_VERSION.equals(schema.path("$id").asString())) {
            throw new ResolutionRuleValidationException("checked-in resolution rule schema has an unexpected $id");
        }
    }
}
