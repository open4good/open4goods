package org.open4goods.datareference.model.registry;

import java.util.Objects;

/**
 * Verifiable immutable result of importing one authored registry revision.
 *
 * @param registry read-only runtime lookup
 * @param contentHash SHA-256 of the exact Git resource bytes
 * @param classCount number of indexed classes
 * @param attributeCount number of indexed attributes
 */
public record RegistryRuntimeIndex(
        InMemoryCanonicalRegistry registry,
        String contentHash,
        int classCount,
        int attributeCount) {

    /**
     * Ensures an importer cannot report counts different from the installed index.
     */
    public RegistryRuntimeIndex {
        Objects.requireNonNull(registry, "registry must not be null");
        if (contentHash == null || !contentHash.matches("[0-9a-f]{64}")) {
            throw new IllegalArgumentException("contentHash must be a lower-case SHA-256 digest");
        }
        if (classCount != registry.classCount() || attributeCount != registry.attributeCount()) {
            throw new IllegalArgumentException("runtime index counts must match the installed registry");
        }
    }
}
