package org.open4goods.services.urlfetching.service;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import io.micrometer.core.instrument.MeterRegistry;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.fetchers.HttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.ProxifiedHttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.SeleniumHttpFetcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service that orchestrates URL fetching based on domain configuration.
 */
@Service
public class UrlFetchingService {

    private static final Logger logger = LoggerFactory.getLogger(UrlFetchingService.class);

    private final UrlFetcherConfig urlFetcherConfig;
    private final MeterRegistry meterRegistry;
    private final Executor executor;

    /**
     * Constructs a new UrlFetchingService.
     *
     * @param urlFetcherConfig the URL fetcher configuration properties
     * @param meterRegistry    the Micrometer MeterRegistry for metrics
     * @param executor         the Executor for asynchronous operations
     */
    public UrlFetchingService(UrlFetcherConfig urlFetcherConfig, MeterRegistry meterRegistry) {
        this.urlFetcherConfig = urlFetcherConfig;
        this.meterRegistry = meterRegistry;
        this.executor = Executors.newFixedThreadPool(urlFetcherConfig.getThreadPoolSize());
    }

    /**
     * Fetches a URL using the appropriate strategy based on the domain.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    public CompletableFuture<FetchResponse> fetchUrl(String url) {
        logger.info("Initiating fetch for URL: {}", url);
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
            logger.warn("No specific configuration found for domain '{}'. Using default configuration.", domain);
            // Use a default configuration if none is provided for the domain
            domainConfig = new DomainConfig();
            domainConfig.setUserAgent("DefaultUserAgent/1.0");
            domainConfig.setStrategy(FetchStrategy.HTTP);
        }

        Fetcher fetcher = getFetcherForStrategy(domainConfig);
        // Apply retry policy if defined
        int maxRetries = domainConfig.getRetryPolicy() != null ? domainConfig.getRetryPolicy().getMaxRetries() : 0;
        return executeWithRetry(fetcher, url, maxRetries);
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
            default:
                logger.info("Selected HTTP strategy for fetching");
                return new HttpFetcher(domainConfig, executor, meterRegistry);
        }
    }

    /**
     * Executes the fetch operation with retry logic.
     *
     * @param fetcher    the Fetcher to use
     * @param url        the URL to fetch
     * @param maxRetries maximum number of retries
     * @return a CompletableFuture of FetchResponse
     */
    private CompletableFuture<FetchResponse> executeWithRetry(Fetcher fetcher, String url, int maxRetries) {
        return fetcher.fetchUrl(url).handle((response, ex) -> {
            if (ex == null) {
                return CompletableFuture.completedFuture(response);
            } else if (maxRetries > 0) {
                logger.warn("Error fetching URL {}. Retrying... Remaining retries: {}. Error: {}",
                        url, maxRetries, ex.getMessage());
                try {
                    // Delay before retrying based on configuration
                    DomainConfig config = urlFetcherConfig.getDomains().get(getDomainFromUrl(url));
                    Thread.sleep(config.getRetryPolicy().getDelayBetweenRetries());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return executeWithRetry(fetcher, url, maxRetries - 1);
            } else {
                logger.error("Failed to fetch URL {} after retries. Error: {}", url, ex.getMessage());
                CompletableFuture<FetchResponse> failedFuture = new CompletableFuture<>();
                failedFuture.completeExceptionally(ex);
                return failedFuture;
            }
        }).thenCompose(future -> future);
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
}
