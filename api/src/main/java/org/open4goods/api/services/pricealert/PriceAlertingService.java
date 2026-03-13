package org.open4goods.api.services.pricealert;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.api.config.yml.PriceAlertingProperties;
import org.open4goods.api.dto.pricealert.InternalPriceEventDto;
import org.open4goods.api.dto.pricealert.PriceEventsIngestionRequest;
import org.open4goods.api.dto.pricealert.PriceEventsIngestionResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Best-effort HTTP publisher for price-drop events sent to the product-alert
 * microservice.
 */
public class PriceAlertingService
{
    /**
     * Shared header name for internal API key propagation.
     */
    public static final String API_KEY_HEADER = "X-API-Key";

    private static final Logger LOGGER = LoggerFactory.getLogger(PriceAlertingService.class);

    private final PriceAlertingProperties properties;
    private final RestTemplate restTemplate;
    private final MeterRegistry meterRegistry;

    /**
     * Creates the publisher with a configured {@link RestTemplate}.
     *
     * @param properties integration properties
     * @param restTemplateBuilder rest template builder
     * @param meterRegistry metrics registry
     */
    public PriceAlertingService(PriceAlertingProperties properties,
            RestTemplateBuilder restTemplateBuilder,
            MeterRegistry meterRegistry)
    {
        this(
                properties,
                restTemplateBuilder
                        .connectTimeout(properties.getConnectTimeout())
                        .readTimeout(properties.getReadTimeout())
                        .build(),
                meterRegistry);
    }

    PriceAlertingService(PriceAlertingProperties properties, RestTemplate restTemplate, MeterRegistry meterRegistry)
    {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Publishes a batch of price-drop events without impacting product
     * aggregation on failure.
     *
     * @param events events to publish
     */
    public void publishPriceDropEvents(List<InternalPriceEventDto> events)
    {
        if (!properties.isEnabled() || events == null || events.isEmpty())
        {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (StringUtils.isNotBlank(properties.getApiKey()))
        {
            headers.set(API_KEY_HEADER, properties.getApiKey());
        }

        try
        {
            PriceEventsIngestionResponse response = restTemplate.postForObject(
                    baseUrl() + "/internal/v1/price-events",
                    new HttpEntity<>(new PriceEventsIngestionRequest(events), headers),
                    PriceEventsIngestionResponse.class);

            meterRegistry.counter("price.alerting.publish.success").increment(events.size());
            LOGGER.info("Published {} price-drop events to product-alert. Matched subscriptions: {}, candidates: {}",
                    events.size(),
                    response == null ? 0 : response.matchedSubscriptions(),
                    response == null ? 0 : response.createdCandidates());
        }
        catch (Exception exception)
        {
            meterRegistry.counter("price.alerting.publish.failure").increment(events.size());
            LOGGER.warn("Failed to publish {} price-drop events to product-alert: {}", events.size(), exception.getMessage());
            LOGGER.debug("Price alert publication failure", exception);
        }
    }

    private String baseUrl()
    {
        return StringUtils.removeEnd(properties.getBaseUrl(), "/");
    }
}
