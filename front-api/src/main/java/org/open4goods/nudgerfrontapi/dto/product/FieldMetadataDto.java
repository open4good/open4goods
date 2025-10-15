package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata describing a field that can be used in product queries.
 */
public record FieldMetadataDto(
        @Schema(description = "Path of the field in the product document acting as its identifier.", example = "price.minPrice.price")
        String mapping,
        @Schema(description = "Localised display name for the field when available.", example = "Prix minimum", nullable = true)
        String title,
        @Schema(description = "Localised description for the field when available.", example = "Prix minimum observé pour le produit", nullable = true)
        String description,
        @Schema(description = "Type of values accepted by the field.", example = "numeric", allowableValues = {"text", "numeric"})
        String valueType
) {
}
