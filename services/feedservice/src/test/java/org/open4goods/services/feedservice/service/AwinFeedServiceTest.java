package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

import tools.jackson.databind.ObjectMapper;

/**
 * Tests for {@link AwinFeedService} normalized affiliation mapping.
 */
class AwinFeedServiceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseAwinPromotionsShouldReturnMappedPromotions() throws Exception
    {
        AwinFeedService service = buildService();

        Collection<AffiliationPromotion> mapped = service.parseAwinPromotions(objectMapper.readTree("""
                {"promotions":[
                  {
                    "promotionId":123,
                    "type":"voucher",
                    "advertiser":{"id":456,"name":"Awin Merchant","joined":true},
                    "title":"Promo Title",
                    "description":"20% off",
                    "terms":"Terms apply",
                    "startDate":"2026-01-01T00:00:00.000",
                    "endDate":"2026-12-31T00:00:00.000",
                    "url":"https://merchant.example/promo",
                    "urlTracking":"https://www.awin1.com/cread.php?awinmid=456",
                    "regions":{"list":[{"name":"France","countryCode":"FR"}]},
                    "voucher":{"code":"PROMO20"}
                  }
                ]}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationPromotion promotion = mapped.iterator().next();
        assertThat(promotion.getProviderName()).isEqualTo("Awin");
        assertThat(promotion.getProgramId()).isEqualTo("456");
        assertThat(promotion.getAdvertiserName()).isEqualTo("Awin Merchant");
        assertThat(promotion.getTitle()).isEqualTo("Promo Title");
        assertThat(promotion.getVoucherCode()).isEqualTo("PROMO20");
        assertThat(promotion.getDiscountType()).isEqualTo("voucher");
        assertThat(promotion.getStartDate().toString()).isEqualTo("2026-01-01");
        assertThat(promotion.getEndDate().toString()).isEqualTo("2026-12-31");
        assertThat(promotion.getLandingUrl()).isEqualTo("https://merchant.example/promo");
        assertThat(promotion.getTrackingUrl()).isEqualTo("https://www.awin1.com/cread.php?awinmid=456");
        assertThat(promotion.getConditions()).isEqualTo("Terms apply");
        assertThat(promotion.getCountryCodes()).containsExactly("FR");
    }

    @Test
    void parseAwinTransactionsShouldReturnMappedTransactions() throws Exception
    {
        AwinFeedService service = buildService();

        Collection<AffiliationTransaction> mapped = service.parseAwinTransactions(objectMapper.readTree("""
                {"transactions":[
                  {
                    "id":"tx-1",
                    "advertiserId":456,
                    "clickDate":"2026-05-29T11:00:00Z",
                    "transactionDate":"2026-05-29T12:00:00Z",
                    "status":"approved",
                    "saleAmount":{"amount":"120.50","currency":"EUR"},
                    "commissionAmount":{"amount":"6.03","currency":"EUR"},
                    "clickRef":"sub-1",
                    "basketProducts":[{"productId":"sku-1"}]
                  }
                ]}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationTransaction transaction = mapped.iterator().next();
        assertThat(transaction.getProviderName()).isEqualTo("Awin");
        assertThat(transaction.getTransactionId()).isEqualTo("tx-1");
        assertThat(transaction.getProgramId()).isEqualTo("456");
        assertThat(transaction.getClickDate()).isEqualTo(Instant.parse("2026-05-29T11:00:00Z"));
        assertThat(transaction.getTransactionDate()).isEqualTo(Instant.parse("2026-05-29T12:00:00Z"));
        assertThat(transaction.getStatus()).isEqualTo("approved");
        assertThat(transaction.getSaleAmount()).isEqualByComparingTo("120.50");
        assertThat(transaction.getCommissionAmount()).isEqualByComparingTo("6.03");
        assertThat(transaction.getCurrency()).isEqualTo("EUR");
        assertThat(transaction.getSubId()).isEqualTo("sub-1");
        assertThat(transaction.getProductId()).isEqualTo("sku-1");
    }

    private AwinFeedService buildService()
    {
        FeedConfiguration config = new FeedConfiguration();
        config.setDefaultCsvProperties(new CsvDataSourceProperties());
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return new AwinFeedService(
                config,
                mock(RemoteFileCachingService.class),
                dataSourceConfigService,
                new SerialisationService(),
                "123456",
                "token");
    }
}
