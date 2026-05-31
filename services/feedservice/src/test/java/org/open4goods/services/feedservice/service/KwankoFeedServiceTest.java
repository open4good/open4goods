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
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;

import tools.jackson.databind.ObjectMapper;

/**
 * Tests for {@link KwankoFeedService} normalized affiliation mapping.
 */
class KwankoFeedServiceTest
{
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void parseKwankoProgramsShouldReturnMappedPrograms() throws Exception
    {
        KwankoFeedService service = buildService();

        Collection<AffiliationProgram> mapped = service.parseKwankoPrograms(objectMapper.readTree("""
                {"campaigns":[
                  {
                    "id":71827,
                    "name":"Kwanko Merchant",
                    "state":"active",
                    "url":"https://merchant.example/",
                    "logo":"https://merchant.example/logo.png",
                    "description":"Merchant description",
                    "currency":"EUR",
                    "languages":["fr_FR"],
                    "postclick_default":30,
                    "iab_categories":["shopping"]
                  }
                ]}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationProgram program = mapped.iterator().next();
        assertThat(program.getProviderName()).isEqualTo("Kwanko");
        assertThat(program.getProgramId()).isEqualTo("71827");
        assertThat(program.getAdvertiserName()).isEqualTo("Kwanko Merchant");
        assertThat(program.getStatus()).isEqualTo("active");
        assertThat(program.getPortalUrl()).isEqualTo("https://merchant.example/");
        assertThat(program.getLogoUrl()).isEqualTo("https://merchant.example/logo.png");
        assertThat(program.getCurrency()).isEqualTo("EUR");
        assertThat(program.getCookieDurationDays()).isEqualTo(30);
        assertThat(program.getCountryCodes()).containsExactly("FR");
        assertThat(program.getCategories()).containsExactly("shopping");
    }

    @Test
    void parseKwankoPromotionsShouldReturnMappedVoucherAds() throws Exception
    {
        KwankoFeedService service = buildService();

        Collection<AffiliationPromotion> mapped = service.parseKwankoPromotions(objectMapper.readTree("""
                {"ads":[
                  {
                    "campaign":{"id":1111,"name":"Campaign name","url":"https://merchant.example","languages":["fr_FR"]},
                    "type":"voucher_code",
                    "name":"Voucher code name",
                    "description":"Voucher code description",
                    "code":"PROMO20",
                    "validity_date":{"start":"2026-01-01T00:00:00+00:00","end":"2026-12-31T23:59:59+00:00"},
                    "tracked_material_per_websites":[{"urls":{"click":"https://action.metaffiliation.com/trk.php?mclic=XXXXX"}}]
                  }
                ]}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationPromotion promotion = mapped.iterator().next();
        assertThat(promotion.getProviderName()).isEqualTo("Kwanko");
        assertThat(promotion.getProgramId()).isEqualTo("1111");
        assertThat(promotion.getAdvertiserName()).isEqualTo("Campaign name");
        assertThat(promotion.getTitle()).isEqualTo("Voucher code name");
        assertThat(promotion.getVoucherCode()).isEqualTo("PROMO20");
        assertThat(promotion.getStartDate().toString()).isEqualTo("2026-01-01");
        assertThat(promotion.getEndDate().toString()).isEqualTo("2026-12-31");
        assertThat(promotion.getTrackingUrl()).isEqualTo("https://action.metaffiliation.com/trk.php?mclic=XXXXX");
        assertThat(promotion.getCountryCodes()).containsExactly("FR");
    }

    @Test
    void parseKwankoTransactionsShouldReturnMappedConversions() throws Exception
    {
        KwankoFeedService service = buildService();

        Collection<AffiliationTransaction> mapped = service.parseKwankoTransactions(objectMapper.readTree("""
                {"conversions":[
                  {
                    "kwanko_id":123456,
                    "unique_conversion_id":"conversion-1",
                    "state":"approved",
                    "completed_at":"2026-05-29T12:00:00Z",
                    "campaign":{"id":12345,"name":"Merchant","currency":"EUR"},
                    "earnings":{"value":"1.50"},
                    "websites_per_language":[{"argsites":{"argsite":"sub-1"}}]
                  }
                ]}
                """));

        assertThat(mapped).hasSize(1);
        AffiliationTransaction transaction = mapped.iterator().next();
        assertThat(transaction.getProviderName()).isEqualTo("Kwanko");
        assertThat(transaction.getTransactionId()).isEqualTo("conversion-1");
        assertThat(transaction.getProgramId()).isEqualTo("12345");
        assertThat(transaction.getTransactionDate()).isEqualTo(Instant.parse("2026-05-29T12:00:00Z"));
        assertThat(transaction.getStatus()).isEqualTo("approved");
        assertThat(transaction.getCommissionAmount()).isEqualByComparingTo("1.50");
        assertThat(transaction.getCurrency()).isEqualTo("EUR");
        assertThat(transaction.getSubId()).isEqualTo("sub-1");
    }

    private KwankoFeedService buildService()
    {
        FeedConfiguration config = new FeedConfiguration();
        config.getKwanko().setEnabled(true);
        config.setDefaultCsvProperties(new CsvDataSourceProperties());
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return new KwankoFeedService(
                config,
                mock(RemoteFileCachingService.class),
                dataSourceConfigService,
                new SerialisationService(),
                "token");
    }
}
