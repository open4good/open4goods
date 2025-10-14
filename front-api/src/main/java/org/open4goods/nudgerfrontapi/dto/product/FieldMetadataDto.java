package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Objects;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata describing a field that can be used in product queries.
 */
public record FieldMetadataDto(
        @Schema(description = "Stable identifier of the field as exposed by the API.", example = "price")
        String id,
        @Schema(description = "Path of the field in the product document.", example = "price.minPrice.price")
        String mapping,
        @Schema(description = "Localised display name for the field when available.", example = "Prix minimum", nullable = true)
        String title,
        @Schema(description = "Localised description for the field when available.", example = "Prix minimum observé pour le produit", nullable = true)
        String description,
        @Schema(description = "Optional aggregation definition used to render charts or default controls.", nullable = true)
        AggregationFieldDefinitionDto definition
) {

    public FieldMetadataDto withDefinition(AggregationFieldDefinitionDto updatedDefinition) {
        if (Objects.equals(this.definition, updatedDefinition)) {
            return this;
        }
        return new FieldMetadataDto(id, mapping, title, description, updatedDefinition);
    }
}
