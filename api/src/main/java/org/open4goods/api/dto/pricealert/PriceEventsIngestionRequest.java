package org.open4goods.api.dto.pricealert;

import java.util.List;

/**
 * Outbound batch request for price-drop event publication.
 *
 * @param events events to publish
 */
public record PriceEventsIngestionRequest(List<InternalPriceEventDto> events)
{
}
