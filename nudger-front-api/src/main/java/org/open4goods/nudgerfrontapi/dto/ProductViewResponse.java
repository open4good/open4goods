package org.open4goods.nudgerfrontapi.dto;

/**
 * Frontend view of a product.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductViewResponse(
        @Schema(description = "Original request")
        ProductViewRequest request,

        @Schema(description = "Timing metadata")
        RequestMetadata metadatas,

        @Schema(description = "Requested GTIN", example = "7612345678901")
        long gtin) {

    /**
     * Convenience constructor keeping existing usage.
     */
    public ProductViewResponse(ProductViewRequest request) {
        this(request, null, 0);
    }
}
