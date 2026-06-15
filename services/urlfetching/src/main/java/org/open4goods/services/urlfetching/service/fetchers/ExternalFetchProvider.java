package org.open4goods.services.urlfetching.service.fetchers;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.ExternalProviderConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.FetchResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.micrometer.core.instrument.MeterRegistry;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Provider-neutral adapter for external anti-bot URL fetching APIs.
 */
public class ExternalFetchProvider implements Fetcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExternalFetchProvider.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Set<String> INTERNAL_HEADER_NAMES = Set.of(
            "x-open4goods-fetch-mode",
            "x-open4goods-fetch-provider",
            "x-open4goods-playwright-proxy",
            "x-open4goods-fetch-timeout-ms",
            "x-open4goods-expected-gtin",
            "x-open4goods-force-fetch",
            "x-forwarded-for",
            "x-real-ip",
            "forwarded",
            "host",
            "content-length");

    private final String providerName;
    private final ExternalProviderConfig config;
    private final Executor executor;
    private final MeterRegistry meterRegistry;
    private final HttpClient httpClient;

    public ExternalFetchProvider(String providerName, ExternalProviderConfig config, Executor executor,
            MeterRegistry meterRegistry) {
        this.providerName = providerName;
        this.config = config;
        this.executor = executor;
        this.meterRegistry = meterRegistry;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getTimeout()))
                .executor(executor)
                .build();
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url) {
        return fetchUrlAsync(url, null);
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetchUrlSync(url, headers);
            } catch (Exception e) {
                LOGGER.warn("URL_FETCH strategy=EXTERNAL provider={} url={} outcome=failed error={}",
                        providerName, url, e.getMessage(), e);
                meterRegistry.counter("url.fetch.total", "strategy", FetchStrategy.EXTERNAL.name(),
                        "provider", providerName, "outcome", "failed").increment();
                return new FetchResponse(url, 500, "", "", FetchStrategy.EXTERNAL);
            }
        }, executor);
    }

    @Override
    public FetchResponse fetchUrlSync(String url) throws IOException, InterruptedException {
        return fetchUrlSync(url, null);
    }

    public FetchResponse fetchUrlSync(String url, Map<String, String> runtimeHeaders)
            throws IOException, InterruptedException {
        if (!config.isEnabled()) {
            LOGGER.warn("URL_FETCH strategy=EXTERNAL provider={} url={} outcome=disabled", providerName, url);
            return new FetchResponse(url, 503, "", "", FetchStrategy.EXTERNAL);
        }
        long start = System.nanoTime();
        HttpRequest request = request(url, runtimeHeaders);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String html = extractHtml(response.body());
        long durationMs = Duration.ofNanos(System.nanoTime() - start).toMillis();
        meterRegistry.counter("url.fetch.total", "strategy", FetchStrategy.EXTERNAL.name(),
                "provider", providerName, "outcome", "complete").increment();
        meterRegistry.counter("url.fetch.status", "strategy", FetchStrategy.EXTERNAL.name(),
                "provider", providerName, "status", String.valueOf(response.statusCode())).increment();
        meterRegistry.timer("url.fetch.duration", "strategy", FetchStrategy.EXTERNAL.name(),
                "provider", providerName, "status", String.valueOf(response.statusCode()))
                .record(Duration.ofMillis(durationMs));
        LOGGER.info("URL_FETCH strategy=EXTERNAL provider={} url={} statusCode={} durationMs={} htmlChars={}",
                providerName, url, response.statusCode(), durationMs, html == null ? 0 : html.length());
        return FetchResponseFactory.fromHtml(url, response.statusCode(), html == null ? "" : html,
                FetchStrategy.EXTERNAL);
    }

    private HttpRequest request(String url, Map<String, String> runtimeHeaders) throws IOException {
        String method = config.getMethod() == null ? "GET" : config.getMethod().trim().toUpperCase();
        URI uri = "GET".equals(method) ? getUri(url) : URI.create(config.getEndpoint());
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMillis(config.getTimeout()));
        config.getStaticHeaders().forEach(builder::setHeader);
        if (runtimeHeaders != null) {
            runtimeHeaders.entrySet().stream()
                    .filter(entry -> !isInternalHeader(entry.getKey()))
                    .forEach(entry -> builder.setHeader(entry.getKey(), entry.getValue()));
        }
        if (config.getApiKeyHeader() != null && config.getApiKey() != null) {
            builder.setHeader(config.getApiKeyHeader(), config.getApiKey());
        }
        if ("POST".equals(method)) {
            builder.setHeader("Content-Type", "application/json");
            String field = config.getUrlBodyField() == null ? "url" : config.getUrlBodyField();
            String body = OBJECT_MAPPER.writeValueAsString(Map.of(field, url));
            builder.POST(HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.GET();
        }
        return builder.build();
    }

    private boolean isInternalHeader(String headerName) {
        return headerName != null && INTERNAL_HEADER_NAMES.contains(headerName.toLowerCase(Locale.ROOT));
    }

    private URI getUri(String url) {
        String endpoint = config.getEndpoint();
        String separator = endpoint.contains("?") ? "&" : "?";
        String query = config.getUrlQueryParameter() + "="
                + URLEncoder.encode(url, StandardCharsets.UTF_8);
        if (config.getApiKeyQueryParameter() != null && config.getApiKey() != null) {
            query += "&" + config.getApiKeyQueryParameter() + "="
                    + URLEncoder.encode(config.getApiKey(), StandardCharsets.UTF_8);
        }
        return URI.create(endpoint + separator + query);
    }

    private String extractHtml(String responseBody) throws IOException {
        if (config.getHtmlJsonPointer() == null || config.getHtmlJsonPointer().isBlank()) {
            return responseBody;
        }
        JsonNode root = OBJECT_MAPPER.readTree(responseBody);
        JsonNode htmlNode = root.at(config.getHtmlJsonPointer());
        return htmlNode == null || htmlNode.isMissingNode() ? "" : htmlNode.asText();
    }
}
