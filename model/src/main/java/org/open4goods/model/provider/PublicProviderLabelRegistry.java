package org.open4goods.model.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import tools.jackson.databind.ObjectMapper;

/**
 * Immutable, deny-by-default lookup mapping an internal source id to a
 * reviewed public provider label.
 *
 * <p>This registry is authoritative; nothing derives a public label from an
 * internal source id (truncation, casing, or any other transformation). A
 * source id with no reviewed entry has no label - see {@link #find(String)}.
 */
public final class PublicProviderLabelRegistry {

    /** Classpath location of the checked-in, Git-versioned label registry. */
    public static final String REGISTRY_RESOURCE = "/provider-labels/public-provider-labels.json";

    private final Map<String, PublicProviderLabel> labelsBySourceId;

    /**
     * Builds a validated immutable registry from Git-authored label records.
     *
     * @param document checked-in label document
     */
    public PublicProviderLabelRegistry(PublicProviderLabelDocument document) {
        Objects.requireNonNull(document, "document must not be null");
        Map<String, PublicProviderLabel> indexed = new LinkedHashMap<>();
        for (PublicProviderLabel label : document.labels()) {
            PublicProviderLabel previous = indexed.putIfAbsent(label.sourceId(), label);
            if (previous != null) {
                throw new IllegalArgumentException("duplicate public provider label: " + label.sourceId());
            }
        }
        labelsBySourceId = Collections.unmodifiableMap(indexed);
    }

    /**
     * Loads the label registry packaged with this module.
     *
     * @return immutable Git-authored public provider label registry
     * @throws IOException when the registry resource cannot be read
     */
    public static PublicProviderLabelRegistry loadDefault() throws IOException {
        return load(REGISTRY_RESOURCE);
    }

    /**
     * Loads a label registry from an arbitrary classpath resource, for tests
     * exercising a fixture set distinct from the shipped registry.
     *
     * @param classpathResource absolute classpath location of the label document
     * @return immutable label registry
     * @throws IOException when the resource cannot be read
     */
    public static PublicProviderLabelRegistry load(String classpathResource) throws IOException {
        try (InputStream input = PublicProviderLabelRegistry.class.getResourceAsStream(classpathResource)) {
            if (input == null) {
                throw new IOException("missing checked-in public provider label resource: " + classpathResource);
            }
            return new PublicProviderLabelRegistry(
                    new ObjectMapper().readValue(input, PublicProviderLabelDocument.class));
        } catch (RuntimeException exception) {
            throw new IOException("invalid public provider label resource: " + exception.getMessage(), exception);
        }
    }

    /**
     * Finds the reviewed public label for an internal source id.
     *
     * @param sourceId internal source id (raw {@code datasourceName})
     * @return the reviewed label, or empty when none was approved - callers
     *     must treat an empty result as an omission, never fall back to the
     *     internal source id
     */
    public Optional<PublicProviderLabel> find(String sourceId) {
        return sourceId == null ? Optional.empty() : Optional.ofNullable(labelsBySourceId.get(sourceId));
    }

    /**
     * Resolves a caller-supplied public provider selector back to the internal source id.
     *
     * <p>Callers of a public API must never be able to select a provider by its internal source
     * id, only by the reviewed public label - this is the reverse of {@link #find(String)}, used
     * to translate an inbound {@code provider} filter before it reaches an internal query.
     *
     * @param publicLabel exact reviewed public label, as returned by {@link #find(String)}
     * @return the internal source id carrying this label, or empty when no reviewed entry matches
     */
    public Optional<String> findSourceId(String publicLabel) {
        if (publicLabel == null) {
            return Optional.empty();
        }
        return labelsBySourceId.values().stream()
                .filter(label -> label.label().equals(publicLabel))
                .map(PublicProviderLabel::sourceId)
                .findFirst();
    }
}
