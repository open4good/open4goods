package org.open4goods.datareference.model.registry;

import java.util.List;
import java.util.Objects;

/**
 * Complete versioned registry document authored and reviewed in Git.
 *
 * @param schemaVersion immutable schema identifier used to validate the JSON resource
 * @param registryVersion monotonically increasing semantic registry version
 * @param classes canonical product classes
 * @param attributes canonical product attributes
 * @param verticalViews editorial vertical views over explicitly included O4G classes
 */
public record RegistryDocument(
        String schemaVersion,
        RegistryVersion registryVersion,
        List<CanonicalClassDefinition> classes,
        List<CanonicalAttributeDefinition> attributes,
        List<RegistryVerticalView> verticalViews) {

    /** Schema identifier for the registry JSON resource. */
    public static final String SCHEMA_VERSION = "https://open4goods.org/schema/o4g-registry-1.json";

    /**
     * Validates the fixed schema version and takes immutable snapshots of lists.
     */
    public RegistryDocument {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported registry schema: " + schemaVersion);
        }
        Objects.requireNonNull(registryVersion, "registryVersion must not be null");
        classes = List.copyOf(Objects.requireNonNull(classes, "classes must not be null"));
        attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        verticalViews = List.copyOf(Objects.requireNonNull(verticalViews, "verticalViews must not be null"));
    }
}
