package org.open4goods.nudgerfrontapi.dto.product;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Basic product metadata facet aggregating immutable identifiers and lifecycle
 * timestamps.
 */
public record ProductBaseDto(
        @Schema(description = "Product GTIN as stored in the catalogue", example = "7612345678901")
        long gtin,
        @Schema(description = "Creation timestamp expressed in epoch milliseconds", example = "1693000000000")
        long creationDate,
        @Schema(description = "Last modification timestamp expressed in epoch milliseconds", example = "1693590000000")
        long lastChange,
        @Schema(description = "Associated vertical identifier", example = "electronics")
        String vertical,
        @Schema(description = "External identifiers assigned to the product")
        ProductExternalIdsDto externalIds,
        @Schema(description = "Google taxonomy identifier", example = "123")
        Integer googleTaxonomyId,
        @Schema(description = "Whether the product is excluded from vertical representation")
        boolean excluded,
        @Schema(description = "Reasons explaining why the product is excluded")
        Set<String> excludedCauses,
        @Schema(description = "Information inferred from the GTIN itself")
        ProductGtinInfoDto gtinInfo
) {
}
