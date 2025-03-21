package org.open4goods.services.favicon.service;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.tika.Tika;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.open4goods.model.exceptions.InvalidParameterException;
import org.open4goods.services.favicon.config.FaviconConfig;
import org.open4goods.services.favicon.dto.FaviconResponse;
import org.open4goods.services.favicon.exception.FaviconException;
import org.open4goods.services.remotefilecaching.service.RemoteFileCachingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;

/**
 * Implementation of FaviconService that retrieves favicons for a given URL.
 * <p>
 * It supports direct domain mapping from configuration or, if not available,
 * fetching the root domain page and parsing for favicon links (using jsoup).
 * It also caches favicons (including their MIME type) in memory and updates actuator metrics.
 * Additionally, the service accepts either a full URL or just a domain name (e.g. "google.com").
 * </p>
 */
@Service
public class FaviconServiceImpl implements FaviconService, HealthIndicator {

    private static final Logger logger = LoggerFactory.getLogger(FaviconServiceImpl.class);

    private final FaviconConfig faviconConfig;
    private final RemoteFileCachingService remoteFileCachingService;
    private final MeterRegistry meterRegistry;

    // In-memory cache: key is the URL, value is FaviconResponse (data + content type).
    private final Map<String, FaviconResponse> faviconCache = new ConcurrentHashMap<>();

    // Metrics counters.
    private final Counter faviconsReturnedCounter;
    private final Counter faviconsFetchedOkCounter;
    private final Counter faviconsFetchedKoCounter;

    // Favicon link relation priorities.
    private static final String[] FAVICON_REL_PRIORITIES = {
            "icon", "shortcut icon", "apple-touch-icon", "apple-touch-icon-precomposed", "mask-icon", "fluid-icon"
    };

    private final Tika tika = new Tika();

    /**
     * Constructs a new FaviconServiceImpl.
     *
     * @param faviconConfig the favicon configuration properties.
     * @param remoteFileCachingService the service for caching remote files.
     * @param meterRegistry the MeterRegistry for metrics.
     */
    public FaviconServiceImpl(FaviconConfig faviconConfig,
                              RemoteFileCachingService remoteFileCachingService,
                              MeterRegistry meterRegistry) {
        this.faviconConfig = faviconConfig;
        this.remoteFileCachingService = remoteFileCachingService;
        this.meterRegistry = meterRegistry;
        this.faviconsReturnedCounter = meterRegistry.counter("favicon.returned.count");
        this.faviconsFetchedOkCounter = meterRegistry.counter("favicon.fetched.ok.count");
        this.faviconsFetchedKoCounter = meterRegistry.counter("favicon.fetched.ko.count");
    }

    /**
     * Normalizes the provided URL. If the URL does not contain a scheme (e.g. "http://"),
     * it is prefixed with "http://".
     *
     * @param url the input URL or domain name.
     * @return a fully-qualified URL.
     */
    private String normalizeUrl(String url) {
        if (url == null) {
            return null;
        }
        if (!url.contains("://")) {
            return "http://" + url;
        }
        return url;
    }

    @Override
    public boolean hasFavicon(String url) {
        try {
            // Normalize URL and attempt to retrieve the favicon.
            FaviconResponse response = getFavicon(url);
            return response != null && response.faviconData() != null && response.faviconData().length > 0;
        } catch (FaviconException e) {
            logger.info("Favicon not found for URL {}: {}", url, e.getMessage());
            return false;
        }
    }

    @Override
    public FaviconResponse getFavicon(String url) {
        if (!StringUtils.hasText(url)) {
            throw new FaviconException("URL must not be empty");
        }
        // Normalize URL to support both full URLs and bare domain names.
        url = normalizeUrl(url);

        // Check in-memory cache.
        if (faviconCache.containsKey(url)) {
            faviconsReturnedCounter.increment();
            logger.info("Returning favicon from in-memory cache for URL: {}", url);
            return faviconCache.get(url);
        }

        try {
            String domain = extractDomain(url);
            FaviconResponse faviconResponse;

            // Check for a direct mapping.
            if (faviconConfig.getDomainMapping() != null && faviconConfig.getDomainMapping().containsKey(domain)) {
                String mappedResource = faviconConfig.getDomainMapping().get(domain);
                logger.info("Domain {} found in direct mapping. Using resource: {}", domain, mappedResource);
                faviconResponse = fetchFaviconAndDetectType(mappedResource);
            } else {
                // Otherwise, fetch and parse the root page.
                String rootUrl = getRootUrl(url);
                logger.info("Fetching HTML from root URL: {}", rootUrl);
                Document doc = Jsoup.connect(rootUrl).get();
                faviconResponse = parseFaviconFromDocument(doc, rootUrl);
            }

            if (faviconResponse == null || faviconResponse.faviconData() == null || faviconResponse.faviconData().length == 0) {
                faviconsFetchedKoCounter.increment();
                throw new FaviconException("Favicon not found for URL: " + url);
            }

            // Cache the result in memory.
            faviconCache.put(url, faviconResponse);
            faviconsFetchedOkCounter.increment();
            faviconsReturnedCounter.increment();
            logger.info("Favicon fetched and cached for URL: {}", url);
            return faviconResponse;
        } catch (IOException e) {
            faviconsFetchedKoCounter.increment();
            logger.error("I/O error retrieving favicon for URL {}: {}", url, e.getMessage());
            throw new FaviconException("I/O error retrieving favicon for URL: " + url, e);
        } catch (Exception e) {
            faviconsFetchedKoCounter.increment();
            logger.error("Error retrieving favicon for URL {}: {}", url, e.getMessage());
            throw new FaviconException("Error retrieving favicon for URL: " + url, e);
        }
    }

