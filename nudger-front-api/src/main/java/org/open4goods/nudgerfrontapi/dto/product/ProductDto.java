package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.nudgerfrontapi.dto.RequestMetadata;
import com.fasterxml.jackson.annotation.JsonFilter;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Frontend view of a product. Components are optional and can be included on
 * demand by the frontend.
 */
@JsonFilter("inc")
public record ProductDto(
        @Schema(description = "Product GTIN, unique identifier", example = "7612345678901")
        long gtin,

        @Schema(description = "AI review component", example = "{}", nullable = true)
        ProductAiReviewDto aiReview,

        @Schema(description = "Offers component", example = "{}", nullable = true)
        ProductOffersDto offers,

        @Schema(description = "Images component", example = "{}", nullable = true)
        ProductImagesDto images,

        @Schema(description = "Timing metadata", example = "null", nullable = true)
        RequestMetadata metadatas) {

    public enum ProductDtoComponent {
        aiReview,
        offers,
        images
    }
}
