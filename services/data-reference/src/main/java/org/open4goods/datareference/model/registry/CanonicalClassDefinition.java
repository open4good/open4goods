package org.open4goods.datareference.model.registry;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;

/**
 * What the O4G registry says one canonical product class is.
 *
 * @param id stable canonical class identifier
 * @param lifecycle lifecycle state controlled through Git review
 * @param labels mandatory English and French editorial labels
 * @param parent parent class, or {@code null} for a root class
 * @param attributes attributes this class declares, in registry order
 * @param mappings reviewed provider mappings, ordered for deterministic review
 */
public record CanonicalClassDefinition(
        CanonicalClassId id,
        RegistryLifecycle lifecycle,
        Map<String, String> labels,
        CanonicalClassId parent,
        List<CanonicalAttributeId> attributes,
        List<ExternalMapping> mappings) {

    /**
     * Validates the class definition.
     */
    public CanonicalClassDefinition {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(lifecycle, "lifecycle must not be null");
        labels = RegistryText.requireTranslations(labels, "class " + id);
        attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        if (attributes.stream().distinct().count() != attributes.size()) {
            throw new IllegalArgumentException("a class cannot declare an attribute twice: " + id);
        }
        mappings = List.copyOf(Objects.requireNonNull(mappings, "mappings must not be null"));
        RegistryText.requireUniqueMappings(mappings, "class " + id);
        if (id.equals(parent)) {
            throw new IllegalArgumentException("a class must not be its own parent: " + id);
        }
    }
}
