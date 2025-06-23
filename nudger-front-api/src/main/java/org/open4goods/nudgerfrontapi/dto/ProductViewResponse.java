package org.open4goods.nudgerfrontapi.dto;

/**
 * Frontend view of a product.
 */
public record ProductViewResponse(ProductViewRequest request,
                                  RequestMetadata metadatas,
                                  long gtin) {

    /**
     * Convenience constructor keeping existing usage.
     */
    public ProductViewResponse(ProductViewRequest request) {
        this(request, null, 0);
    }
}
