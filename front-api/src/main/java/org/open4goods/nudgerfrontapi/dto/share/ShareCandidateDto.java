package org.open4goods.nudgerfrontapi.dto.share;

import org.open4goods.nudgerfrontapi.dto.product.ProductAggregatedPriceDto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Candidate returned for a share resolution request.
 */
public record ShareCandidateDto(
        @Schema(description = "Identifier of the product (GTIN or slug)", example = "7612345678901")
        String productId,
        @Schema(description = "Candidate display name", example = "Fairphone 4")
        String name,
        @Schema(description = "Preferred image for the candidate", format = "uri", example = "https://cdn.example.org/image.jpg")
        String image,
        @Schema(description = "Eco score when available", example = "15.2")
        Double ecoScore,
        @Schema(description = "Environmental impact score when available", example = "12.5")
        Double impactScore,
        @Schema(description = "Best indexed price without triggering live scraping", implementation = ProductAggregatedPriceDto.class)
        ProductAggregatedPriceDto bestPrice,
        @Schema(description = "Confidence score associated to the match", example = "0.86")
        Double confidence
) {
}
