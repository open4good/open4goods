package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Indexed attribute of a {@link ProductCardDto}, without the sourcing details of
 * {@link ProductIndexedAttributeDto}.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardAttributeDto(
        @Schema(description = "Localised attribute name", example = "Diagonale (en pouces)", nullable = true)
        String name,
        @Schema(description = "Attribute value", example = "40", nullable = true)
        String value,
        @Schema(description = "Numeric interpretation when available", example = "40", nullable = true)
        Double numericValue
) {
}
