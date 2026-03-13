package org.open4goods.api.dto.pricealert;

/**
 * Response returned by the product-alert service after batch ingestion.
 *
 * @param receivedEvents received events count
 * @param matchedSubscriptions matched subscriptions count
 * @param createdCandidates created candidate count
 */
public record PriceEventsIngestionResponse(
        int receivedEvents,
        int matchedSubscriptions,
        int createdCandidates)
{
}
