package org.open4goods.nudgerfrontapi.dto.product;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Represents a single AI generated text with its timestamp.
 */
public record ProductAiDescriptionDto(
        @Schema(description = "Timestamp in epoch milliseconds when the description was generated")
        long timestamp,
        @Schema(description = "Generated textual content")
        String content
) {
}
