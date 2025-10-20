package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Ranking information derived from ecoscore comparisons.
 */
public record ProductRankingDto(
        @Schema(description = "Global ranking position", example = "120")
        Long globalPosition,
        @Schema(description = "Number of items considered in the global ranking", example = "5000")
        Long globalCount,
        @Schema(description = "Details about the best ranked product", implementation = ProductReferenceDto.class, nullable = true)
        ProductReferenceDto globalBest,
        @Schema(description = "Details about the product ranked immediately above", implementation = ProductReferenceDto.class, nullable = true)
        ProductReferenceDto globalBetter,
        @Schema(description = "Specialised ranking position", example = "12")
        Long specializedPosition,
        @Schema(description = "Number of items considered in the specialised ranking", example = "120")
        Long specializedCount,
        @Schema(description = "Identifier of the best product in the specialised ranking", nullable = true)
        String specializedBest,
        @Schema(description = "Identifier of the product ranked immediately above in the specialised ranking", nullable = true)
        String specializedBetter
) {
}
