package org.open4goods.services.urlfetching.service.fetchers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.ProxySelector;
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
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.ProxyConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.FetchResponseFactory;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link Fetcher} that supports fetching via a proxy.
 */
public class ProxifiedHttpFetcher implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(ProxifiedHttpFetcher.class);

    private final HttpClient httpClient;
    private final String userAgent;
    private final Map<String, String> customHeaders;
    private final Duration timeout;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs a new ProxifiedHttpFetcher.
     *
     * @param domainConfig  the domain-specific fetch configuration
     * @param proxy         the global proxy configuration
     * @param executor      the executor for asynchronous operations
     * @param meterRegistry the Micrometer MeterRegistry for metrics
     */
    public ProxifiedHttpFetcher(DomainConfig domainConfig, ProxyConfig proxy, Executor executor,
            MeterRegistry meterRegistry) {
        this.userAgent = domainConfig.getUserAgent();
        this.customHeaders = domainConfig.getCustomHeaders();
        this.timeout = Duration.ofMillis(domainConfig.getTimeout());
        this.meterRegistry = meterRegistry;

        // Build HttpClient with proxy settings if provided
        HttpClient.Builder builder = HttpClient.newBuilder()
                .executor(executor)
                .connectTimeout(this.timeout)
                .followRedirects(HttpClient.Redirect.NEVER);
        if (proxy != null && proxy.getHost() != null) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxy.getHost(), proxy.getPort())));
            if (proxy.getUsername() != null && !proxy.getUsername().isBlank()) {
                builder.authenticator(new java.net.Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication(proxy.getUsername(),
                                proxy.getPassword() == null ? new char[0] : proxy.getPassword().toCharArray());
                    }
                });
            }
            logger.info("Using proxy {}:{} for fetching", proxy.getHost(), proxy.getPort());
        }
        this.httpClient = builder.build();
    }

    /**
     * Asynchronously fetches the URL using an HttpClient configured with a proxy.
     *
     * @param url the URL to fetch
     * @return a CompletableFuture of FetchResponse
     */
    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url) {
        return fetchUrlAsync(url, null);
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url, Map<String, String> headers) {
        logger.info("Fetching URL {} using ProxifiedHttpFetcher", url);
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .timeout(timeout)
                .header("User-Agent", this.userAgent);

        if (customHeaders != null) {
            customHeaders.forEach(requestBuilder::header);
        }
        if (headers != null) {
            headers.forEach(requestBuilder::setHeader);
        }

        HttpRequest request = requestBuilder.build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(response -> {
                    int statusCode = response.statusCode();
                    String htmlContent = response.body();
                    // Increment metrics
                    meterRegistry.counter("url.fetch.total").increment();
                    meterRegistry.counter("url.fetch.status", "code", String.valueOf(statusCode)).increment();
                    logger.info("Fetched URL {} with status code {}", url, statusCode);
                    return FetchResponseFactory.fromHtml(url, statusCode, htmlContent, FetchStrategy.PROXIFIED);
                });
    }

	@Override
	public FetchResponse fetchUrlSync(String url) throws IOException {
		// TODO(p2, feature) : implement
		throw new RuntimeException("Not implemented");
	}
}
