package org.open4goods.nudgerfrontapi.dto;

/**
 * Frontend view of a product.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductView(
        @Schema(description = "Original request")
        ProductViewRequest request,

        @Schema(description = "Timing metadata", nullable = true)
        RequestMetadata metadatas,

        @Schema(description = "Requested GTIN", example = "7612345678901")
        long gtin) {

    /**
     * Convenience constructor keeping existing usage.
     */
    public ProductView(ProductViewRequest request) {
        this(request, null, 0);
    }
}
