package org.open4goods.services.googleindexation.dto;

import java.time.Instant;
import java.util.List;

/**
 * Result returned after attempting to publish URLs to the Google Indexing API.
 *
 * @param totalCount total number of URLs submitted
 * @param successCount number of URLs successfully published
 * @param failureCount number of URLs that failed to publish
 * @param finishedAt timestamp of the batch completion
 * @param items per-url details for the batch
 */
public record GoogleIndexationResult(
        int totalCount,
        int successCount,
        int failureCount,
        Instant finishedAt,
        List<GoogleIndexationResultItem> items
) {
}
