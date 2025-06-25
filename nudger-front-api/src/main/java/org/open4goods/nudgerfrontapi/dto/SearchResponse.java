package org.open4goods.nudgerfrontapi.dto;

/**
 * Result of a product search query.
 */
import java.util.List;

import org.open4goods.model.product.Product;
import io.swagger.v3.oas.annotations.media.Schema;

public record SearchResponse(
        @Schema(description = "Total number of matching products", example = "42")
        long total,

        @Schema(description = "Current page index (1-based)", example = "1")
        int page,

        @Schema(description = "Page size", example = "20")
        int size,

        @Schema(description = "Page content")
        List<Product> items) {
}
