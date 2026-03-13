package org.open4goods.services.productalert.controller;

import jakarta.validation.Valid;
import java.util.Objects;
import org.open4goods.services.productalert.config.yml.ProductAlertProperties;
import org.open4goods.services.productalert.dto.PriceEventsIngestionRequest;
import org.open4goods.services.productalert.dto.PriceEventsIngestionResponse;
import org.open4goods.services.productalert.service.ProductAlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Internal REST endpoint receiving price-drop events from the API service.
 */
@RestController
@RequestMapping("/internal/v1")
public class InternalPriceEventController
{
    /**
     * Shared header name for internal API key checks.
     */
    public static final String API_KEY_HEADER = "X-API-Key";

    private final ProductAlertService productAlertService;
    private final ProductAlertProperties properties;

    /**
     * Creates the controller.
     *
     * @param productAlertService product alert service
     * @param properties service properties
     */
    public InternalPriceEventController(ProductAlertService productAlertService, ProductAlertProperties properties)
    {
        this.productAlertService = productAlertService;
        this.properties = properties;
    }

    /**
     * Ingests a batch of internal price-drop events.
     *
     * @param apiKey request API key
     * @param request ingestion request
     * @return ingestion counters
     */
    @PostMapping("/price-events")
    public ResponseEntity<PriceEventsIngestionResponse> ingestPriceEvents(
            @RequestHeader(name = API_KEY_HEADER, required = false) String apiKey,
            @Valid @RequestBody PriceEventsIngestionRequest request)
    {
        validateApiKey(apiKey);
        return ResponseEntity.ok(productAlertService.ingestPriceEvents(request));
    }

    private void validateApiKey(String apiKey)
    {
        if (!properties.getSecurity().isEnabled())
        {
            return;
        }

        if (!Objects.equals(properties.getInternal().getApiKey(), apiKey))
        {
            throw new ProductAlertUnauthorizedException("Invalid internal API key");
        }
    }
}
