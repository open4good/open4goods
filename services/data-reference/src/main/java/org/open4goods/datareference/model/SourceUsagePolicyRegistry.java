package org.open4goods.datareference.model;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.serialization.DataReferenceJson;

/**
 * Immutable, deny-by-default lookup over Git-versioned source-usage policies.
 *
 * <p>A record may refer only to a policy present in this registry and belonging
 * to the same source. This keeps an accessible endpoint or an arbitrary policy
 * id from becoming an implicit publication permission.
 */
public final class SourceUsagePolicyRegistry {

    /** Classpath location of the authored policy inventory. */
    public static final String POLICY_RESOURCE = "/policy/source-usage-policies.json";

    private final Map<SourceUsagePolicyRef, SourceUsagePolicy> policies;

    /**
     * Builds a validated immutable registry from Git policy records.
     *
     * @param document checked-in policy document
     */
    public SourceUsagePolicyRegistry(SourceUsagePolicyDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        Map<SourceUsagePolicyRef, SourceUsagePolicy> indexed = new LinkedHashMap<>();
        for (SourceUsagePolicy policy : document.policies()) {
            SourceUsagePolicy previous = indexed.putIfAbsent(policy.reference(), policy);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate source usage policy: " + policy.reference());
            }
        }
        policies = Collections.unmodifiableMap(new LinkedHashMap<>(indexed));
    }

    /**
     * Loads the policy inventory packaged with this module.
     *
     * @return immutable Git-authored policy registry
     * @throws IOException when the policy resource cannot be read
     */
    public static SourceUsagePolicyRegistry loadDefault() throws IOException {
        try (InputStream input = SourceUsagePolicyRegistry.class.getResourceAsStream(POLICY_RESOURCE)) {
            if (input == null) {
                throw new IOException("missing checked-in source usage policy resource");
            }
            return new SourceUsagePolicyRegistry(DataReferenceJson.mapper()
                    .readValue(input.readAllBytes(), SourceUsagePolicyDocument.class));
        } catch (IllegalArgumentException exception) {
            throw new IOException("invalid source usage policy resource: " + exception.getMessage(), exception);
        }
    }

    /**
     * Finds one exact versioned policy record.
     *
     * @param reference versioned reference recorded by a source head
     * @return matching policy, when present
     */
    public Optional<SourceUsagePolicy> find(SourceUsagePolicyRef reference) {
        return Optional.ofNullable(policies.get(reference));
    }

    /**
     * Tests direct source evidence against the complete publication gate.
     *
     * @param sourceId source that produced the assertion
     * @param reference exact policy version carried by the source head
     * @param contentType assertion content type
     * @param surface proposed publication surface
     * @param instant publication instant
     * @return {@code true} only for an existing, matching and reviewed permission
     */
    public boolean allows(
            SourceId sourceId,
            SourceUsagePolicyRef reference,
            SourceContentType contentType,
            ProjectionSurface surface,
            Instant instant) {
        if (sourceId == null || reference == null) {
            return false;
        }
        return find(reference)
                .filter(policy -> policy.sourceId().equals(sourceId))
                .map(policy -> policy.allows(contentType, surface, instant))
                .orElse(false);
    }

    /**
     * Derived values never inherit a source redistribution permission.
     *
     * @return always {@code false}; a separate owner-reviewed policy is required
     */
    public boolean allowsDerivedField() {
        return false;
    }

    /**
     * Tests whether source media cache use remains within the reviewed policy.
     *
     * @param sourceId source that produced the media
     * @param reference exact policy version carried by the source head
     * @param surface proposed publication surface
     * @param mediaBytes whether cache use reads image bytes rather than a link
     * @param retrievedAt retrieval instant
     * @param instant cache-use instant
     * @return {@code true} only when publication and both cache windows are allowed
     */
    public boolean allowsMediaCache(
            SourceId sourceId,
            SourceUsagePolicyRef reference,
            ProjectionSurface surface,
            boolean mediaBytes,
            Instant retrievedAt,
            Instant instant) {
        if (!allows(sourceId, reference, SourceContentType.MEDIA, surface, instant)) {
            return false;
        }
        return find(reference).orElseThrow().allowsMediaCache(mediaBytes, retrievedAt, instant);
    }

    /**
     * Returns a stable snapshot for diagnostics and review tooling.
     *
     * @return policy values in authored order
     */
    public List<SourceUsagePolicy> policies() {
        return List.copyOf(policies.values());
    }
}
