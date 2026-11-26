package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Image of a {@link ProductCardDto}: the optimised URL and its original variant.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardImageDto(
        @Schema(description = "Optimised (WebP) image URL", format = "uri", nullable = true)
        String url,
        @Schema(description = "Original image URL", format = "uri", nullable = true)
        String originalUrl
) {
}
