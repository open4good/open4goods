package org.open4goods.services.urlfetching.service.fetchers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.MeterRegistry;

import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import com.vladsch.flexmark.html2md.converter.FlexmarkHtmlConverter;

/**
 * Implementation of Fetcher using Selenium WebDriver.
 */
public class SeleniumHttpFetcher implements Fetcher {

    private final String userAgent;
    private final Map<String, String> customHeaders;
    private final Duration timeout;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs a new SeleniumHttpFetcher.
     *
     * @param domainConfig  the domain-specific configuration
     * @param meterRegistry the Micrometer MeterRegistry for metrics
     */
    public SeleniumHttpFetcher(DomainConfig domainConfig, MeterRegistry meterRegistry) {
        this.userAgent = domainConfig.getUserAgent();
        this.customHeaders = domainConfig.getCustomHeaders();
        this.timeout = Duration.ofMillis(domainConfig.getTimeout());
        this.meterRegistry = meterRegistry;
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrl(String url) {
        return CompletableFuture.supplyAsync(() -> {
            // Setup Chrome options for headless mode
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless");
            options.addArguments("--disable-gpu");
            options.addArguments("--no-sandbox");
            // Set user agent if provided
            if (userAgent != null && !userAgent.isEmpty()) {
                options.addArguments("--user-agent=" + userAgent);
            }
            // TODO: Add custom headers support if required via Selenium
           
            // Create a new instance of ChromeDriver (consider pooling in production)
            WebDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().pageLoadTimeout(timeout);
                driver.get(url);
                String htmlContent = driver.getPageSource();
                String markdownContent = convertHtmlToMarkdown(htmlContent);
                // Increment metrics (assume HTTP 200 for successful Selenium fetch)
                meterRegistry.counter("url.fetch.total").increment();
                meterRegistry.counter("url.fetch.status", "code", "200").increment();
                return new FetchResponse(200, htmlContent, markdownContent);
            } finally {
                driver.quit();
            }
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
