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
        @Schema(description = "GTIN of the best ranked product in the global ranking", nullable = true)
        Long globalBest,
        @Schema(description = "GTIN of the product ranked immediately above", nullable = true)
        Long globalBetter,
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
