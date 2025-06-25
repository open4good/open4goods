package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Image URLs for a product.
 */
public record ProductImagesDto(
        @Schema(description = "Ordered image URLs", example = "['https://example.com/img.jpg']")
        List<String> urls) {}
