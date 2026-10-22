package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

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
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;

/**
 * Tests for {@link EffiliationFeedService} on anonymised Effinity API v3 responses: requests, pagination,
 * mapping, enablement, cache fallback and secret handling.
 */
class EffiliationFeedServiceTest {

    private static final String API_KEY = "effi-secret-key";

    @TempDir
    Path tempDir;

    @Test
    void retrieveEffiliationFeedsShouldThrowWhenDisabled() {
        EffiliationFeedService service = buildService(false, mock(RemoteFileCachingService.class));

        assertThatThrownBy(() -> service.retrieveEffiliationFeeds(API_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("disabled");
    }

    @Test
    void retrieveEffiliationFeedsShouldMergeAllPages() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        stubPage(cacheService, "publisher/links/productfeeds", 1, "productfeeds-page-1.json");
        stubPage(cacheService, "publisher/links/productfeeds", 2, "productfeeds-page-2.json");

        var feeds = buildService(true, cacheService).retrieveEffiliationFeeds(API_KEY);

        assertThat(feeds).extracting(feed -> feed.path("linkId").asInt())
                .containsExactly(1024, 1025, 1026, 1027, 1028);
    }

    @Test
    void getDatasourcesShouldBuildOneDatasourcePerActiveFeed() throws Exception {
        RemoteFileCachingService cacheService = programsAndFeeds();

        Set<DataSourceProperties> datasources = buildService(true, cacheService).getDatasources();

        // Skipped: frozen program, feed without merchant, feed without URL.
        Map<String, DataSourceProperties> byFeedKey = datasources.stream()
                .collect(Collectors.toMap(DataSourceProperties::getFeedKey, ds -> ds));
        assertThat(byFeedKey).containsOnlyKeys("Active Merchant - Flux produits", "Closing Merchant - Flux produits");

        DataSourceProperties active = byFeedKey.get("Active Merchant - Flux produits");
        assertThat(active.getCsvDatasource().getDatasourceUrls()).containsExactly("https://feeds.example/active-feed.csv");
        assertThat(active.getPortalUrl()).isEqualTo("https://active.example");
        assertThat(active.getAffiliatedPortalUrl()).isEqualTo("https://track.example/active");
        assertThat(active.getLogo()).isEqualTo("https://cdn.example/active-logo.png");
        assertThat(active.getName()).isEqualTo("active.example");

        DataSourceProperties closing = byFeedKey.get("Closing Merchant - Flux produits");
        assertThat(closing.getCsvDatasource().getDatasourceUrls()).containsExactly("https://feeds.example/closing-feed.csv");
    }

    @Test
    void requestsShouldTargetApiV3WithTheKeyAsPathSegmentAndPaginate() throws Exception {
        RemoteFileCachingService cacheService = programsAndFeeds();

        buildService(true, cacheService).getDatasources();

        ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> labels = ArgumentCaptor.forClass(String.class);
        verify(cacheService, atLeastOnce()).getResource(urls.capture(), eq(1), labels.capture());

        // load() also refreshes programs and promotions.
        assertThat(urls.getAllValues()).contains(
                "https://api.effinity.fr/apiv3/effi-secret-key/publisher/programs?status=active&status=inactive&page=1&perPage=1000",
                "https://api.effinity.fr/apiv3/effi-secret-key/publisher/programs?status=active&status=inactive&page=2&perPage=1000",
                "https://api.effinity.fr/apiv3/effi-secret-key/publisher/links/productfeeds?page=1&perPage=1000",
                "https://api.effinity.fr/apiv3/effi-secret-key/publisher/links/productfeeds?page=2&perPage=1000");
        assertThat(urls.getAllValues()).allSatisfy(url -> {
            URI uri = URI.create(url);
            assertThat(uri.getHost()).isEqualTo("api.effinity.fr");
            assertThat(uri.getPath()).startsWith("/apiv3/" + API_KEY + "/publisher/");
        });
        assertThat(labels.getAllValues()).allSatisfy(label -> assertThat(label)
                .startsWith("https://api.effinity.fr/apiv3/{apikey}/publisher/")
                .doesNotContain(API_KEY));
    }

    @Test
    void getDatasourcesShouldUseStaleCacheWhenRefreshFails() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        stubPage(cacheService, "publisher/programs", 1, "programs-page-1.json");
        stubPage(cacheService, "publisher/programs", 2, "programs-page-2.json");
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/links/productfeeds")), eq(1), anyString()))
                .thenThrow(new IOException("network down for " + API_KEY));
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/links/productfeeds") && url.contains("page=1&")),
                eq(Integer.MAX_VALUE), anyString())).thenReturn(fixture("productfeeds-page-1.json"));
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/links/productfeeds") && url.contains("page=2&")),
                eq(Integer.MAX_VALUE), anyString())).thenReturn(fixture("productfeeds-page-2.json"));

        Set<DataSourceProperties> datasources = buildService(true, cacheService).getDatasources();

        assertThat(datasources).extracting(DataSourceProperties::getFeedKey)
                .containsExactlyInAnyOrder("Active Merchant - Flux produits", "Closing Merchant - Flux produits");
    }

    @Test
    void getDatasourcesShouldReturnNoneWhenProgramsPayloadIsInvalid() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/programs")), anyInt(), anyString()))
                .thenReturn(jsonFile("{\"pagination\":{\"lastPage\":1},\"message\":\"unexpected\"}"));
        stubPage(cacheService, "publisher/links/productfeeds", 1, "productfeeds-page-1.json");
        stubPage(cacheService, "publisher/links/productfeeds", 2, "productfeeds-page-2.json");

        assertThat(buildService(true, cacheService).getDatasources()).isEmpty();
    }

    @Test
    void paginationShouldStopOnRunawayLastPage() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/links/productfeeds")), anyInt(), anyString()))
                .thenReturn(jsonFile("{\"pagination\":{\"lastPage\":100000},\"links\":[]}"));

        assertThatThrownBy(() -> buildService(true, cacheService).retrieveEffiliationFeeds(API_KEY))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("more than " + EffiliationFeedService.MAX_PAGES + " pages");
    }

    @Test
    void getProgramsShouldReturnMappedPrograms() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        stubPage(cacheService, "publisher/programs", 1, "programs-page-1.json");
        stubPage(cacheService, "publisher/programs", 2, "programs-page-2.json");

        Collection<AffiliationProgram> mapped = buildService(true, cacheService).getPrograms();

        assertThat(mapped).extracting(AffiliationProgram::getProgramId)
                .containsExactly("6600000101", "6600000303", "6600000202");
        AffiliationProgram ap = mapped.iterator().next();
        assertThat(ap.getProviderName()).isEqualTo("Effiliation");
        assertThat(ap.getAdvertiserName()).isEqualTo("Example Active Merchant");
        assertThat(ap.getStatus()).isEqualTo("active");
        assertThat(ap.getCountryCodes()).containsExactlyInAnyOrder("fr", "be");
        assertThat(ap.getCategories()).containsExactlyInAnyOrder("high-tech", "home appliances");
        assertThat(ap.getCurrency()).isEqualTo("EUR");
        assertThat(ap.getCookieDurationDays()).isEqualTo(30);
        assertThat(ap.getLogoUrl()).isEqualTo("https://cdn.example/active-logo.png");
        assertThat(ap.getPortalUrl()).isEqualTo("https://active.example");
        assertThat(ap.getTrackingUrl()).isEqualTo("https://track.example/active");
        assertThat(ap.getDescription()).isEqualTo("Example merchant selling appliances.");
        // Not provided by the API v3: absent, never zero, and no hypermedia link (it carries the key).
        assertThat(ap.getClickCommission()).isNull();
        assertThat(ap.getSaleCommissionPercent()).isNull();
        assertThat(ap.getLeadCommission()).isNull();
        assertThat(ap.getDisplayCpm()).isNull();
        assertThat(ap.getEpc()).isNull();
        assertThat(ap.getConversionRate()).isNull();
        assertThat(ap.getSignupUrl()).isNull();
    }

    @Test
    void getPromotionsShouldReturnMappedPromotions() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(argThat(url -> url != null
                && url.contains("/publisher/links?linkType=voucherOrDeal&page=1&perPage=1000")), anyInt(), anyString()))
                .thenReturn(fixture("promotions.json"));

        Collection<AffiliationPromotion> mapped = buildService(true, cacheService).getPromotions();

        assertThat(mapped).hasSize(1);
        AffiliationPromotion ap = mapped.iterator().next();
        assertThat(ap.getProviderName()).isEqualTo("Effiliation");
        assertThat(ap.getProgramId()).isEqualTo("6600000101");
        assertThat(ap.getAdvertiserName()).isEqualTo("Example Active Merchant");
        assertThat(ap.getTitle()).isEqualTo("Promo Title");
        assertThat(ap.getDescription()).isEqualTo("20% off");
        assertThat(ap.getVoucherCode()).isEqualTo("PROMO20");
        assertThat(ap.getDiscountType()).isEqualTo("voucher");
        assertThat(ap.getDiscountValue()).isEqualByComparingTo("20");
        assertThat(ap.getStartDate()).isEqualTo(LocalDate.of(2026, 1, 1));
        assertThat(ap.getEndDate()).isEqualTo(LocalDate.of(2026, 12, 31));
        assertThat(ap.getLandingUrl()).isEqualTo("https://active.example/promo");
        assertThat(ap.getTrackingUrl()).isEqualTo("https://track.example/promo");
    }

    @Test
    void getTransactionsShouldReturnMappedConversions() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/conversions")), anyInt(), anyString()))
                .thenReturn(fixture("conversions.json"));

        Collection<AffiliationTransaction> mapped = buildService(true, cacheService)
                .getTransactions(Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-31T21:59:59Z"));

        ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
        verify(cacheService).getResource(urls.capture(), eq(1), anyString());
        assertThat(urls.getValue()).isEqualTo("https://api.effinity.fr/apiv3/effi-secret-key/publisher/conversions"
                + "?start=2026-05-01&end=2026-05-31&dateType=conversionDate&conversionType=all&status=all&page=1&perPage=1000");

        assertThat(mapped).hasSize(2);
        AffiliationTransaction at = mapped.iterator().next();
        assertThat(at.getProviderName()).isEqualTo("Effiliation");
        assertThat(at.getTransactionId()).isEqualTo("anon-conv-0001");
        assertThat(at.getProgramId()).isEqualTo("6600000101");
        // Local Europe/Paris date-times (UTC+2 in May).
        assertThat(at.getTransactionDate()).isEqualTo(Instant.parse("2026-05-29T12:00:00Z"));
        assertThat(at.getClickDate()).isEqualTo(Instant.parse("2026-05-28T07:30:00Z"));
        assertThat(at.getStatus()).isEqualTo("validated");
        assertThat(at.getSaleAmount()).isEqualByComparingTo("120.50");
        assertThat(at.getCommissionAmount()).isEqualByComparingTo("6.03");
        assertThat(at.getCurrency()).isEqualTo("EUR");
        assertThat(at.getSubId()).isEqualTo("sub-0001");

        AffiliationTransaction lead = mapped.stream().skip(1).findFirst().orElseThrow();
        assertThat(lead.getTransactionDate()).isEqualTo(Instant.parse("2026-05-30T06:00:00Z"));
        assertThat(lead.getStatus()).isEqualTo("pending");
        assertThat(lead.getSaleAmount()).isNull();
        assertThat(lead.getCommissionAmount()).isEqualByComparingTo("1.4");
        assertThat(lead.getCurrency()).isEqualTo("EUR");
    }

    @Test
    void conversionsShouldBeRequestedByWindowsOfAtMost90Days() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(argThat(url -> url != null && url.contains("publisher/conversions")), anyInt(), anyString()))
                .thenReturn(jsonFile("{\"pagination\":{\"lastPage\":1},\"conversions\":[]}"));

        List<?> conversions = buildService(true, cacheService)
                .retrieveEffiliationConversions(API_KEY, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 5, 31));

        ArgumentCaptor<String> urls = ArgumentCaptor.forClass(String.class);
        verify(cacheService, atLeastOnce()).getResource(urls.capture(), eq(1), anyString());
        assertThat(conversions).isEmpty();
        assertThat(urls.getAllValues()).extracting(url -> url.substring(url.indexOf('?'), url.indexOf("&dateType")))
                .containsExactly("?start=2026-01-01&end=2026-03-31", "?start=2026-04-01&end=2026-05-31");
    }

    @Test
    void apiKeyShouldNeverReachTheLogs() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        when(cacheService.getResource(anyString(), anyInt(), anyString()))
                .thenThrow(new IOException("Server returned HTTP response code: 403 for URL: https://api.effinity.fr/apiv3/"
                        + API_KEY + "/publisher/programs"));

        Logger logger = (Logger) LoggerFactory.getLogger(EffiliationFeedService.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            EffiliationFeedService service = buildService(true, cacheService);
            service.getDatasources();
            service.getTransactions(Instant.parse("2026-05-01T00:00:00Z"), Instant.parse("2026-05-31T00:00:00Z"));
        } finally {
            logger.detachAppender(appender);
        }

        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(API_KEY);
            for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {
                assertThat(String.valueOf(proxy.getMessage())).doesNotContain(API_KEY);
            }
        });
    }

    private RemoteFileCachingService programsAndFeeds() throws Exception {
        RemoteFileCachingService cacheService = mock(RemoteFileCachingService.class);
        stubPage(cacheService, "publisher/programs", 1, "programs-page-1.json");
        stubPage(cacheService, "publisher/programs", 2, "programs-page-2.json");
        stubPage(cacheService, "publisher/links/productfeeds", 1, "productfeeds-page-1.json");
        stubPage(cacheService, "publisher/links/productfeeds", 2, "productfeeds-page-2.json");
        return cacheService;
    }

    private void stubPage(RemoteFileCachingService cacheService, String resource, int page, String fixture) throws Exception {
        File file = fixture(fixture);
        when(cacheService.getResource(argThat(url -> url != null && url.contains(resource) && url.contains("page=" + page + "&")),
                anyInt(), anyString())).thenReturn(file);
    }

    private EffiliationFeedService buildService(boolean enabled, RemoteFileCachingService cacheService) {
        FeedConfiguration config = new FeedConfiguration();
        FeedConfiguration.EffiliationConfig effiliation = new FeedConfiguration.EffiliationConfig();
        effiliation.setEnabled(enabled);
        effiliation.setCacheTtlDays(1);
        effiliation.setMaxJitterSeconds(0);
        config.setEffiliation(effiliation);

        CsvDataSourceProperties csvDataSourceProperties = new CsvDataSourceProperties();
        config.setDefaultCsvProperties(csvDataSourceProperties);

        DataSourceConfigService dataSourceConfigService = mock(DataSourceConfigService.class);
        when(dataSourceConfigService.getDatasourcePropertiesForFeed(anyString())).thenReturn(null);
        when(dataSourceConfigService.datasourceConfigs()).thenReturn(Map.of());

        return new EffiliationFeedService(
                config,
                cacheService,
                dataSourceConfigService,
                new SerialisationService(),
                API_KEY);
    }

    private File fixture(String name) throws IOException {
        try (InputStream in = getClass().getResourceAsStream("/effiliation/v3/" + name)) {
            assertThat(in).as("fixture %s", name).isNotNull();
            return jsonFile(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    private File jsonFile(String content) throws IOException {
        Path file = Files.createTempFile(tempDir, "effiliation", ".json");
        Files.writeString(file, content, StandardCharsets.UTF_8);
        return file.toFile();
    }
}
