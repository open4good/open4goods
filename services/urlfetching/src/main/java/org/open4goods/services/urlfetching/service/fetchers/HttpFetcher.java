package org.open4goods.services.urlfetching.service.fetchers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.HtmlToMarkdownConverter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Fetcher} using java.net.http.HttpClient.
 * <p>
 * This implementation supports adding a user agent and custom headers.
 * </p>
 */
public class HttpFetcher implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(HttpFetcher.class);

    private final HttpClient httpClient;
    private final String userAgent;
    private final Map<String, String> customHeaders;
    private final Duration timeout;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs a new HttpFetcher.
     *
     * @param domainConfig  the domain-specific configuration
     * @param executor      the executor to use for asynchronous operations
     * @param meterRegistry the Micrometer MeterRegistry for metrics
     */
    public HttpFetcher(DomainConfig domainConfig, Executor executor, MeterRegistry meterRegistry) {
        this.userAgent = domainConfig.getUserAgent();
        this.customHeaders = domainConfig.getCustomHeaders();
        this.timeout = Duration.ofMillis(domainConfig.getTimeout());
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(this.timeout)
                .build();
    }

    /**
     * Asynchronously fetches the URL using HttpClient.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    @Override
    public CompletableFuture<FetchResponse> fetchUrl(String url) {
        logger.info("Fetching URL {} using HttpFetcher", url);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(timeout)
                .header("User-Agent", this.userAgent);

        // Add custom headers if available
        if (customHeaders != null) {
            customHeaders.forEach(requestBuilder::header);
        }

        HttpRequest request = requestBuilder.build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    String htmlContent = response.body();
                    String markdownContent = HtmlToMarkdownConverter.convert(htmlContent);
                    // Increment metrics
                    meterRegistry.counter("url.fetch.total").increment();
                    meterRegistry.counter("url.fetch.status", "code", String.valueOf(statusCode)).increment();
                    logger.info("Fetched URL {} with status code {}", url, statusCode);
                    return new FetchResponse(url, statusCode, htmlContent, markdownContent,FetchStrategy.HTTP);
                });
    }
}
