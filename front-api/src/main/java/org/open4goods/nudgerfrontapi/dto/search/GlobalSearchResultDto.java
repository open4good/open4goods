package org.open4goods.nudgerfrontapi.dto.search;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single product hit returned by the global search endpoint.
 */
public record GlobalSearchResultDto(
        @Schema(description = "Product GTIN uniquely identifying the hit", example = "7612345678901")
        Long gtin,
        @Schema(description = "Identifier of the vertical attached to the product, when available", example = "smartphones", nullable = true)
        String verticalId,
        @Schema(description = "Display title resolved for the product", example = "Fairphone 4 128 Go")
        String title,
        @Schema(description = "Brand extracted from referential attributes", example = "Fairphone", nullable = true)
        String brand,
        @Schema(description = "Model extracted from referential attributes", example = "Fairphone 4", nullable = true)
        String model,
        @Schema(description = "Number of active offers for the product", example = "12", nullable = true)
        Integer offersCount,
        @Schema(description = "Pertinence score returned by Elasticsearch", example = "7.42")
        double score) {
}
