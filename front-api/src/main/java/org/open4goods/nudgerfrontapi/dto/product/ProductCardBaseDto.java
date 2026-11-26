package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Base facet of a {@link ProductCardDto}: the subset of {@link ProductBaseDto} read by product cards.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardBaseDto(
        @Schema(description = "Product GTIN", example = "7612345678901")
        long gtin,
        @Schema(description = "Vertical identifier", example = "tv", nullable = true)
        String vertical,
        @Schema(description = "Best computed product name", example = "PHILIPS 40PFS6009/12", nullable = true)
        String bestName,
        @Schema(description = "Public URL of the cover image", format = "uri", nullable = true)
        String coverImagePath
) {
}
