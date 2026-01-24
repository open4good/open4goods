package org.open4goods.nudgerfrontapi.model;

import java.time.Instant;

/**
 * Represents a single URL awaiting submission to the Google Indexing API.
 *
 * @param url          absolute URL to index
 * @param createdAt    timestamp of the enqueue action
 * @param lastAttemptAt timestamp of the most recent submission attempt
 * @param attemptCount number of submission attempts performed so far
 */
public record GoogleIndexingQueueItem(
        String url,
        Instant createdAt,
        Instant lastAttemptAt,
        int attemptCount) {
}
