package org.open4goods.services.urlfetching.service.fetchers;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;

import com.sun.net.httpserver.HttpServer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for provider-neutral external URL fetching.
 */
class ExternalFetchProviderTest {

    @Test
    void fetchUrlAsync_ExtractsHtmlFromJsonPointerAndStripsInternalHeaders() throws Exception {
        Map<String, String> observedHeaders = new HashMap<>();
        HttpServer server = startServer(observedHeaders);
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            UrlFetcherConfig.ExternalProviderConfig config = new UrlFetcherConfig.ExternalProviderConfig();
            config.setEnabled(true);
            config.setEndpoint("http://localhost:" + server.getAddress().getPort() + "/external");
            config.setMethod("POST");
            config.setUrlBodyField("target");
            config.setHtmlJsonPointer("/result/html");
            config.setApiKey("secret-token");
            config.setApiKeyHeader("X-Provider-Key");
            config.setStaticHeaders(Map.of("X-Static", "static-value"));

            ExternalFetchProvider fetcher = new ExternalFetchProvider("test-provider", config, executor,
                    new SimpleMeterRegistry());
            FetchResponse response = fetcher.fetchUrlAsync("https://merchant.example/product",
                    Map.of("X-Open4goods-Fetch-Mode", "external", "X-Forwarded-For", "127.0.0.1",
                            "X-Runtime", "runtime-value"))
                    .get();

            assertThat(response.fetchStrategy()).isEqualTo(FetchStrategy.EXTERNAL);
            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.htmlContent()).contains("External Review");
            assertThat(response.markdownContent()).contains("External Review");
            assertThat(observedHeaders).containsEntry("x-provider-key", "secret-token")
                    .containsEntry("x-static", "static-value")
                    .containsEntry("x-runtime", "runtime-value");
            assertThat(observedHeaders).doesNotContainKeys("x-open4goods-fetch-mode", "x-forwarded-for");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchUrlSync_ReturnsUnavailableWhenProviderDisabled() throws IOException, InterruptedException {
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            UrlFetcherConfig.ExternalProviderConfig config = new UrlFetcherConfig.ExternalProviderConfig();
            config.setEnabled(false);

            ExternalFetchProvider fetcher = new ExternalFetchProvider("disabled-provider", config, executor,
                    new SimpleMeterRegistry());
            FetchResponse response = fetcher.fetchUrlSync("https://merchant.example/product");

            assertThat(response.fetchStrategy()).isEqualTo(FetchStrategy.EXTERNAL);
            assertThat(response.statusCode()).isEqualTo(503);
        }
    }

    private HttpServer startServer(Map<String, String> observedHeaders) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/external", exchange -> {
            exchange.getRequestHeaders().forEach((name, values) ->
                    observedHeaders.put(name.toLowerCase(Locale.ROOT), values.getFirst()));
            String body = """
                    {"result":{"html":"<html><body><main><h1>External Review</h1></main></body></html>"}}
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.start();
        return server;
    }
}
