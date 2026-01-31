package org.open4goods.services.googleindexation.dto;

import java.time.Instant;

/**
 * Per-URL result details for a Google Indexing API request.
 *
 * @param url URL submitted to the Indexing API
 * @param success whether the publish call succeeded
 * @param message optional error or response message
 * @param finishedAt timestamp when the URL was processed
 */
public record GoogleIndexationResultItem(
        String url,
        boolean success,
        String message,
        Instant finishedAt
) {
}
