package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Exposes the various external identifiers attached to a product.
 */
public record ProductExternalIdsDto(
        @Schema(description = "Amazon Standard Identification Number", example = "B012345678")
        String asin,
        @Schema(description = "Icecat identifier", example = "12345")
        String icecat,
        @Schema(description = "Known manufacturer part numbers")
        Set<String> mpn,
        @Schema(description = "Known stock keeping units")
        Set<String> sku
) {
}
