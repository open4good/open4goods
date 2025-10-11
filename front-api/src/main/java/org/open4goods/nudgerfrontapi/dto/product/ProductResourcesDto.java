package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Media resources associated with the product.
 */
public record ProductResourcesDto(
        @Schema(description = "Images grouped and ranked by relevance")
        List<ProductResourceDto> images,
        @Schema(description = "Video resources extracted from datasources")
        List<ProductResourceDto> videos,
        @Schema(description = "PDF resources extracted from datasources")
        List<ProductResourceDto> pdfs,
        @Schema(description = "External cover automatically computed from resources", example = "https://cdn.example.org/cover.jpg")
        String externalCover
) {
}
