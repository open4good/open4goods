package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Indexed attribute representation used by the frontend.
 */
public record ProductIndexedAttributeDto(
        @Schema(description = "Attribute name", example = "color")
        String name,
        @Schema(description = "Attribute value", example = "black")
        String value,
        @Schema(description = "Numeric interpretation when available", nullable = true, example = "12.5")
        Double numericValue,
        @Schema(description = "Boolean interpretation when available", nullable = true)
        Boolean booleanValue
) {
}
