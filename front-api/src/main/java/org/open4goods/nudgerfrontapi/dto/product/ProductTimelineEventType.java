package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Enumeration describing every milestone exposed through the product timeline.
 */
@Schema(description = "Categorises lifecycle events so the frontend can display contextual labels", enumAsRef = true)
public enum ProductTimelineEventType {
    /** First time the product was observed in new condition. */
    @Schema(description = "First new offer detected in the price history")
    PRICE_FIRST_SEEN_NEW,
    /** First time the product was observed as occasion. */
    @Schema(description = "First second-hand offer detected in the price history")
    PRICE_FIRST_SEEN_OCCASION,
    /** Most recent new offer. */
    @Schema(description = "Most recent new offer recorded in the price history")
    PRICE_LAST_SEEN_NEW,
    /** Most recent second-hand offer. */
    @Schema(description = "Most recent second-hand offer recorded in the price history")
    PRICE_LAST_SEEN_OCCASION,
    /** EPREL market entry. */
    @Schema(description = "EPREL declared on-market start date")
    EPREL_ON_MARKET_START,
    /** EPREL market exit. */
    @Schema(description = "EPREL declared on-market end date")
    EPREL_ON_MARKET_END,
    /** EPREL first on-market start. */
    @Schema(description = "EPREL first on-market start date")
    EPREL_ON_MARKET_FIRST_START,
    /** EPREL first publication. */
    @Schema(description = "EPREL first publication date")
    EPREL_FIRST_PUBLICATION,
    /** EPREL last publication. */
    @Schema(description = "EPREL most recent publication date")
    EPREL_LAST_PUBLICATION,
    /** EPREL export timestamp. */
    @Schema(description = "EPREL export timestamp")
    EPREL_EXPORT,
    /** EPREL spare parts availability end. */
    @Schema(description = "EPREL calculated end date for spare parts availability")
    EPREL_SPARE_PARTS_END,
    /** EPREL software updates support end. */
    @Schema(description = "EPREL calculated end date for software updates availability")
    EPREL_SOFTWARE_SUPPORT_END,
    /** EPREL guaranteed support end. */
    @Schema(description = "EPREL calculated end date for overall technical support")
    EPREL_SUPPORT_END,
    /** Internal import timestamp. */
    @Schema(description = "Date when the EPREL entry was imported")
    EPREL_IMPORTED,
    /** Organisation closure event. */
    @Schema(description = "EPREL organisation close date")
    EPREL_ORGANISATION_CLOSED
}
