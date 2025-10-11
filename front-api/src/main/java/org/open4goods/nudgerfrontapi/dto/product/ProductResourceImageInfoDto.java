package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Image metadata exported with the resource facet.
 */
public record ProductResourceImageInfoDto(
        @Schema(description = "Image height in pixels", example = "1080")
        Integer height,
        @Schema(description = "Image width in pixels", example = "1920")
        Integer width,
        @Schema(description = "Perceptual hash value when available")
        Long pHashValue,
        @Schema(description = "Number of bits used for the perceptual hash")
        Integer pHashLength
) {
}
