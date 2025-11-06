package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Representation of a general product attribute.
 */
public record ProductAttributeDto(
        @Schema(description = "Attribute name", example = "screen_size")
        String name,
        @Schema(description = "Attribute value", example = "55")
        String value,
        @Schema(description = "Icecat taxonomy identifiers associated to the attribute")
        Set<Integer> icecatTaxonomyIds,
        @Schema(description = "Sourcing metadata for this attribute, including contributing datasources")
        ProductAttributeSourceDto sourcing
) {
}
