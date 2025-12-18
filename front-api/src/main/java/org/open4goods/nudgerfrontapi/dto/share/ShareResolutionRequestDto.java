package org.open4goods.nudgerfrontapi.dto.share;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Payload accepted by the share resolution endpoint.
 */
public record ShareResolutionRequestDto(
        @Schema(description = "Product URL shared from a merchant site", required = true,
                example = "https://shop.example.org/product/fairphone-4")
        String url,
        @Schema(description = "Optional title forwarded by the share sheet", example = "Great phone")
        String title,
        @Schema(description = "Optional free-form text forwarded by the share sheet", example = "Worth checking this out")
        String text
) {
}
