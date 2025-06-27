package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.product.ExternalIds;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Basic product metadata facet.
 */
public record ProductBaseDto(
        @Schema(description = "Product GTIN", example = "7612345678901")
        long gtin,
        @Schema(description = "Creation timestamp", example = "1693000000000")
        long creationDate,
        @Schema(description = "Last change timestamp", example = "1693590000000")
        long lastChange,
        @Schema(description = "Associated vertical", example = "electronics")
        String vertical,
        @Schema(description = "External identifiers")
        ExternalIds externalIds,
        @Schema(description = "Google taxonomy identifier", example = "123")
        Integer googleTaxonomyId
) {}
