package org.open4goods.services.productalert.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * Batch request for internal price-event ingestion.
 *
 * @param events price events to process
 */
public record PriceEventsIngestionRequest(
        @NotEmpty(message = "events must not be empty")
        List<@Valid InternalPriceEventDto> events)
{
}
