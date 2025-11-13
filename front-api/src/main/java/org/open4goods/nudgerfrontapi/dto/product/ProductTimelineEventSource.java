package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Origin of a timeline event to help the frontend convey context.
 */
@Schema(description = "Origin datasource that produced the timeline event", enumAsRef = true)
public enum ProductTimelineEventSource {
    /** Price history derived from aggregated offers. */
    @Schema(description = "Event built from the historical price chart")
    PRICE_HISTORY,
    /** EPREL registry milestone. */
    @Schema(description = "Event published by the EPREL registry")
    EPREL
}
