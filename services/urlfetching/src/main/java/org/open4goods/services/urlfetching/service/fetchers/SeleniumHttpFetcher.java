package org.open4goods.services.urlfetching.service.fetchers;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.micrometer.core.instrument.MeterRegistry;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.HtmlToMarkdownConverter;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of Fetcher using Selenium WebDriver.
 */
public class SeleniumHttpFetcher implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(SeleniumHttpFetcher.class);

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
        logger.info("Fetching URL {} using SeleniumHttpFetcher", url);
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
            // Removed TODO for custom headers support

            // Create a new instance of ChromeDriver (consider pooling in production)
            WebDriver driver = new ChromeDriver(options);
            try {
                driver.manage().timeouts().pageLoadTimeout(timeout);
                driver.get(url);
                String htmlContent = driver.getPageSource();
                String markdownContent = HtmlToMarkdownConverter.convert(htmlContent);
                // Increment metrics (assume HTTP 200 for successful Selenium fetch)
                meterRegistry.counter("url.fetch.total").increment();
                meterRegistry.counter("url.fetch.status", "code", "200").increment();
                logger.info("Successfully fetched URL {} with status code 200", url);
                return new FetchResponse(url, 200, htmlContent, markdownContent,FetchStrategy.SELENIUM);
            } finally {
                driver.quit();
                logger.debug("Closed Selenium WebDriver for URL {}", url);
            }
        });
    }
}
