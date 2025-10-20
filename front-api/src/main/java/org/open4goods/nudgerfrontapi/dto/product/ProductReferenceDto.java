package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight representation of a related product used in ranking sections.
 */
public record ProductReferenceDto(
        @Schema(description = "GTIN identifier of the referenced product", example = "7612345678901")
        Long id,
        @Schema(description = "Fully qualified slug pointing to the product page", example = "/telephones-reconditionnes/fairphone-4", nullable = true)
        String fullSlug,
        @Schema(description = "Best resolved product name", example = "Fairphone 4", nullable = true)
        String bestName,
        @Schema(description = "Brand associated with the product", example = "Fairphone", nullable = true)
        String brand,
        @Schema(description = "Model identifier exposed by the datasource", example = "FP4", nullable = true)
        String model
) {
}
