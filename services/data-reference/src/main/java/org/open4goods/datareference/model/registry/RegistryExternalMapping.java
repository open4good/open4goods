package org.open4goods.datareference.model.registry;

import java.util.Objects;

import org.open4goods.datareference.model.CanonicalConceptId;

/**
 * A reviewed external coordinate together with the stable O4G concept it names.
 *
 * @param conceptId independently authored canonical class or attribute
 * @param mapping approved, effective-date-bounded mapping
 */
public record RegistryExternalMapping(CanonicalConceptId conceptId, ExternalMapping mapping) {

    /**
     * Prevents proposed or rejected coordinates from reaching a resolver.
     */
    public RegistryExternalMapping {
        Objects.requireNonNull(conceptId, "conceptId must not be null");
        Objects.requireNonNull(mapping, "mapping must not be null");
        if (mapping.status() != ExternalMappingStatus.REVIEWED) {
            throw new IllegalArgumentException("only reviewed mappings are resolvable");
        }
    }
}
