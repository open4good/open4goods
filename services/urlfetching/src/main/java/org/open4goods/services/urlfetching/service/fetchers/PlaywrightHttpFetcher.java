package org.open4goods.services.urlfetching.service.fetchers;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
            "--no-sandbox",
            "--disable-blink-features=AutomationControlled"
    );

    /**
     * JS payload applied to every new document before page scripts run, masking the
     * most common bot-detection signals (navigator.webdriver, plugin list, languages,
     * window.chrome, permissions API, WebGL vendor/renderer).
     */
    private static final String STEALTH_INIT_SCRIPT = """
            Object.defineProperty(navigator, 'webdriver', { get: () => undefined });
            Object.defineProperty(navigator, 'languages', { get: () => ['fr-FR', 'fr', 'en-US', 'en'] });
            Object.defineProperty(navigator, 'plugins', { get: () => [
              { name: 'Chrome PDF Plugin', filename: 'internal-pdf-viewer', description: 'Portable Document Format' },
              { name: 'Chrome PDF Viewer', filename: 'mhjfbmdgcfjbbpaeojofohoefgiehjai', description: '' },
              { name: 'Native Client', filename: 'internal-nacl-plugin', description: '' }
            ]});
            window.chrome = window.chrome || { runtime: {}, app: {}, csi: () => {}, loadTimes: () => {} };
            const __origQuery = window.navigator.permissions && window.navigator.permissions.query;
            if (__origQuery) {
              window.navigator.permissions.query = (p) => (
                p && p.name === 'notifications'
                  ? Promise.resolve({ state: Notification.permission })
                  : __origQuery(p)
              );
            }
            const __getParam = WebGLRenderingContext.prototype.getParameter;
            WebGLRenderingContext.prototype.getParameter = function(parameter) {
              if (parameter === 37445) return 'Intel Inc.';
              if (parameter === 37446) return 'Intel Iris OpenGL Engine';
              return __getParam.apply(this, arguments);
            };
            """;

    /** Channels that failed to launch in this JVM — skipped on subsequent fetches. */
    private static final ConcurrentMap<String, Boolean> UNAVAILABLE_CHANNELS = new ConcurrentHashMap<>();

    private final String userAgent;
    private final Map<String, String> customHeaders;
    private final Duration timeout;
    private final String browserChannel;
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
        this.browserChannel = domainConfig.getBrowserChannel();
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
                Browser browser = launchBrowser(playwright);
                BrowserContext context = browser.newContext(contextOptions(headers))) {

            context.addInitScript(STEALTH_INIT_SCRIPT);
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

    /**
     * Launches Chromium, preferring the configured branded channel for a more realistic
     * fingerprint. If the channel is unavailable on the host (e.g. branded Chrome is not
     * installed), falls back to bundled Chromium and remembers the failure for the rest
     * of the JVM's lifetime to avoid repeated launch attempts.
     */
    private Browser launchBrowser(Playwright playwright) {
        String channel = browserChannel;
        boolean useChannel = channel != null && !channel.isBlank()
                && !UNAVAILABLE_CHANNELS.containsKey(channel);
        if (useChannel) {
            try {
                return playwright.chromium().launch(baseLaunchOptions().setChannel(channel));
            } catch (RuntimeException e) {
                if (UNAVAILABLE_CHANNELS.putIfAbsent(channel, Boolean.TRUE) == null) {
                    logger.warn("URL_FETCH strategy=PLAYWRIGHT channel={} unavailable, falling back to bundled Chromium: {}",
                            channel, e.getMessage());
                }
            }
        }
        return playwright.chromium().launch(baseLaunchOptions());
    }

    private BrowserType.LaunchOptions baseLaunchOptions() {
        return new BrowserType.LaunchOptions()
                .setHeadless(true)
                .setTimeout((double) timeout.toMillis())
                .setArgs(CHROMIUM_ARGS);
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
