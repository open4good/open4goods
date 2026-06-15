package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Coverage status for one requested response facet.
 *
 * @param id facet identifier
 * @param covered true when the product has enough data for this facet
 */
@Schema(description = "Coverage status for one requested response facet.")
public record B2bCoverageMeta(
        @Schema(description = "Facet identifier.", example = "price")
        String id,
        @Schema(description = "Whether the product has enough data for this facet.", example = "true")
        boolean covered) {
}
