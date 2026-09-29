package org.open4goods.b2bapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Mandatory attribution block for facets serving EPREL-sourced content, per the EPREL API Terms
 * and Conditions 4 3.
 *
 * @param source human-readable name of the origin database
 * @param sourceUrl link to the EPREL public database
 * @param notice usage notice restating the free-of-charge, account-gated access rule
 */
@Schema(description = "Mandatory attribution for EPREL-sourced content (EPREL API Terms and Conditions 4 3).")
public record B2bEprelAttributionDto(
        @Schema(description = "Human-readable name of the origin database.", example = "EPREL - European Product Registry for Energy Labelling")
        String source,
        @Schema(description = "Link to the EPREL public database.", example = "https://eprel.ec.europa.eu")
        String sourceUrl,
        @Schema(description = "Usage notice: this facet is free of charge for any authenticated account and never billed.",
                example = "Data sourced from the EU EPREL database. Free of charge for any authenticated account; never billed.")
        String notice) {
}
