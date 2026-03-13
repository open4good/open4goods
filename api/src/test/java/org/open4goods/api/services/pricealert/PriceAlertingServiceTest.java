package org.open4goods.api.services.pricealert;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.PriceAlertingProperties;
import org.open4goods.api.dto.pricealert.InternalPriceEventDto;
import org.open4goods.model.product.ProductCondition;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * HTTP integration tests for {@link PriceAlertingService}.
 */
class PriceAlertingServiceTest
{
    private RestTemplate restTemplate;
    private MockRestServiceServer server;
    private PriceAlertingService service;

    @BeforeEach
    void setUp()
    {
        restTemplate = new RestTemplate();
        server = MockRestServiceServer.createServer(restTemplate);

        PriceAlertingProperties properties = new PriceAlertingProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://product-alert:8087");
        properties.setApiKey("secret");

        service = new PriceAlertingService(properties, restTemplate, new SimpleMeterRegistry());
    }

    @Test
    void publishPriceDropEventsPostsBatchToProductAlert()
    {
        server.expect(requestTo("http://product-alert:8087/internal/v1/price-events"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header(PriceAlertingService.API_KEY_HEADER, "secret"))
                .andRespond(withSuccess("{\"receivedEvents\":1,\"matchedSubscriptions\":2,\"createdCandidates\":1}", MediaType.APPLICATION_JSON));

        service.publishPriceDropEvents(List.of(new InternalPriceEventDto(
                1234567890128L,
                ProductCondition.NEW,
                100d,
                90d,
                Instant.parse("2026-03-13T10:15:30Z"))));

        server.verify();
    }
}
