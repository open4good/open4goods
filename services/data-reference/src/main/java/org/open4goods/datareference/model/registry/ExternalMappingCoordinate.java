package org.open4goods.datareference.model.registry;

import java.util.Objects;

/**
 * Stable key of one external taxonomy or provider coordinate.
 *
 * @param system provider-neutral mapping system
 * @param externalId opaque provider coordinate
 */
public record ExternalMappingCoordinate(String system, String externalId) {

    /**
     * Normalizes the coordinate through the same validation as an authored mapping.
     */
    public ExternalMappingCoordinate {
        ExternalMapping validated = new ExternalMapping(system, externalId, ExternalMappingStatus.PROPOSED,
                java.time.LocalDate.of(1970, 1, 1), null);
        system = validated.system();
        externalId = validated.externalId();
        Objects.requireNonNull(system, "system must not be null");
    }
}
