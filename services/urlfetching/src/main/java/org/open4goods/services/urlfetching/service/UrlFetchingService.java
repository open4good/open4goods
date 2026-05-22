package org.open4goods.services.urlfetching.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.Map;

import tools.jackson.databind.ObjectMapper;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.fetchers.HttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.ProxifiedHttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.PlaywrightHttpFetcher;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.StructuredMetadataExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service that orchestrates URL fetching based on domain configuration.
 * <p>
 * When the property {@code urlfetcher.record.enabled} is set to true, the service will record
 * (i.e. write to file) each successful fetch response as a JSON file into the destination folder
 * specified by {@code urlfetcher.record.destinationFolder}. This feature is primarily intended for
 * test or mock generation purposes.
 * </p>
 */
@Service
public class UrlFetchingService {

    private static final Logger logger = LoggerFactory.getLogger(UrlFetchingService.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String FETCH_MODE_HEADER = "X-Open4goods-Fetch-Mode";
    private static final String FETCH_PROVIDER_HEADER = "X-Open4goods-Fetch-Provider";
    private static final String PLAYWRIGHT_PROXY_HEADER = "X-Open4goods-Playwright-Proxy";
    public static final String EXPECTED_GTIN_HEADER = "X-Open4goods-Expected-Gtin";
    public static final String FORCE_FETCH_HEADER = "X-Open4goods-Force-Fetch";

    private final UrlFetcherConfig urlFetcherConfig;
    private final MeterRegistry meterRegistry;
    private final Executor executor;

    /**
     * Constructs a new UrlFetchingService.
     *
     * @param urlFetcherConfig the URL fetcher configuration properties
     * @param meterRegistry    the Micrometer MeterRegistry for metrics
     */
    public UrlFetchingService(UrlFetcherConfig urlFetcherConfig, MeterRegistry meterRegistry) {
        this.urlFetcherConfig = urlFetcherConfig;
        this.meterRegistry = meterRegistry;
        this.executor = Executors.newFixedThreadPool(urlFetcherConfig.getThreadPoolSize());
    }

    /**
     * Fetches a URL using the appropriate strategy based on the domain.
     * <p>
     * If recording mode is enabled (i.e. {@code urlfetcher.record.enabled} is true), the fetch response
     * is also recorded to a file for later playback in tests.
     * </p>
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url) {
        return fetchUrlAsync(url, null);
    }

    /**
     * Fetches a URL using the appropriate strategy based on the domain, with custom headers.
     * <p>
     * If recording mode is enabled (i.e. {@code urlfetcher.record.enabled} is true), the fetch response
     * is also recorded to a file for later playback in tests.
     * </p>
     *
     * @param url the URL to fetch
     * @param headers custom headers to add to the request
     * @return a CompletableFuture of FetchResponse
     */
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url, Map<String, String> headers) {
        logger.info("URL_FETCH url={} phase=select", url);
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
            logger.warn("URL_FETCH domain={} phase=select outcome=defaultConfig strategy=HTTP", domain);
            domainConfig = new DomainConfig();
            domainConfig.setUserAgent("DefaultUserAgent/1.0");
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }
        domainConfig = withRuntimeStrategyOverride(domainConfig, headers);
        boolean forcePlaywrightProxy = requestedPlaywrightProxy(headers);
        String expectedGtin = headers == null ? null : headers.get(EXPECTED_GTIN_HEADER); //TODO : Why expectedGtin is Empty ?
        Map<String, String> outboundHeaders = outboundHeaders(headers);

