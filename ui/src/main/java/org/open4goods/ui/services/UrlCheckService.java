package org.open4goods.ui.services;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamReader;

import org.open4goods.ui.config.yml.UrlCheckConfig;
import org.open4goods.ui.model.CheckedUrl;
import org.open4goods.ui.repository.CheckedUrlRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Health.Builder;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import crawlercommons.sitemaps.AbstractSiteMap;
import crawlercommons.sitemaps.SiteMap;
import crawlercommons.sitemaps.SiteMapIndex;
import crawlercommons.sitemaps.SiteMapParser;
import crawlercommons.sitemaps.SiteMapURL;
import crawlercommons.sitemaps.UnknownFormatException;

/**
 * Service that:
 *  1) Reads (and re-reads) a sitemap (including indexes) from a URL in the config and stores new URLs in Elasticsearch.
 *  2) Checks all stored URLs in a multi-threaded fashion, collecting stats about:
 *     HTTP status codes, response times, found patterns, etc.
 *  3) Exposes a HealthIndicator (Actuator) status based on presence of URLs, errors, or bad patterns.
 */
@Service
public class UrlCheckService implements HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(UrlCheckService.class);

    // Health check messages
    private final Map<String, String> infosHealthCheck = new ConcurrentHashMap<>();
    private final Map<String, String> errorsHealthCheck = new ConcurrentHashMap<>();

    // Injected config properties
    private final UrlCheckConfig config;

    // Elasticsearch repository
    private final CheckedUrlRepository checkedUrlRepository;

    // Executor & HTTP client
    private final ExecutorService executor;
    private final HttpClient client;

    // Counters
    private final AtomicInteger totalUrlsTested      = new AtomicInteger(0);
    private final AtomicInteger total500Errors       = new AtomicInteger(0);
    private final AtomicInteger totalBadPatternHits  = new AtomicInteger(0);
    private final AtomicInteger totalRedirects       = new AtomicInteger(0);
    private final AtomicInteger totalOtherStatus     = new AtomicInteger(0);

    private volatile boolean lastCrawlHad500Errors   = false;
    private volatile boolean lastCrawlHadBadPatterns = false;

    public UrlCheckService(CheckedUrlRepository checkedUrlRepository, UrlCheckConfig config) {
        this.checkedUrlRepository = checkedUrlRepository;
        this.config = config;

        // Create the executor with the configured thread pool size
        int poolSize = config.getThreadPoolSize();
        logger.info("Creating fixed thread pool of size {}", poolSize);
        this.executor = Executors.newFixedThreadPool(poolSize);

        // Create an HttpClient
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * Schedules a weekly read of sitemap at 11:10 AM on Mondays (example).
     * The sitemap URL is taken from application.yml config (config.getSitemapUrl()).
     */
    @Scheduled(cron = "0 10 11 ? * MON")
    public void scheduledSitemapRead() {
        logger.info("scheduledSitemapRead() :: Start");
        String sitemapUrl = config.getSitemapUrl();
        logger.info("Running weekly sitemap read for sitemap: {}", sitemapUrl);

        // Clear old messages
        infosHealthCheck.clear();
        errorsHealthCheck.clear();

        try {
            readSitemapAndStore(sitemapUrl);
        } catch (Exception e) {
            logger.error("Error reading sitemap on schedule: {}", e.getMessage(), e);
            errorsHealthCheck.put("SCHEDULED_READ_ERROR", e.getMessage());
        }
    }

    /**
     * Schedules a daily check of all URLs at 06:27 AM.
     */
    @Scheduled(cron = "0 27 6 * * ?")
    public void scheduledDailyCheckAllUrls() {
        logger.info("scheduledDailyCheckAllUrls() :: Start");
        logger.info("Running daily checkAllUrls() at 06:27 AM");

        // Clear old messages
        infosHealthCheck.clear();
        errorsHealthCheck.clear();

        checkAllUrls();
    }

    /**
     * Reads a sitemap (or sitemap index) from the given URL and stores new URLs in Elasticsearch.
     */
    public void readSitemapAndStore(String sitemapUrl) throws IOException, UnknownFormatException {
        logger.info("readSitemapAndStore() :: Start for {}", sitemapUrl);

        SiteMapParser parser = new SiteMapParser();
        URL url = new URL(sitemapUrl);

        try (BufferedInputStream ignored = new BufferedInputStream(url.openStream())) {
            AbstractSiteMap sm = parser.parseSiteMap(url);

            if (sm instanceof SiteMapIndex) {
                SiteMapIndex smi = (SiteMapIndex) sm;
                for (AbstractSiteMap child : smi.getSitemaps()) {
                    if (!child.isIndex()) {
                        readSitemapAndStore(child.getUrl().toString());
                    } else {
                        readSitemapAndStore(child.getUrl().toString());
                    }
                }
            } else {
                // Single sitemap
                storeSingleSiteMap((SiteMap) sm);
            }

        } catch (Exception e) {
            logger.warn("crawler-commons parse failed for {}: {}. Falling back to manual parse.", sitemapUrl, e.getMessage());
            fallbackManualParseAndStore(sitemapUrl);
        }
    }

    /**
     * If crawler-commons fails, do a simple manual XML parse for <loc> elements.
     */
    private void fallbackManualParseAndStore(String sitemapUrl) {
        logger.info("fallbackManualParseAndStore() :: Start for {}", sitemapUrl);
        try {
            List<String> urls = parseSitemapUrls(sitemapUrl);
            bulkStoreUrls(urls, sitemapUrl);
        } catch (Exception ex) {
            logger.error("Fallback parse also failed for {} : {}", sitemapUrl, ex.getMessage());
            errorsHealthCheck.put("FALLBACK_PARSE_FAIL_" + sitemapUrl, ex.getMessage());
        }
    }

    /**
     * Stores each URL found in a single (non-index) sitemap if not already present.
     */
    private void storeSingleSiteMap(SiteMap siteMap) {
        logger.info("storeSingleSiteMap() :: Start for {}", siteMap.getUrl());
        if (siteMap == null || siteMap.getSiteMapUrls() == null) {
            logger.error("storeSingleSiteMap: No URLs found or invalid SiteMap object.");
            errorsHealthCheck.put("INVALID_SITEMAP", "Empty or null SiteMap encountered.");
            return;
        }

        List<String> directUrls = new ArrayList<>();
        for (SiteMapURL smu : siteMap.getSiteMapUrls()) {
            directUrls.add(smu.getUrl().toExternalForm());
        }

        if (directUrls.isEmpty()) {
            try {
                fallbackManualParseAndStore(siteMap.getUrl().toString());
            } catch (Exception e) {
                logger.error("Error in fallback parse for {}: {}", siteMap.getUrl(), e.getMessage());
                errorsHealthCheck.put("STORE_SINGLE_SITEMAP_PARSE_ERR", e.getMessage());
            }
        } else {
            bulkStoreUrls(directUrls, siteMap.getUrl().toString());
        }
    }

    /**
     * Bulk stores a list of URLs into Elasticsearch if they do not already exist.
     */
    private void bulkStoreUrls(List<String> urls, String source) {
        logger.info("bulkStoreUrls() :: Start for source {}", source);
        if (urls.isEmpty()) {
            errorsHealthCheck.put("EMPTY_SITEMAP", "No URLs found in " + source);
            logger.error("No URLs found from sitemap: {}", source);
            return;
        }

        List<CheckedUrl> toSave = new ArrayList<>();
        for (String urlValue : urls) {
            if (checkedUrlRepository.findById(urlValue).isEmpty()) {
                CheckedUrl cu = new CheckedUrl(urlValue);
                toSave.add(cu);
            }
        }

        if (!toSave.isEmpty()) {
            checkedUrlRepository.saveAll(toSave);
            logger.info("Stored {} new URLs from sitemap {}", toSave.size(), source);
        } else {
            errorsHealthCheck.put("NO_NEW_URLS_" + source, "All URLs from " + source + " already existed.");
            logger.warn("No new URLs from {}", source);
        }
    }

    /**
     * Manual XML parsing (StAX) to extract <loc> elements from a sitemap or index file.
     */
    public List<String> parseSitemapUrls(String sitemapUrl) throws Exception {
        logger.info("parseSitemapUrls() :: Start for {}", sitemapUrl);
        List<String> urls = new ArrayList<>();
        XMLInputFactory factory = XMLInputFactory.newInstance();

        try (InputStream in = new URL(sitemapUrl).openStream()) {
            XMLStreamReader reader = factory.createXMLStreamReader(in);
            String currentTag = null;

            while (reader.hasNext()) {
                int event = reader.next();
                if (event == XMLStreamConstants.START_ELEMENT) {
                    currentTag = reader.getLocalName();
                } else if (event == XMLStreamConstants.CHARACTERS) {
                    if ("loc".equalsIgnoreCase(currentTag)) {
                        String locValue = reader.getText().trim();
                        if (!locValue.isEmpty()) {
                            urls.add(locValue);
                        }
                    }
                }
            }
            reader.close();
        }
        logger.debug("parseSitemapUrls found {} URLs in {}", urls.size(), sitemapUrl);
        return urls;
    }

    /**
     * Checks all URLs in Elasticsearch in a multi-threaded fashion.
     */
    public void checkAllUrls() {
        logger.info("checkAllUrls() :: Start");

        Iterable<CheckedUrl> allIter = checkedUrlRepository.findAll();
        Iterator<CheckedUrl> all = allIter.iterator();

        // Reset counters
        totalUrlsTested.set(0);
        total500Errors.set(0);
        totalBadPatternHits.set(0);
        totalRedirects.set(0);
        totalOtherStatus.set(0);
        lastCrawlHad500Errors = false;
        lastCrawlHadBadPatterns = false;

        if (!all.hasNext()) {
            logger.warn("No URLs to check! Possibly the sitemap read was empty.");
            errorsHealthCheck.put("NO_URLS_IN_DB", "Elasticsearch contains 0 URLs in checked-urls index.");
            return;
        }

        List<Future<CheckedUrl>> futures = new ArrayList<>();
        all.forEachRemaining(cu -> {
            futures.add(executor.submit(() -> checkSingleUrl(cu)));
        });

        List<CheckedUrl> updatedList = new ArrayList<>();
        for (Future<CheckedUrl> f : futures) {
            try {
                CheckedUrl updated = f.get();
                updatedList.add(updated);
            } catch (InterruptedException | ExecutionException e) {
                logger.error("Error in checkAllUrls tasks: {}", e.getMessage());
                errorsHealthCheck.put("CHECK_TASK_ERROR", e.getMessage());
            }
        }

        checkedUrlRepository.saveAll(updatedList);

        if (total500Errors.get() > 0) {
            lastCrawlHad500Errors = true;
        }
        if (totalBadPatternHits.get() > 0) {
            lastCrawlHadBadPatterns = true;
        }

        infosHealthCheck.put("CHECK_RESULT_tested",      String.valueOf(totalUrlsTested.get()));
        infosHealthCheck.put("CHECK_RESULT_500",         String.valueOf(total500Errors.get()));
        infosHealthCheck.put("CHECK_RESULT_badPattern",  String.valueOf(totalBadPatternHits.get()));
        infosHealthCheck.put("CHECK_RESULT_30x",         String.valueOf(totalRedirects.get()));
        infosHealthCheck.put("CHECK_RESULT_other",       String.valueOf(totalOtherStatus.get()));

        logger.info("Check done. tested={}, 500={}, badPattern={}, 30x={}, other={}",
                totalUrlsTested.get(),
                total500Errors.get(),
                totalBadPatternHits.get(),
                totalRedirects.get(),
                totalOtherStatus.get()
        );
    }

    /**
     * Performs the actual HTTP request for a single CheckedUrl.
     */
    private CheckedUrl checkSingleUrl(CheckedUrl cu) {
        logger.info("checkSingleUrl() :: Start for {}", cu.getUrl());

        long totalStart = System.currentTimeMillis();
        long connectStart;
        long connectEnd;

        int status = 0;
        boolean healthOk = true;
        Set<String> encounteredPatterns = new HashSet<>();

        try {
            var request = java.net.http.HttpRequest.newBuilder()
                    .uri(new URL(cu.getUrl()).toURI())
                    .GET()
                    .timeout(Duration.ofSeconds(15))
                    .build();

            connectStart = System.currentTimeMillis();
            var response = client.send(request, HttpResponse.BodyHandlers.ofString());
            connectEnd = System.currentTimeMillis();

            status = response.statusCode();

            // Evaluate status categories
            if (status >= 300 && status < 400) {
                totalRedirects.incrementAndGet();
                logger.warn("URL {} returned redirect status: {}", cu.getUrl(), status);
            } else if (status == 500) {
                total500Errors.incrementAndGet();
                healthOk = false;
                logger.warn("URL {} returned HTTP 500", cu.getUrl());
            } else if (status != 200) {
                totalOtherStatus.incrementAndGet();
                healthOk = false;
                logger.warn("URL {} returned non-200 status: {}", cu.getUrl(), status);
            }

            // Check for bad patterns using the config's list
            String body = response.body();
            for (String pattern : config.getBadPatterns()) {
                if (body != null && body.contains(pattern)) {
                    encounteredPatterns.add(pattern);
                    healthOk = false;
                }
            }
            if (!encounteredPatterns.isEmpty()) {
                totalBadPatternHits.incrementAndGet();
                logger.warn("URL {} encountered bad patterns: {}", cu.getUrl(), encounteredPatterns);
            }

            long totalEnd = System.currentTimeMillis();

            cu.setLastStatus(status);
            cu.setDurationMillis(totalEnd - totalStart);
            cu.setConnectTimeMillis(connectEnd - connectStart);
            cu.setHealthCheckOk(healthOk);
            cu.setUpdated(System.currentTimeMillis());
            cu.setBadPatternsEncountered(encounteredPatterns);

        } catch (Exception e) {
            logger.error("Error checking URL {}: {}", cu.getUrl(), e.getMessage());
            status = -1;
            healthOk = false;
            totalOtherStatus.incrementAndGet();

            long totalEnd = System.currentTimeMillis();
            cu.setLastStatus(status);
            cu.setDurationMillis(totalEnd - totalStart);
            cu.setConnectTimeMillis(0);
            cu.setHealthCheckOk(false);
            cu.setUpdated(System.currentTimeMillis());
            cu.setBadPatternsEncountered(encounteredPatterns);
        }

        totalUrlsTested.incrementAndGet();
        return cu;
    }

    // ---------------------------------------------------------------------------------------
    // GETTERS for counters, if needed from other classes
    // ---------------------------------------------------------------------------------------
    public int getTotalUrlsTested()     { return totalUrlsTested.get(); }
    public int getTotal500Errors()      { return total500Errors.get(); }
    public int getTotalBadPatternHits() { return totalBadPatternHits.get(); }
    public int getTotalRedirects()      { return totalRedirects.get(); }
    public int getTotalOtherStatus()    { return totalOtherStatus.get(); }
    public boolean lastCrawlHad500Errors()   { return lastCrawlHad500Errors; }
    public boolean lastCrawlHadBadPatterns() { return lastCrawlHadBadPatterns; }

    /**
     * Expose an Actuator Health indicator.
     */
    @Override
    public Health health() {
        logger.info("health() :: Start HealthIndicator evaluation");

        if (lastCrawlHad500Errors) {
            errorsHealthCheck.put("HTTP_500_ERROR", "At least one URL returned HTTP 500 in the last check.");
        }
        if (lastCrawlHadBadPatterns) {
            errorsHealthCheck.put("BAD_PATTERNS_FOUND", "At least one URL contained known bad patterns.");
        }

        Builder builder = Health.up();
        if (!errorsHealthCheck.isEmpty()) {
            builder = Health.down();
        }
        builder.withDetails(infosHealthCheck);
        builder.withDetails(errorsHealthCheck);

        return builder.build();
    }
}
