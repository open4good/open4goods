package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

import tools.jackson.databind.ObjectMapper;

import com.sun.net.httpserver.HttpServer;

/**
 * Tests for {@link WebgainsFeedService} normalized affiliation mapping.
 */
class WebgainsFeedServiceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final FeedConfiguration feedConfig = new FeedConfiguration();

    @Test
    void parseWebgainsPromotionsShouldReturnMappedOffersAndVouchers() throws Exception
    {
        WebgainsFeedService service = buildService();

        Collection<AffiliationPromotion> mapped = service.parseWebgainsPromotions(objectMapper.readTree("""
                {"data":{"items":[
                  {
                    "program":{"id":12345,"name":"Webgains Merchant"},
                    "title":"Webgains offer",
                    "description":"20% off selected products",
                    "type":"voucher",
                    "code":"WEB20",
                    "discountValue":"20",
                    "startDate":"2026-01-01T00:00:00Z",
                    "endDate":"2026-12-31T23:59:59Z",
                    "landingUrl":"https://merchant.example/promo",
                    "trackingUrl":"https://track.webgains.com/click.html?wglinkid=123",
                    "terms":"Terms apply",
                    "countries":[{"code":"fr"}]
                  }
                ]}}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationPromotion promotion = mapped.iterator().next();
        assertThat(promotion.getProviderName()).isEqualTo("Webgains");
        assertThat(promotion.getProgramId()).isEqualTo("12345");
        assertThat(promotion.getAdvertiserName()).isEqualTo("Webgains Merchant");
        assertThat(promotion.getTitle()).isEqualTo("Webgains offer");
        assertThat(promotion.getDescription()).isEqualTo("20% off selected products");
        assertThat(promotion.getVoucherCode()).isEqualTo("WEB20");
        assertThat(promotion.getDiscountType()).isEqualTo("voucher");
        assertThat(promotion.getDiscountValue()).isEqualByComparingTo("20");
        assertThat(promotion.getStartDate().toString()).isEqualTo("2026-01-01");
        assertThat(promotion.getEndDate().toString()).isEqualTo("2026-12-31");
        assertThat(promotion.getLandingUrl()).isEqualTo("https://merchant.example/promo");
        assertThat(promotion.getTrackingUrl()).isEqualTo("https://track.webgains.com/click.html?wglinkid=123");
        assertThat(promotion.getConditions()).isEqualTo("Terms apply");
        assertThat(promotion.getCountryCodes()).containsExactly("FR");
    }

    /**
     * A failed API call raises an exception that does not quote the key carried by the endpoint.
     */
    @Test
    void failedCallNeverQuotesTheApiKey() throws Exception
    {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchange.sendResponseHeaders(500, -1);
            exchange.close();
        });
        server.start();
        try
        {
            WebgainsFeedService service = buildService();
            feedConfig.getWebgains().setOffersAndVouchersEndpoint(
                    "http://127.0.0.1:" + server.getAddress().getPort() + "/offers?key=secret-key&page=1");

            Throwable thrown = catchThrowable(service::retrieveWebgainsPromotions);

            assertThat(thrown).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("HTTP status 500")
                    .message().doesNotContain("secret-key");
        }
        finally
        {
            server.stop(0);
        }
    }

    private WebgainsFeedService buildService()
    {
        FeedConfiguration config = feedConfig;
        config.getWebgains().setEnabled(true);
        config.setDefaultCsvProperties(new CsvDataSourceProperties());
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return new WebgainsFeedService(
                config,
                mock(RemoteFileCachingService.class),
                dataSourceConfigService,
                new SerialisationService(),
                "token");
    }
}
