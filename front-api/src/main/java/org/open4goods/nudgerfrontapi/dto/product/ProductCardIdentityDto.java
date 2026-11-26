package org.open4goods.nudgerfrontapi.dto.product;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Identity facet of a {@link ProductCardDto}: brand, model and best name used by card titles and table columns.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductCardIdentityDto(
        @Schema(description = "Brand", example = "PHILIPS", nullable = true)
        String brand,
        @Schema(description = "Model", example = "40PFS6009/12", nullable = true)
        String model,
        @Schema(description = "Best computed product name", example = "PHILIPS 40PFS6009/12", nullable = true)
        String bestName
) {
}
