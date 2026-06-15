package org.open4goods.services.feedservice.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.open4goods.commons.config.yml.datasource.DataSourceProperties;
import org.open4goods.commons.helper.GenericFileLogger;
import org.open4goods.services.feedservice.config.FeedIndexingProperties;
import org.open4goods.services.feedservice.model.FeedIndexingJobStat;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;

import ch.qos.logback.classic.Level;
import jakarta.annotation.PreDestroy;
import tools.jackson.dataformat.csv.CsvSchema;

/**
 * Owns CSV feed indexing queues, dialect detection, worker health, and feed run stats.
 */
public class FeedIndexingService implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(FeedIndexingService.class);
    private static final int JOBS_QUEUE_CAPACITY = 1000;

    private final CsvDialectDetector csvDialectDetector = new CsvDialectDetector();
    private final BlockingQueue<DataSourceProperties> queue = new LinkedBlockingQueue<>(JOBS_QUEUE_CAPACITY);
    private final Map<String, FeedIndexingJobStat> runningJobs = new ConcurrentHashMap<>();
    private final AtomicLong feedNoUrls = new AtomicLong(0L);
    private final Set<String> brokenCsvFiles = Collections.synchronizedSet(new HashSet<>());

    public FeedIndexingService(FeedIndexingProperties properties, DataFragmentCompletionService completionService,
            DataFragmentIndexer dataFragmentIndexer, RemoteFileCachingService remoteFileCachingService,
            String logsFolder) {
        int workers = Math.max(1, properties.getConcurrentFetcherTask());
        for (int i = 0; i < workers; i++) {
            Thread worker = new Thread(new FeedIndexingWorker(this, completionService, dataFragmentIndexer,
                    properties.getWorkerPauseDurationMs(), logsFolder, remoteFileCachingService));
            worker.setName("feed-indexing-worker-" + i);
            worker.start();
        }
    }

    public void start(final DataSourceProperties config, final String datasourceConfName) {
        try {
            queue.put(config);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("Error while putting CSV feed indexing job in queue", e);
        }
    }

    public void stop(final String providerName) {
        runningJobs.remove(providerName);
    }

    public Map<String, FeedIndexingJobStat> stats() {
        return runningJobs;
    }

    public CsvSchema detectSchema(File file, Charset charset) throws IOException {
        LOGGER.info("Autodetecting CSV schema for file {} (charset {})", file.getAbsolutePath(), charset);
        CsvSchema schema = csvDialectDetector.detectSchema(file, charset);
        LOGGER.warn("Auto detected schema is quoteChar:{} separatorChar:{} escapeChar:none",
                schema.getQuoteChar() == -1 ? "none" : Character.toString((char) schema.getQuoteChar()),
                schema.getColumnSeparator() == -1 ? "none" : Character.toString((char) schema.getColumnSeparator()));
        return schema;
    }

    public void finished(final FeedIndexingJobStat fetchingJobStats, final DataSourceProperties dataSourceProperties) {
        LOGGER.info("Datasource feed indexing of {} is terminated", dataSourceProperties.getName());
        runningJobs.put(dataSourceProperties.getDatasourceConfigName(), fetchingJobStats);
    }

    Logger createDatasourceLogger(final String datasourceConfigName, final DataSourceProperties datasourceConfig,
            final String logDir) {
        Level level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
        if (Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.OFF).equals(Level.OFF)) {
            LOGGER.warn("Specific logging for {} is not or badly defined. Turned warn", datasourceConfigName);
            level = Level.toLevel(datasourceConfig.getDedicatedLogLevel(), Level.WARN);
        }
        return GenericFileLogger.initLogger(datasourceConfigName, level, logDir);
    }

    @PreDestroy
    private void destroy() {
        for (final String provider : runningJobs.keySet().stream().collect(Collectors.toSet())) {
            stop(provider);
        }
    }

    public BlockingQueue<DataSourceProperties> getQueue() {
        return queue;
    }

    public synchronized void incrementFeedNoUrls() {
        feedNoUrls.incrementAndGet();
    }

    public synchronized void brokenCsv(String url) {
        brokenCsvFiles.add(url);
    }

    @Override
    public Health health() {
        return Health.up()
                .withDetail("feed_without_urls", feedNoUrls.get())
                .withDetail("invalid_csv_files", StringUtils.join(brokenCsvFiles, "\n"))
                .build();
    }
}
