package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.nudgerfrontapi.dto.RequestMetadata;

/**
 * Frontend view of a product.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record ProductDto(


        @Schema(description = "Timing metadata", nullable = true)
        RequestMetadata metadatas,

        @Schema(description = "Requested GTIN", example = "7612345678901")
        long gtin) {


}
