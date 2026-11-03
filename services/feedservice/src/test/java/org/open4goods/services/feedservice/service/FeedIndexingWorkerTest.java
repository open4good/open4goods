package org.open4goods.services.feedservice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.open4goods.commons.config.yml.datasource.CsvDataSourceProperties;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.services.feedservice.model.FeedIndexingJobStat;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.read.ListAppender;
import tools.jackson.dataformat.csv.CsvSchema;

import com.sun.net.httpserver.HttpServer;

/**
 * Ensures {@link FeedIndexingWorker} never writes a feed URL, which can carry an API key as a path segment (Effinity
 * {@code apiv3/{key}/...}, Awin), in its logs, job stats or health details.
 */
class FeedIndexingWorkerTest {

    private static final String SECRET = "secret-key";

    private static final String FEED_PATH = "/apiv3/" + SECRET + "/feed.csv";

    @TempDir
    Path tempDir;

    private HttpServer server;

    private FeedIndexingService csvService;

    private ListAppender<ILoggingEvent> appender;

    private Level rootLevel;

    @BeforeEach
    void setUp() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            if (!FEED_PATH.equals(exchange.getRequestURI().getPath())) {
                exchange.sendResponseHeaders(404, -1);
                exchange.close();
                return;
            }
            byte[] body = "name;price\nproduct without url;12.5\n".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();

        csvService = mock(FeedIndexingService.class);
        Logger dedicatedLogger = (Logger) LoggerFactory.getLogger("feed-indexing-worker-test");
        when(csvService.createDatasourceLogger(anyString(), any(), anyString())).thenReturn(dedicatedLogger);

        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLevel = root.getLevel();
        root.setLevel(Level.ALL);
        appender = new ListAppender<>();
        appender.start();
        root.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        Logger root = (Logger) LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        root.detachAppender(appender);
        root.setLevel(rootLevel);
        server.stop(0);
    }

    /**
     * A feed whose download fails (HTTP 404, reported by the JDK with the URL as message) is logged by its label.
     */
    @Test
    void downloadFailureNeverLogsTheFeedUrl() {
        String url = baseUrl() + "/apiv3/" + SECRET + "/missing.csv";

        newWorker().fetch(datasource(url));

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("Exception while downloading feed")
                .contains(FeedIndexingWorker.loggableFeedUrl(url)));
        assertNoSecretLogged();
        assertNoSecretInStats();
    }

    /**
     * A feed that cannot be parsed is logged, and reported in the health details, by its label.
     */
    @Test
    void parsingFailureNeverLogsTheFeedUrl() throws IOException {
        String url = baseUrl() + FEED_PATH;
        when(csvService.detectSchema(any(File.class), any(Charset.class))).thenThrow(new IOException("unreadable"));

        newWorker().fetch(datasource(url));

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .contains("Critical exception while parsing CSV file"));
        ArgumentCaptor<String> broken = ArgumentCaptor.forClass(String.class);
        verify(csvService).brokenCsv(broken.capture());
        assertThat(broken.getValue()).isEqualTo(FeedIndexingWorker.loggableFeedUrl(url)).doesNotContain(SECRET);
        assertNoSecretLogged();
        assertNoSecretInStats();
    }

    /**
     * Rows of a downloaded feed that fail validation are logged with the feed label.
     */
    @Test
    void rowFailuresNeverLogTheFeedUrl() throws IOException {
        String url = baseUrl() + FEED_PATH;
        when(csvService.detectSchema(any(File.class), any(Charset.class)))
                .thenReturn(CsvSchema.emptySchema().withHeader().withColumnSeparator(';'));

        newWorker().fetch(datasource(url));

        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage())
                .startsWith("STARTING FEED URL")
                .contains(FeedIndexingWorker.loggableFeedUrl(url)));
        assertThat(appender.list).anySatisfy(event -> assertThat(event.getFormattedMessage()).startsWith("Done:"));
        assertNoSecretLogged();
        assertNoSecretInStats();
    }

    /**
     * The label keeps the scheme, host and port only, and stays distinct for two feeds of the same host.
     */
    @Test
    void loggableFeedUrlDropsPathAndQuery() {
        String feed = "https://api.effinity.fr/apiv3/" + SECRET + "/publisher/feed.csv?token=" + SECRET;
        String other = "https://api.effinity.fr/apiv3/" + SECRET + "/publisher/other.csv";

        assertThat(FeedIndexingWorker.loggableFeedUrl(feed))
                .startsWith("https://api.effinity.fr/<redacted> #")
                .doesNotContain(SECRET)
                .isNotEqualTo(FeedIndexingWorker.loggableFeedUrl(other));
        assertThat(FeedIndexingWorker.loggableFeedUrl(null)).isNull();
    }

    /**
     * No attribute of an indexed fragment carries the feed URL: {@code feed_url} holds the label.
     */
    @Test
    void indexedFragmentsNeverCarryTheFeedUrl() throws Exception {
        String url = baseUrl() + FEED_PATH;
        when(csvService.detectSchema(any(File.class), any(Charset.class)))
                .thenReturn(CsvSchema.emptySchema().withHeader().withColumnSeparator(';'));
        DataFragmentIndexer indexer = mock(DataFragmentIndexer.class);
        DataSourceProperties ds = datasource(url);

        newWorker(indexer).fetch(ds);

        ArgumentCaptor<DataFragment> fragments = ArgumentCaptor.forClass(DataFragment.class);
        verify(indexer).index(fragments.capture(), anyString());
        assertThat(fragments.getValue().getAttributes()).isNotEmpty().allSatisfy(attribute -> {
            assertThat(String.valueOf(attribute.getName())).doesNotContain(SECRET);
            assertThat(String.valueOf(attribute.getValue())).doesNotContain(SECRET).doesNotContain(url);
        });
        assertThat(fragments.getValue().getAttributes()).anySatisfy(attribute -> {
            assertThat(attribute.getName()).isEqualToIgnoringCase("feed_url");
            assertThat(attribute.getValue()).isEqualTo(FeedIndexingWorker.loggableFeedUrl(url));
        });
    }

    private FeedIndexingWorker newWorker() {
        return newWorker(mock(DataFragmentIndexer.class));
    }

    private FeedIndexingWorker newWorker(DataFragmentIndexer indexer) {
        return new FeedIndexingWorker(csvService, mock(DataFragmentCompletionService.class), indexer,
                10, tempDir.resolve("logs").toString(), new RemoteFileCachingService(tempDir.resolve("cache").toString()));
    }

    private DataSourceProperties datasource(String url) {
        DataSourceProperties ds = new DataSourceProperties();
        ds.setName("shop");
        ds.setDatasourceConfigName("shop.yml");
        CsvDataSourceProperties csv = new CsvDataSourceProperties();
        csv.getDatasourceUrls().add(url);
        ds.setCsvDatasource(csv);
        return ds;
    }

    private String baseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private void assertNoSecretLogged() {
        assertThat(appender.list).isNotEmpty();
        assertThat(appender.list).allSatisfy(event -> {
            assertThat(event.getFormattedMessage()).doesNotContain(SECRET);
            for (IThrowableProxy proxy = event.getThrowableProxy(); proxy != null; proxy = proxy.getCause()) {
                assertThat(String.valueOf(proxy.getMessage())).doesNotContain(SECRET);
            }
        });
    }

    private void assertNoSecretInStats() {
        ArgumentCaptor<FeedIndexingJobStat> stats = ArgumentCaptor.forClass(FeedIndexingJobStat.class);
        verify(csvService).finished(stats.capture(), any());
        assertThat(stats.getValue().getUrl()).doesNotContain(SECRET);
    }
}
