package org.open4goods.services.urlfetching.service;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.fetchers.HttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.ProxifiedHttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.SeleniumHttpFetcher;
import org.open4goods.services.urlfetching.service.Fetcher;
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
        logger.info("Initiating fetch for URL: {}", url);
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
            logger.warn("No specific configuration found for domain '{}'. Using default configuration.", domain);
            domainConfig = new DomainConfig();
            domainConfig.setUserAgent("DefaultUserAgent/1.0");
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }

        Fetcher fetcher = getFetcherForStrategy(domainConfig);
        CompletableFuture<FetchResponse> future = fetcher.fetchUrlAsync(url, headers);
        return future.thenApply(response -> {
            // Recording mode: if enabled, record the fetch response to file.
            if (urlFetcherConfig.getRecord() != null && urlFetcherConfig.getRecord().isEnabled()) {
                try {
                    recordResponse(url, response);
                } catch (Exception e) {
                    logger.error("Failed to record response for URL {}: {}", url, e.getMessage());
                }
            }
            return response;
        });
    }
    public FetchResponse fetchUrlSync(String url) throws IOException, InterruptedException {
        logger.info("Initiating fetch for URL: {}", url);
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
            logger.warn("No specific configuration found for domain '{}'. Using default configuration.", domain);
            domainConfig = new DomainConfig();
            domainConfig.setUserAgent("DefaultUserAgent/1.0");
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }

        Fetcher fetcher = getFetcherForStrategy(domainConfig);
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
    private Fetcher getFetcherForStrategy(DomainConfig domainConfig) {
        if (domainConfig.getStrategy() == null) {
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }
        switch (domainConfig.getStrategy()) {
            case PROXIFIED:
                logger.info("Selected PROXIFIED strategy for fetching");
                return new ProxifiedHttpFetcher(domainConfig, executor, meterRegistry);
            case SELENIUM:
                logger.info("Selected SELENIUM strategy for fetching");
                return new SeleniumHttpFetcher(domainConfig, meterRegistry);
            case HTTP:
                logger.info("Selected HTTP strategy for fetching");
                return new HttpFetcher(domainConfig, executor, meterRegistry);
            default:
                logger.info("Selected SELENIUM strategy for fetching (default)");
                return new SeleniumHttpFetcher(domainConfig, meterRegistry);
        }
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
