package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metering details for one response facet.
 *
 * @param id facet identifier
 * @param credits credit price for the facet
 * @param served true when the facet was returned in the response
 * @param billable true when serving this facet consumed credits
 */
@Schema(description = "Metering details for one response facet.")
public record B2bFacetMeta(
        @Schema(description = "Facet identifier.", example = "price")
        String id,
        @Schema(description = "Credit price for this facet.", example = "1")
        long credits,
        @Schema(description = "Whether this facet was returned in the response.", example = "true")
        boolean served,
        @Schema(description = "Whether serving this facet consumed credits.", example = "true")
        boolean billable) {
}
