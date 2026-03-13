package org.open4goods.services.productalert.dto;

/**
 * Counters returned after internal price-event ingestion.
 *
 * @param receivedEvents number of received events
 * @param matchedSubscriptions number of matched subscriptions
 * @param createdCandidates number of created candidates
 */
public record PriceEventsIngestionResponse(
        int receivedEvents,
        int matchedSubscriptions,
        int createdCandidates)
{
}
