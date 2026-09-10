package org.open4goods.datareference.model.registry;

import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.CanonicalClassId;

/**
 * What the O4G registry says one canonical product class is.
 *
 * @param id stable canonical class identifier
 * @param parent parent class, or {@code null} for a root class
 * @param attributes attributes this class declares, in registry order
 */
public record CanonicalClassDefinition(
        CanonicalClassId id,
        CanonicalClassId parent,
        List<CanonicalAttributeId> attributes) {

    /**
     * Validates the class definition.
     */
    public CanonicalClassDefinition {
        Objects.requireNonNull(id, "id must not be null");
        attributes = List.copyOf(Objects.requireNonNull(attributes, "attributes must not be null"));
        if (id.equals(parent)) {
            throw new IllegalArgumentException("a class must not be its own parent: " + id);
        }
    }
}
