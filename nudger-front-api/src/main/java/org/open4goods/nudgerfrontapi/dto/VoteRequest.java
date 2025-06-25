package org.open4goods.nudgerfrontapi.dto;

/**
 * Incoming vote payload.
 */
import io.swagger.v3.oas.annotations.media.Schema;

public record VoteRequest(
        @Schema(description = "Target item identifier", example = "post-123")
        String itemId,

        @Schema(description = "Vote value", example = "1")
        int value) {
}
