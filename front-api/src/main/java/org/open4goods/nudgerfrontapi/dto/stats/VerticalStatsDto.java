package org.open4goods.nudgerfrontapi.dto.stats;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing detailed statistics for a specific vertical.
 */
public record VerticalStatsDto(
        @Schema(description = "Total number of products in this vertical (including excluded).", example = "1000")
        long totalProducts,

        @Schema(description = "Number of excluded products in this vertical.", example = "50")
        long excludedProducts,

        @Schema(description = "Number of valid products (active, with offers) in this vertical.", example = "800")
        long validProducts,

        @Schema(description = "Number of rated products (having an impact score) in this vertical.", example = "700")
        long ratedProducts,

        @Schema(description = "Number of reviewed products (having an AI description) in this vertical.", example = "600")
        long reviewedProducts
) { }
