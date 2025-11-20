package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Map;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Detailed representation of a score attached to the product.
 */
public record ProductScoreDto(
        @Schema(description = "Score id", example = "ENERGY")
        String id,
        @Schema(description = "Score name", example = "Classe energetique")
        String name,
        @Schema(description = "Score description", example = "La classe énergetique de l'objet")
        String description,
        @Schema(description = "Whether the score is virtual")
        Boolean virtual,
        @Schema(description = "Raw score value", nullable = true)
        Double value,
        @Schema(description = "Absolute cardinality information", nullable = true)
        ProductCardinalityDto absolute,
        @Schema(description = "Relative cardinality information", nullable = true)
        ProductCardinalityDto relativ,
        @Schema(description = "Additional metadata")
        Map<String, String> metadatas,
        @Schema(description = "Ranking of the product for this score", nullable = true)
        Integer ranking,
        @Schema(description = "Percentage representation of the score on a 0-100 scale", nullable = true)
        Long percent,
        @Schema(description = "Score scaled on 0-20", nullable = true)
        Long on20,
        @Schema(description = "Absolute value formatted as text", nullable = true)
        String absoluteValue,
        @Schema(description = "Relative value formatted as text", nullable = true)
        String relativeValue,
        @Schema(description = "Letter grade representation", nullable = true)
        String letter
) {
}