        logger.info("URL_FETCH domain={} phase=select strategy={} timeoutMs={} customHeaderNames={}",
                domain, domainConfig.getStrategy(), domainConfig.getTimeout(),
                domainConfig.getCustomHeaders() == null ? java.util.Set.of() : domainConfig.getCustomHeaders().keySet());
        Fetcher fetcher = getFetcherForStrategy(domainConfig, forcePlaywrightProxy);
        // TODO : Here the strategy for reverse proxying http proxy seems to fail
        CompletableFuture<FetchResponse> future = fetcher.fetchUrlAsync(url, outboundHeaders);
        return future.thenApply(response -> {
            FetchResponse effectiveResponse = rejectOnGtinMismatch(response, expectedGtin);
            // Recording mode: if enabled, record the fetch response to file.
            if (urlFetcherConfig.getRecord() != null && urlFetcherConfig.getRecord().isEnabled()) {
                try {
                    recordResponse(url, effectiveResponse);
                } catch (Exception e) {
                    logger.error("Failed to record response for URL {}: {}", url, e.getMessage());
                }
            }
            return effectiveResponse;
        });
    }
    public FetchResponse fetchUrlSync(String url) throws IOException, InterruptedException {
        logger.info("URL_FETCH url={} phase=select", url);
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
            logger.warn("URL_FETCH domain={} phase=select outcome=defaultConfig strategy=HTTP", domain);
            domainConfig = new DomainConfig();
            domainConfig.setUserAgent("DefaultUserAgent/1.0");
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }

        Fetcher fetcher = getFetcherForStrategy(domainConfig, false);
        FetchResponse response = fetcher.fetchUrlSync(url);
        recordResponse(url, response);
        return response;


    }

    /**
     * Returns a Fetcher implementation based on the configured strategy.
     *
     * @param domainConfig the domain configuration
     * @return a Fetcher instance
     */
    private Fetcher getFetcherForStrategy(DomainConfig domainConfig, boolean forcePlaywrightProxy) {
        if (domainConfig.getStrategy() == null) {
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }
        switch (domainConfig.getStrategy()) {
            case PROXIFIED:
                logger.info("URL_FETCH strategy=PROXIFIED phase=selected");
                return new ProxifiedHttpFetcher(domainConfig, urlFetcherConfig.getProxy(), executor, meterRegistry);
            case PLAYWRIGHT:
                logger.info("URL_FETCH strategy=PLAYWRIGHT phase=selected");
                return new PlaywrightHttpFetcher(domainConfig, urlFetcherConfig.getProxy(),
                        urlFetcherConfig.isPlaywrightProxyFallbackEnabled(),
                        urlFetcherConfig.isPlaywrightProxyRequired() || forcePlaywrightProxy, meterRegistry);
            case HTTP:
                logger.info("URL_FETCH strategy=HTTP phase=selected");
                return new HttpFetcher(domainConfig, executor, meterRegistry);
            default:
                logger.info("URL_FETCH strategy=PLAYWRIGHT phase=selected defaultFallback=true");
                return new PlaywrightHttpFetcher(domainConfig, urlFetcherConfig.getProxy(),
                        urlFetcherConfig.isPlaywrightProxyFallbackEnabled(),
                        urlFetcherConfig.isPlaywrightProxyRequired() || forcePlaywrightProxy, meterRegistry);
        }
    }

    private DomainConfig withRuntimeStrategyOverride(DomainConfig original, Map<String, String> headers) {
        FetchStrategy override = requestedStrategy(headers);
        if (override == null) {
            return original;
        }
        DomainConfig overridden = new DomainConfig();
        overridden.setUserAgent(original.getUserAgent());
        overridden.setStrategy(override);
        overridden.setCustomHeaders(original.getCustomHeaders());
        overridden.setTimeout(original.getTimeout());
        overridden.setRetryPolicy(original.getRetryPolicy());
        overridden.setBrowserChannel(original.getBrowserChannel());
        logger.info("URL_FETCH phase=select runtimeStrategyOverride={}", override);
        return overridden;
    }

    private boolean requestedPlaywrightProxy(Map<String, String> headers) {
        return headers != null && Boolean.parseBoolean(headers.get(PLAYWRIGHT_PROXY_HEADER));
    }

    private FetchStrategy requestedStrategy(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        String provider = headers.get(FETCH_PROVIDER_HEADER);
        if (provider != null && provider.equalsIgnoreCase("zenrows")) {
            return FetchStrategy.PROXIFIED;
        }
        String mode = headers.get(FETCH_MODE_HEADER);
        if (mode == null || mode.isBlank()) {
            return null;
        }
        return switch (mode.trim().toLowerCase()) {
            case "http", "http_simple" -> FetchStrategy.HTTP;
            case "playwright", "playwright_headless" -> FetchStrategy.PLAYWRIGHT;
            case "proxified", "zenrows" -> FetchStrategy.PROXIFIED;
            default -> null;
        };
    }

    private Map<String, String> outboundHeaders(Map<String, String> headers) {
        if (headers == null || headers.isEmpty()) {
            return headers;
        }
        Map<String, String> outbound = new java.util.HashMap<>(headers);
        outbound.remove(FETCH_MODE_HEADER);
        outbound.remove(FETCH_PROVIDER_HEADER);
        outbound.remove(PLAYWRIGHT_PROXY_HEADER);
        outbound.remove(EXPECTED_GTIN_HEADER);
        outbound.remove(FORCE_FETCH_HEADER);
        return outbound.isEmpty() ? null : outbound;
    }

    private FetchResponse rejectOnGtinMismatch(FetchResponse response, String expectedGtin) {
        if (response == null || expectedGtin == null || expectedGtin.isBlank() || response.extractedGtins() == null
                || response.extractedGtins().isEmpty()) {
            return response;
        }
        String normalizedExpected = StructuredMetadataExtractor.normalizeGtin(expectedGtin);
        boolean matched = response.extractedGtins().stream()
                .map(StructuredMetadataExtractor::normalizeGtin)
                .anyMatch(normalizedExpected::equals);
        if (matched) {
            return response;
        }
        logger.warn("URL_FETCH url={} phase=metadataValidation outcome=rejected expectedGtin={} extractedGtins={}",
                response.url(), normalizedExpected, response.extractedGtins());
        return response.withRejection(409, "Structured metadata GTIN does not match requested GTIN");
    }

    /**
     * Extracts the domain from the URL.
     *
     * @param url the URL string
     * @return the domain part of the URL
     */
    private String getDomainFromUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            logger.error("Invalid URL: {}", url, e);
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    /**
     * Records the fetch response to a file in the designated destination folder.
     *
     * @param url      the URL that was fetched
     * @param response the fetch response to record
     * @throws IOException if an I/O error occurs during recording
     */
    private void recordResponse(String url, FetchResponse response) throws IOException {
        String destinationFolder = urlFetcherConfig.getRecord().getDestinationFolder();
        Path folderPath = Paths.get(destinationFolder);
        Files.createDirectories(folderPath);
        String fileName = sanitizeUrlToFileName(url);
        Path filePath = folderPath.resolve(fileName);
        objectMapper.writeValue(filePath.toFile(), response);
        logger.info("Recorded fetch response for URL {} at {}", url, filePath.toAbsolutePath());
    }

    /**
     * Sanitizes a URL into a safe file name by removing the protocol and replacing non-alphanumeric characters.
     *
     * @param url the URL to sanitize
     * @return a sanitized file name ending with .json
     */
    private static String sanitizeUrlToFileName(String url) {
        String sanitized = url.replaceFirst("^(https?://)", "").replaceAll("[^a-zA-Z0-9]", "_");
        return sanitized + ".txt";
    }
}