    @Override
    public void clearCache() {
        faviconCache.clear();
        logger.info("In-memory favicon cache cleared.");
        // Optionally clear file cache as well.
    }

    /**
     * Extracts the domain from the URL.
     */
    private String extractDomain(String url) throws Exception {
        URI uri = new URI(url);
        String domain = uri.getHost();
        if (domain == null) {
            throw new Exception("Invalid URL: " + url);
        }
        return domain.startsWith("www.") ? domain.substring(4) : domain;
    }

    /**
     * Returns the root URL (scheme + host) for a given URL.
     */
    private String getRootUrl(String url) throws Exception {
        URI uri = new URI(url);
        String scheme = uri.getScheme();
        String domain = uri.getHost();
        if (scheme == null || domain == null) {
            throw new Exception("Invalid URL: " + url);
        }
        return scheme + "://" + domain;
    }

    /**
     * Fetches favicon data from a resource location and detects its MIME type.
     * The resource may be a URL, file system path, or classpath resource.
     * @throws InvalidParameterException 
     */
    private FaviconResponse fetchFaviconAndDetectType(String resourceLocation) throws IOException, InvalidParameterException {
        File faviconFile;
        if (resourceLocation.startsWith("http")) {
            faviconFile = remoteFileCachingService.getResource(resourceLocation);
        } else if (resourceLocation.startsWith("file:")) {
            faviconFile = new File(resourceLocation.substring(5));
        } else if (resourceLocation.startsWith("classpath:")) {
            var resource = getClass().getResource(resourceLocation.substring(10));
            if (resource == null) {
                throw new IOException("Classpath resource not found: " + resourceLocation);
            }
            faviconFile = new File(resource.getFile());
        } else {
            faviconFile = new File(resourceLocation);
        }
        if (!faviconFile.exists()) {
            throw new IOException("Favicon resource not found at: " + resourceLocation);
        }
        byte[] data = java.nio.file.Files.readAllBytes(faviconFile.toPath());
        String mimeType = detectMimeType(data, resourceLocation);
        return new FaviconResponse(data, mimeType);
    }

    /**
     * Parses the HTML document for a favicon link using the defined priority order.
     */
    private FaviconResponse parseFaviconFromDocument(Document doc, String baseUrl) throws IOException {
        for (String rel : FAVICON_REL_PRIORITIES) {
            Elements links = doc.select("link[rel~=(?i)" + rel + "]");
            if (!links.isEmpty()) {
                for (Element link : links) {
                    String href = link.attr("href");
                    if (StringUtils.hasText(href)) {
                        String faviconUrl = resolveUrl(baseUrl, href);
                        logger.info("Found favicon link with rel='{}': {}", rel, faviconUrl);
                        try {
                            File faviconFile = remoteFileCachingService.getResource(faviconUrl);
                            if (faviconFile.exists()) {
                                byte[] data = java.nio.file.Files.readAllBytes(faviconFile.toPath());
                                String mimeType = detectMimeType(data, faviconUrl);
                                return new FaviconResponse(data, mimeType);
                            }
                        } catch (Exception e) {
                            logger.warn("Failed to fetch favicon from {}: {}", faviconUrl, e.getMessage());
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Resolves a relative URL against a base URL.
     */
    private String resolveUrl(String baseUrl, String relativeUrl) {
        try {
            URL base = new URL(baseUrl);
            URL resolved = new URL(base, relativeUrl);
            return resolved.toString();
        } catch (Exception e) {
            logger.warn("Error resolving URL. Base: {}, Relative: {}. Error: {}", baseUrl, relativeUrl, e.getMessage());
            return relativeUrl;
        }
    }

    /**
     * Detects the MIME type using Apache Tika.
     * If Tika fails, it falls back to checking the resource URL extension and defaults to "image/png".
     *
     * @param data the favicon bytes.
     * @param resourceUrl the URL or resource location of the favicon.
     * @return the detected MIME type.
     */
    private String detectMimeType(byte[] data, String resourceUrl) {
        String mimeType = null;
        try {
            mimeType = tika.detect(data);
            if (mimeType == null || mimeType.isBlank() || "application/octet-stream".equals(mimeType)) {
                logger.warn("Tika detection returned unhelpful MIME type for {}. Falling back to file extension.", resourceUrl);
            } else {
                return mimeType;
            }
        } catch (Exception e) {
            logger.warn("Tika detection failed for {}: {}. Falling back to file extension.", resourceUrl, e.getMessage());
        }
        // Fallback: check the resource URL file extension.
        String lowerUrl = resourceUrl.toLowerCase();
        if (lowerUrl.endsWith(".ico")) {
            return "image/x-icon";
        } else if (lowerUrl.endsWith(".svg")) {
            return "image/svg+xml";
        } else if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        }
        // Default to image/png.
        return "image/png";
    }

    /**
     * Health check: the service is healthy if the cache folder is configured and accessible.
     */
    @Override
    public Health health() {
        if (faviconConfig.getCacheFolder() == null || faviconConfig.getCacheFolder().isBlank()) {
            logger.error("Cache folder is not configured.");
            return Health.down().withDetail("error", "Cache folder not configured").build();
        }
        File folder = new File(faviconConfig.getCacheFolder());
        if (!folder.exists() || !folder.canWrite()) {
            logger.error("Cache folder {} is not accessible.", faviconConfig.getCacheFolder());
            return Health.down().withDetail("error", "Cache folder not accessible").build();
        }
        return Health.up().build();
    }
}
