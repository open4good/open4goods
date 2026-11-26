package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Resources facet of a {@link ProductCardDto}: the cover image and the first product image, used as fallbacks
 * by cards. Videos, PDFs and other images stay on the product page.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardResourcesDto(
        @Schema(description = "Public URL of the cover image", format = "uri", nullable = true)
        String coverImagePath,
        @Schema(description = "External cover image URL used when no cover is cached", format = "uri", nullable = true)
        String externalCover,
        @Schema(description = "First product image, empty when the product has none")
        List<ProductCardImageDto> images
) {
}
