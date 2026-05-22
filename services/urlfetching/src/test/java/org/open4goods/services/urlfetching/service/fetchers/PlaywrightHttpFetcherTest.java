package org.open4goods.services.urlfetching.service.fetchers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;

import com.microsoft.playwright.Browser;
import com.microsoft.playwright.BrowserType;
import com.microsoft.playwright.Playwright;
import com.sun.net.httpserver.HttpServer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Headless Playwright smoke tests for the browser-backed URL fetcher.
 */
class PlaywrightHttpFetcherTest {

    @Test
    void fetchUrlSync_RendersLocalPageWithHeadlessChromium() throws Exception {
        assumeChromiumInstalled();
        HttpServer server = startServer();
        try {
            DomainConfig domainConfig = new DomainConfig();
            domainConfig.setStrategy(FetchStrategy.PLAYWRIGHT);
            domainConfig.setUserAgent("Open4GoodsTestAgent/1.0");
            domainConfig.setTimeout(10000);

            PlaywrightHttpFetcher fetcher = new PlaywrightHttpFetcher(domainConfig, null, false, false,
                    new SimpleMeterRegistry());
            FetchResponse response = fetcher.fetchUrlSync("http://localhost:" + server.getAddress().getPort() + "/review");

            assertThat(response.fetchStrategy()).isEqualTo(FetchStrategy.PLAYWRIGHT);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.htmlContent()).contains("Rendered Review");
            assertThat(response.markdownContent()).contains("Rendered Review");
            assertThat(response.markdownContent()).contains("Measured contrast");
        } finally {
            server.stop(0);
        }
    }

    private void assumeChromiumInstalled() {
        try (Playwright playwright = Playwright.create();
                Browser browser = playwright.chromium().launch(new BrowserType.LaunchOptions().setHeadless(true))) {
            browser.close();
        } catch (RuntimeException e) {
            assumeTrue(false, "Playwright Chromium is not installed locally: " + e.getMessage());
        }
    }

    private HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/review", exchange -> {
            String body = """
                    <html>
                      <body>
                        <main>
                          <h1>Rendered Review</h1>
                          <p>Measured contrast and latency are available after browser rendering.</p>
                        </main>
                      </body>
                    </html>
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();
        return server;
    }
}
