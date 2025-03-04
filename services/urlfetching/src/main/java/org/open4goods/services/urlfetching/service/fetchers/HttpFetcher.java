package org.open4goods.services.urlfetching.service.fetchers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;

import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Implementation of Fetcher using java.net.http.HttpClient.
 */
public class HttpFetcher implements Fetcher {

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

    @Override
    public CompletableFuture<FetchResponse> fetchUrl(String url) {
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
                    String markdownContent = convertHtmlToMarkdown(htmlContent);
                    // Increment metrics
                    meterRegistry.counter("url.fetch.total").increment();
                    meterRegistry.counter("url.fetch.status", "code", String.valueOf(statusCode)).increment();
                    return new FetchResponse(statusCode, htmlContent, markdownContent);
                });
    }

    /**
     * Converts HTML content to markdown using FlexmarkHtmlConverter.
     *
     * @param html the HTML content
     * @return markdown representation of the HTML content
     */
    private String convertHtmlToMarkdown(String html) {
        String markdown = FlexmarkHtmlConverter.builder().build().convert(html);
        // Remove markdown links and trim content
        String trimmed = trimBeforeFirstHeading(markdown);
        return (!trimmed.isEmpty()) ? trimmed : replaceMarkdownLinks(markdown);
    }

    /**
     * Trims content before the first heading.
     *
     * @param input the markdown content
     * @return trimmed markdown content
     */
    private String trimBeforeFirstHeading(String input) {
        String[] lines = input.split("\n");
        StringBuilder sb = new StringBuilder();
        boolean foundHeading = false;
        for (String line : lines) {
            if (!foundHeading && line.startsWith("*") && line.length() > 1 && !line.substring(1, 2).equals(" ")) {
                foundHeading = true;
            }
            if (foundHeading) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    /**
     * Replaces markdown links with just their text.
     *
     * @param input the markdown content
     * @return markdown content without links
     */
    private String replaceMarkdownLinks(String input) {
        String regex = "\\[(.*?)\\]\\((https?://[^\\s)]+)(?:\\s+\"[^\"]*\")?\\)";
        return input.replaceAll(regex, "$1");
    }
}
