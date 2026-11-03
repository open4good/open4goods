package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.affiliation.AffiliationProgram;
import org.open4goods.model.affiliation.AffiliationPromotion;
import org.open4goods.model.affiliation.AffiliationTransaction;
import org.open4goods.services.feedservice.config.FeedConfiguration;
import org.open4goods.services.remotefilecaching.config.CacheResourceConfig;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.databind.ObjectMapper;

/**
 * Tests for {@link AwinFeedService} normalized affiliation mapping.
 */
class AwinFeedServiceTest
{
    private static final String SECRET = "secret-key";

    private static final String CATALOG_URL = "https://productdata.awin.com/datafeed/list/apikey/" + SECRET;

    private static final String FEED_URL = "https://productdata.awin.com/datafeed/download/apikey/" + SECRET
            + "/fid/42/format/csv";

    private static final String CATALOG_HEADER = "Advertiser ID,Advertiser Name,Membership Status,URL\n";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    /**
     * Loading a catalog whose URL, feed URLs and API endpoint carry the Awin key never writes the key in logs, while
     * the datasource keeps its real feed URL.
     */
    @Test
    void loadingTheCatalogNeverLogsTheApiKey() throws Exception
    {
        RemoteFileCachingService cacheService = catalogService(CATALOG_HEADER
                + "42,Shop,Active," + FEED_URL + "\n"
                + "43,,Active," + FEED_URL + "\n");
        AwinFeedService service = buildOfflineService(cacheService);

        ListAppender<ILoggingEvent> appender = captureLogs();
        Set<DataSourceProperties> datasources;
        try
        {
            datasources = service.getDatasources();
        }
        finally
        {
            releaseLogs(appender);
        }

        assertThat(datasources).singleElement()
                .satisfies(ds -> assertThat(ds.getCsvDatasource().getDatasourceUrls()).containsExactly(FEED_URL));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("Loading datasource properties for provider Awin from catalog: https://productdata.awin.com/"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("Skipping Awin catalog line of advertiser '43'"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("Retrieving Awin merchant metadata"));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("Volatile datasource for key 'Shop'"));
        assertNoSecretLogged(appender);

        ArgumentCaptor<CacheResourceConfig> catalog = ArgumentCaptor.forClass(CacheResourceConfig.class);
        verify(cacheService).retrieve(catalog.capture());
        assertThat(catalog.getValue().getUrl()).isEqualTo(CATALOG_URL);
        assertThat(catalog.getValue().getLogLabel()).isEqualTo("Awin catalog");
    }

    /**
     * An empty catalog is reported without its URL.
     */
    @Test
    void emptyCatalogWarningNeverLogsTheApiKey() throws Exception
    {
        AwinFeedService service = buildOfflineService(catalogService(CATALOG_HEADER + "43,,Active," + FEED_URL + "\n"));

        ListAppender<ILoggingEvent> appender = captureLogs();
        try
        {
            assertThat(service.getDatasources()).isEmpty();
        }
        finally
        {
            releaseLogs(appender);
        }

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("Datasource cache is empty after loading from catalog for provider Awin"));
        assertNoSecretLogged(appender);
    }

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
        return new AwinFeedService(feedConfiguration(), mock(RemoteFileCachingService.class), dataSourceConfigService(),
                new SerialisationService(), "123456", SECRET);
    }

    /**
     * Returns a service whose program and promotion loads, which call the Awin API, are disabled.
     */
    private AwinFeedService buildOfflineService(RemoteFileCachingService cacheService)
    {
        return new AwinFeedService(feedConfiguration(), cacheService, dataSourceConfigService(),
                new SerialisationService(), "123456", SECRET)
        {
            @Override
            protected Collection<AffiliationProgram> loadProgramsInternal()
            {
                return List.of();
            }

            @Override
            protected Collection<AffiliationPromotion> loadPromotionsInternal()
            {
                return List.of();
            }
        };
    }

    private static FeedConfiguration feedConfiguration()
    {
        FeedConfiguration config = new FeedConfiguration();
        config.setDefaultCsvProperties(new CsvDataSourceProperties());
        config.setCatalogUrl(CATALOG_URL);
        config.setDatasourceKeyAttribute("Advertiser Name");
        config.setDatasourceUrlAttribute("URL");
        return config;
    }

    private static DataSourceConfigService dataSourceConfigService()
    {
        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());
        return dataSourceConfigService;
    }

    /**
     * Returns a cache service serving {@code catalog} as the Awin catalog and one joined merchant as the programmes.
     */
    private RemoteFileCachingService catalogService(String catalog) throws Exception
    {
        Path catalogFile = Files.writeString(tempDir.resolve("catalog.csv"), catalog, StandardCharsets.UTF_8);
        Path merchantsFile = Files.writeString(tempDir.resolve("merchants.json"), "[{\"id\":42,\"name\":\"Shop\",\"displayUrl\":\"https://www.shop.example\"}]",
                StandardCharsets.UTF_8);
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.retrieve(any(CacheResourceConfig.class))).thenReturn(catalogFile.toFile());
        when(cacheService.getResource(anyString(), anyInt())).thenReturn(merchantsFile.toFile());
        return cacheService;
    }

    private static ListAppender<ILoggingEvent> captureLogs()
    {
        // AbstractFeedService logs under the runtime class name, AwinFeedService under its own
        Logger logger = (Logger) LoggerFactory.getLogger(AwinFeedService.class.getPackageName());
        logger.setLevel(Level.DEBUG);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private static void releaseLogs(ListAppender<ILoggingEvent> appender)
    {
        Logger logger = (Logger) LoggerFactory.getLogger(AwinFeedService.class.getPackageName());
        logger.detachAppender(appender);
        logger.setLevel(null);
    }

    private static void assertNoSecretLogged(ListAppender<ILoggingEvent> appender)
    {
        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET);
            for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause())
            {
                assertThat(String.valueOf(proxy.getMessage())).doesNotContain(SECRET);
            }
        });
    }
}
