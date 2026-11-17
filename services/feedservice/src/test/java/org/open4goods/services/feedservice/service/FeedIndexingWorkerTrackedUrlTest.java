package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.services.remotefilecaching.config.RemoteFileCachingProperties;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Ensures an offer of a Kwanko product feed is never indexed with an untracked direct link (GOU-2320): the sample
 * reproduces the "Carrefour - marketplace" feed, whose only url column ({@code product page URL}) carries a Kwanko
 * tracked link on most rows and a direct {@code www.carrefour.fr} link on some.
 */
class FeedIndexingWorkerTrackedUrlTest {

    private static final String TRACKED_URL = "https://action.metaffiliation.com/trk.php?mclic=P0000000B00F00DS1UF0000a0b000000001V4";

    private static final String DIRECT_URL = "https://www.carrefour.fr/p/tcl-c80-50c805-tv-127-cm-(50\")-4k-ultra-hd-smart-tv-5901292521486?s=3093";

    private static final String FIRST_PARTY_TRACKED_URL = "https://qby.merchant.example/?P0000F000B00F00S1UC00000f000001V4";

    private static final String CLICK_TEMPLATE = "https://action.metaffiliation.com/trk.php?mclic=P0000000B00F00DS1U&redir={XXX}";

    @TempDir
    Path tempDir;

    private FeedIndexingService csvService;

    private DataFragmentIndexer indexer;

    private Logger dedicatedLogger;

    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() throws Exception {
        csvService = mock(FeedIndexingService.class);
        indexer = mock(DataFragmentIndexer.class);
        dedicatedLogger = (Logger) LoggerFactory.getLogger("feed-indexing-worker-tracked-url-test");
        dedicatedLogger.setLevel(Level.ALL);
        appender = new ListAppender<>();
        appender.start();
        dedicatedLogger.addAppender(appender);
        when(csvService.createDatasourceLogger(anyString(), any(), anyString())).thenReturn(dedicatedLogger);
        when(csvService.detectSchema(any(File.class), any(Charset.class)))
                .thenReturn(CsvSchema.emptySchema().withHeader().withColumnSeparator('|'));
    }

    @AfterEach
    void tearDown() {
        dedicatedLogger.detachAppender(appender);
    }

    /**
     * With the campaign click template, the direct link is tracked and tracked links are left unchanged.
     */
    @Test
    void untrackedUrlIsRewrittenWithTheCampaignClickTemplate() throws Exception {
        newWorker().fetch(kwankoDatasource(CLICK_TEMPLATE));

        List<DataFragment> fragments = indexedFragments();
        assertThat(fragments).extracting(DataFragment::getUrl).containsExactly(
                TRACKED_URL,
                CLICK_TEMPLATE.replace("{XXX}", URLEncoder.encode(DIRECT_URL, StandardCharsets.UTF_8)),
                FIRST_PARTY_TRACKED_URL);
        assertThat(fragments).allSatisfy(fragment -> {
            assertThat(fragment.getAffiliatedUrl()).isEqualTo(fragment.getUrl());
            assertThat(fragment.getUrl()).doesNotStartWith("https://www.carrefour.fr");
        });
        assertThat(doneMessage()).contains("imported: 3", "not_validable: 0", "tracking_rewritten: 1", "untracked_rejected: 0");
    }

    /**
     * Without click template, the direct link is rejected and counted, never indexed.
     */
    @Test
    void untrackedUrlIsRejectedWithoutClickTemplate() throws Exception {
        newWorker().fetch(kwankoDatasource(null));

        assertThat(indexedFragments()).extracting(DataFragment::getUrl).containsExactly(TRACKED_URL, FIRST_PARTY_TRACKED_URL);
        assertThat(doneMessage()).contains("imported: 2", "not_validable: 1", "tracking_rewritten: 0", "untracked_rejected: 1");
    }

    /**
     * A datasource without tracked url pattern keeps its url as is (other networks, YAML datasources).
     */
    @Test
    void datasourceWithoutTrackedUrlPatternKeepsDirectUrls() throws Exception {
        DataSourceProperties ds = kwankoDatasource(null);
        ds.getCsvDatasource().setTrackedUrlPattern(null);

        newWorker().fetch(ds);

        assertThat(indexedFragments()).extracting(DataFragment::getUrl).contains(DIRECT_URL);
        assertThat(doneMessage()).contains("imported: 3", "tracking_rewritten: 0", "untracked_rejected: 0");
    }

    /**
     * The Kwanko "Vevor FR" feed names its url column {@code page url} (GOU-2391): its rows were all rejected as not_validable.
     */
    @Test
    void pageUrlColumnIsRecognisedAsProductUrl() throws Exception {
        csvService = mock(FeedIndexingService.class);
        when(csvService.createDatasourceLogger(anyString(), any(), anyString())).thenReturn(dedicatedLogger);
        when(csvService.detectSchema(any(File.class), any(Charset.class)))
                .thenReturn(CsvSchema.emptySchema().withHeader().withColumnSeparator(','));
        DataSourceProperties ds = kwankoDatasource(CLICK_TEMPLATE);
        ds.setName("vevorfr");
        ds.setDatasourceConfigName("Vevor FR");
        ds.getCsvDatasource().getDatasourceUrls().clear();
        ds.getCsvDatasource().getDatasourceUrls().add(Path.of(getClass().getResource("/kwanko/vevorfr-sample.csv").toURI()).toString());

        newWorker().fetch(ds);

        assertThat(indexedFragments()).hasSize(2).allSatisfy(fragment -> assertThat(fragment.getUrl()).startsWith(TRACKED_URL.substring(0, 40)));
        assertThat(doneMessage()).contains("imported: 2", "not_validable: 0");
    }

    private List<DataFragment> indexedFragments() throws Exception {
        ArgumentCaptor<DataFragment> fragments = ArgumentCaptor.forClass(DataFragment.class);
        verify(indexer, atLeastOnce()).index(fragments.capture(), anyString());
        return fragments.getAllValues();
    }

    private String doneMessage() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(message -> message.startsWith("Done:"))
                .findFirst()
                .orElseThrow();
    }

    private FeedIndexingWorker newWorker() {
        return new FeedIndexingWorker(csvService, mock(DataFragmentCompletionService.class), indexer,
                10, tempDir.resolve("logs").toString(), new RemoteFileCachingService(tempDir.resolve("cache").toString(),
                        new RemoteFileCachingProperties(), tempDir.resolve("feeds").toFile()));
    }

    /**
     * Builds a datasource with the url columns and tracked url pattern {@link KwankoFeedService} gives its product feeds.
     */
    private DataSourceProperties kwankoDatasource(String clickTemplate) throws Exception {
        DataSourceProperties ds = new DataSourceProperties();
        ds.setName("carrefourmarketplace");
        ds.setDatasourceConfigName("Carrefour - marketplace");
        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.setUrl("product_url");
        csv.setAffiliatedUrl("tracking_url");
        csv.setTrackedUrlPattern(KwankoFeedService.KWANKO_TRACKED_URL_PATTERN);
        csv.setTrackingLinkTemplate(clickTemplate);
        csv.getDatasourceUrls().add(Path.of(getClass().getResource("/kwanko/carrefour-marketplace-sample.csv").toURI()).toString());
        ds.setCsvDatasource(csv);
        return ds;
    }
}
