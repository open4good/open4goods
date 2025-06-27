package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Media resources associated with a product.
 */
public record ProductResourcesDto(
        @Schema(description = "Image URLs", type = "array")
        List<String> images,
        @Schema(description = "Video URLs", type = "array")
        List<String> videos,
        @Schema(description = "PDF URLs", type = "array")
        List<String> pdfs,
        @Schema(description = "Cover image URL")
        String coverPath
) {}
