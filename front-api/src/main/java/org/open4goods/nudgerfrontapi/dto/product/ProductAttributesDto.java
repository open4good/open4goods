package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;
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
        @Schema(description = "Attributes classified using Icecat feature groups hierarchy")
        List<ProductClassifiedAttributeGroupDto> classifiedAttributes,
        @Schema(description = "All raw attributes keyed by their identifier")
        Map<String, ProductAttributeDto> allAttributes
) {
}
