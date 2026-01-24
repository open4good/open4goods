package org.open4goods.nudgerfrontapi.dto;

import java.time.Duration;
import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO representing Google Indexing queue metrics exposed to the frontend.
 */
public record GoogleIndexingMetricsDto(
        @Schema(description = "Whether Google Indexing is enabled")
        boolean enabled,
        @Schema(description = "Number of URLs waiting to be submitted")
        int pendingCount,
        @Schema(description = "Number of URLs recently indexed")
        int indexedCount,
        @Schema(description = "Number of URLs dropped after exceeding max attempts")
        int deadLetterCount,
        @Schema(description = "Timestamp of the last successful submission", format = "date-time")
        Instant lastSuccessAt,
        @Schema(description = "Timestamp of the last failed submission", format = "date-time")
        Instant lastFailureAt,
        @Schema(description = "Configured batch size", example = "50")
        int batchSize,
        @Schema(description = "Delay between retries", example = "PT30M")
        Duration retryDelay,
        @Schema(description = "Maximum retry attempts", example = "5")
        int maxAttempts,
        @Schema(description = "Whether realtime processing is enabled")
        boolean realtimeEnabled) {
}
