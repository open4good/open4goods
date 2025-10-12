package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Media resources associated with the product.
 */
public record ProductResourcesDto(
        @Schema(description = "Images grouped and ranked by relevance")
        List<ProductImageDto> images,
        @Schema(description = "Video resources extracted from datasources")
        List<ProductVideoDto> videos,
        @Schema(description = "PDF resources extracted from datasources")
        List<ProductPdfDto> pdfs,
        @Schema(description = "Absolute URL of the preferred cover image when available", example = "https://cdn.example.org/covers/123.jpg")
        String coverImagePath,
        @Schema(description = "External cover automatically computed from resources", example = "https://cdn.example.org/cover.jpg")
        String externalCover
) {
}
