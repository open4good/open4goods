package org.open4goods.nudgerfrontapi.dto.product;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Container exposing the ordered lifecycle events for a product.
 */
public record ProductTimelineDto(
        @Schema(description = "Chronologically sorted lifecycle events")
        List<ProductTimelineEventDto> events
) {
}
