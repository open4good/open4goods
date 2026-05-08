package org.open4goods.services.urlfetching.service.fetchers;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;
import org.open4goods.services.urlfetching.service.Fetcher;
import org.open4goods.services.urlfetching.util.HtmlToMarkdownConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserContext;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.Playwright;
import com.microsoft.playwright.Response;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.WaitUntilState;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * URL fetcher backed by Playwright Chromium in headless mode.
 * <p>
 * A fresh browser context is created per fetch so user-agent and header
 * overrides remain isolated across domains and review-generation attempts.
 * </p>
 */
public class PlaywrightHttpFetcher implements Fetcher {

    private static final Logger logger = LoggerFactory.getLogger(PlaywrightHttpFetcher.class);
    private static final List<String> CHROMIUM_ARGS = List.of(
            "--disable-dev-shm-usage",
            "--disable-gpu",
            "--no-sandbox"
    );

    private final String userAgent;
    private final Map<String, String> customHeaders;
    private final Duration timeout;
    private final MeterRegistry meterRegistry;

    /**
     * Constructs a new Playwright fetcher.
     *
     * @param domainConfig  domain-specific fetch configuration
     * @param meterRegistry Micrometer registry for fetch metrics
     */
    public PlaywrightHttpFetcher(DomainConfig domainConfig, MeterRegistry meterRegistry) {
        this.userAgent = domainConfig.getUserAgent();
        this.customHeaders = domainConfig.getCustomHeaders();
        this.timeout = Duration.ofMillis(domainConfig.getTimeout());
        this.meterRegistry = meterRegistry;
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url) {
        return fetchUrlAsync(url, null);
    }

    @Override
    public CompletableFuture<FetchResponse> fetchUrlAsync(String url, Map<String, String> headers) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return fetch(url, headers);
            } catch (Exception e) {
                logger.warn("URL_FETCH strategy=PLAYWRIGHT url={} outcome=failed error={}", url, e.getMessage(), e);
                throw new IllegalStateException("Playwright fetch failed for " + url, e);
            }
        });
    }

    @Override
    public FetchResponse fetchUrlSync(String url) throws IOException {
        try {
            return fetch(url, null);
        } catch (Exception e) {
            throw new IOException("Playwright fetch failed for " + url, e);
        }
    }

    private FetchResponse fetch(String url, Map<String, String> runtimeHeaders) {
        long start = System.currentTimeMillis();
        Map<String, String> headers = mergedHeaders(runtimeHeaders);
        logger.info("URL_FETCH strategy=PLAYWRIGHT url={} phase=start timeoutMs={} headless=true customHeaderNames={}",
                url, timeout.toMillis(), headers.keySet());

        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                        .setHeadless(true)
                        .setTimeout((double) timeout.toMillis())
                        .setArgs(CHROMIUM_ARGS));
                BrowserContext context = browser.newContext(contextOptions(headers))) {

            Page page = context.newPage();
            page.setDefaultNavigationTimeout(timeout.toMillis());
            page.setDefaultTimeout(timeout.toMillis());
            Response response = page.navigate(url, new Page.NavigateOptions()
                    .setTimeout((double) timeout.toMillis())
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED);

            int statusCode = response == null ? 200 : response.status();
            String htmlContent = page.content();
            String markdownContent = HtmlToMarkdownConverter.convert(htmlContent);
            long duration = System.currentTimeMillis() - start;

            meterRegistry.counter("url.fetch.total", "strategy", FetchStrategy.PLAYWRIGHT.name()).increment();
            meterRegistry.counter("url.fetch.status", "strategy", FetchStrategy.PLAYWRIGHT.name(),
                    "code", String.valueOf(statusCode)).increment();
            logger.info(
                    "URL_FETCH strategy=PLAYWRIGHT url={} phase=complete statusCode={} durationMs={} htmlChars={} markdownChars={}",
                    url, statusCode, duration, htmlContent == null ? 0 : htmlContent.length(),
                    markdownContent == null ? 0 : markdownContent.length());
            return new FetchResponse(url, statusCode, htmlContent, markdownContent, FetchStrategy.PLAYWRIGHT);
        }
    }

    private Browser.NewContextOptions contextOptions(Map<String, String> headers) {
        Browser.NewContextOptions options = new Browser.NewContextOptions()
                .setLocale("fr-FR")
                .setViewportSize(1365, 900);
        if (userAgent != null && !userAgent.isBlank()) {
            options.setUserAgent(userAgent);
        }
        if (!headers.isEmpty()) {
            options.setExtraHTTPHeaders(headers);
        }
        return options;
    }

    private Map<String, String> mergedHeaders(Map<String, String> runtimeHeaders) {
        Map<String, String> headers = new HashMap<>();
        if (customHeaders != null) {
            headers.putAll(customHeaders);
        }
        if (runtimeHeaders != null) {
            headers.putAll(runtimeHeaders);
        }
        return headers;
    }
}
