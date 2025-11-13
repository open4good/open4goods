package org.open4goods.nudgerfrontapi.dto.product;

import org.open4goods.model.product.ProductCondition;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Single event on the lifecycle timeline.
 */
public record ProductTimelineEventDto(
        @Schema(description = "Epoch milliseconds describing when the event occurred", example = "1711929600000")
        Long timestamp,
        @Schema(description = "Type of lifecycle event", implementation = ProductTimelineEventType.class)
        ProductTimelineEventType type,
        @Schema(description = "Source system where the event originates", implementation = ProductTimelineEventSource.class)
        ProductTimelineEventSource source,
        @Schema(description = "Product condition associated with the event when relevant")
        ProductCondition condition,
        @Schema(description = "Recorded price when the event originates from price history", example = "249.99")
        Double price
) {
}
