package org.open4goods.nudgerfrontapi.service.dto;

import java.time.Duration;
import java.time.Instant;

/**
 * Internal metrics snapshot for Google Indexing activity.
 *
 * @param enabled           whether indexing is enabled
 * @param pendingCount      number of queued URLs
 * @param indexedCount      number of URLs recently indexed
 * @param deadLetterCount   number of URLs dropped after max attempts
 * @param lastSuccessAt     timestamp of last success
 * @param lastFailureAt     timestamp of last failure
 * @param batchSize         configured batch size
 * @param retryDelay        configured retry delay
 * @param maxAttempts       configured max attempts
 * @param realtimeEnabled   whether realtime processing is enabled
 */
public record GoogleIndexingMetrics(
        boolean enabled,
        int pendingCount,
        int indexedCount,
        int deadLetterCount,
        Instant lastSuccessAt,
        Instant lastFailureAt,
        int batchSize,
        Duration retryDelay,
        int maxAttempts,
        boolean realtimeEnabled) {
}
