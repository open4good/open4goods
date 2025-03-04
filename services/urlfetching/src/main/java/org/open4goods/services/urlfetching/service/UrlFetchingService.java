package org.open4goods.services.urlfetching.service;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import io.micrometer.core.instrument.MeterRegistry;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.fetchers.HttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.ProxifiedHttpFetcher;
import org.open4goods.services.urlfetching.service.fetchers.SeleniumHttpFetcher;
import org.springframework.stereotype.Service;

/**
 * Service that orchestrates URL fetching based on domain configuration.
 */
@Service
public class UrlFetchingService {

    private final UrlFetcherConfig urlFetcherConfig;
    private final MeterRegistry meterRegistry;

    // Shared executor for HTTP-based fetchers
    // TODO : From conf
    private final java.util.concurrent.Executor executor = Executors.newFixedThreadPool(10);

    /**
     * Constructs a new UrlFetchingService.
     *
     * @param urlFetcherConfig the URL fetcher configuration properties
     * @param meterRegistry    the Micrometer MeterRegistry for metrics
     */
    public UrlFetchingService(UrlFetcherConfig urlFetcherConfig, MeterRegistry meterRegistry) {
        this.urlFetcherConfig = urlFetcherConfig;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Fetches a URL using the appropriate strategy based on the domain.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    public CompletableFuture<FetchResponse> fetchUrl(String url) {
        String domain = getDomainFromUrl(url);
        DomainConfig domainConfig = urlFetcherConfig.getDomains().get(domain);
        if (domainConfig == null) {
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
                return new ProxifiedHttpFetcher(domainConfig, executor, meterRegistry);
            case SELENIUM:
                return new SeleniumHttpFetcher(domainConfig, meterRegistry);
            case HTTP:
            default:
                return new HttpFetcher(domainConfig, executor, meterRegistry);
        }
    }

    /**
     * Executes the fetch operation with retry logic.
     *
     * @param fetcher   the Fetcher to use
     * @param url       the URL to fetch
     * @param maxRetries maximum number of retries
     * @return a CompletableFuture of FetchResponse
     */
    private CompletableFuture<FetchResponse> executeWithRetry(Fetcher fetcher, String url, int maxRetries) {
        return fetcher.fetchUrl(url).handle((response, ex) -> {
            if (ex == null) {
                return CompletableFuture.completedFuture(response);
            } else if (maxRetries > 0) {
                try {
                    // Delay before retrying based on configuration
                    DomainConfig config = urlFetcherConfig.getDomains().get(getDomainFromUrl(url));
                    Thread.sleep(config.getRetryPolicy().getDelayBetweenRetries());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return executeWithRetry(fetcher, url, maxRetries - 1);
            } else {
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
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }
}
