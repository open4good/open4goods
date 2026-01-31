package org.open4goods.nudgerfrontapi.dto.indexation;

import java.time.Instant;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO exposing Google Indexation queue metrics.
 */
public record GoogleIndexationMetricsDto(
        @Schema(description = "Whether Google indexation dispatch is enabled", example = "false")
        boolean enabled,
        @Schema(description = "Number of URLs currently stored in the queue", example = "12")
        int queuedCount,
        @Schema(description = "Number of URLs pending processing", example = "5")
        int pendingCount,
        @Schema(description = "Number of URLs that failed during the last attempts", example = "2")
        int failedCount,
        @Schema(description = "Timestamp of the last successful dispatch", example = "2024-01-01T12:00:00Z")
        Instant lastSuccessAt,
        @Schema(description = "Timestamp of the last failed dispatch", example = "2024-01-01T12:05:00Z")
        Instant lastFailureAt,
        @Schema(description = "Last failure message, if any", example = "HTTP 429: quota exceeded")
        String lastFailureMessage
) {
}
