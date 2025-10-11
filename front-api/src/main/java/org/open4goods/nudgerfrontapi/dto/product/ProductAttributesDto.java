package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Aggregated attributes detected for the product.
 */
public record ProductAttributesDto(
        @Schema(description = "Referential attributes keyed by the official attribute key")
        Map<String, String> referentialAttributes,
        @Schema(description = "Indexed attributes keyed by their identifier")
        Map<String, ProductIndexedAttributeDto> indexedAttributes,
        @Schema(description = "All attributes keyed by their identifier")
        Map<String, ProductAttributeDto> allAttributes,
        @Schema(description = "Concatenated human readable characteristics")
        String characteristics
) {
}
