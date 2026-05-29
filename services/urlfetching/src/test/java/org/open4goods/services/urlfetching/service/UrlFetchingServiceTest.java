package org.open4goods.services.urlfetching.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.urlfetching.config.FetchStrategy;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig;
import org.open4goods.services.urlfetching.config.UrlFetcherConfig.DomainConfig;
import org.open4goods.services.urlfetching.dto.FetchResponse;

import com.sun.net.httpserver.HttpServer;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

/**
 * Unit tests for URL fetching orchestration.
 */
class UrlFetchingServiceTest {

    @Test
    void fetchUrlAsync_RejectsStructuredMetadataGtinMismatch() throws Exception {
        HttpServer server = startServer();
        try {
            UrlFetcherConfig config = new UrlFetcherConfig();
            DomainConfig domainConfig = new DomainConfig();
            domainConfig.setStrategy(FetchStrategy.HTTP);
            domainConfig.setUserAgent("Open4GoodsTestAgent/1.0");
            domainConfig.setTimeout(5000);
            config.setDomains(Map.of("localhost", domainConfig));

            UrlFetchingService service = new UrlFetchingService(config, new SimpleMeterRegistry());
            FetchResponse response = service.fetchUrlAsync(
                    "http://localhost:" + server.getAddress().getPort() + "/product",
                    Map.of(UrlFetchingService.EXPECTED_GTIN_HEADER, "9999999999999")).get();

            assertThat(response.rejected()).isTrue();
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(response.extractedGtins()).containsExactly("1234567890123");
            assertThat(response.rejectionReason()).contains("GTIN");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchUrlAsync_DoesNotFollowRedirects() throws Exception {
        HttpServer server = startServer();
        try {
            UrlFetcherConfig config = new UrlFetcherConfig();
            DomainConfig domainConfig = new DomainConfig();
            domainConfig.setStrategy(FetchStrategy.HTTP);
            domainConfig.setUserAgent("Open4GoodsTestAgent/1.0");
            domainConfig.setTimeout(5000);
            config.setDomains(Map.of("localhost", domainConfig));

            UrlFetchingService service = new UrlFetchingService(config, new SimpleMeterRegistry());
            FetchResponse response = service.fetchUrlAsync(
                    "http://localhost:" + server.getAddress().getPort() + "/redirect").get();

            assertThat(response.statusCode()).isEqualTo(302);
            assertThat(response.htmlContent()).doesNotContain("Test product");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchUrlAsync_AppliesRuntimeTimeoutOverrideWithoutMutatingConfiguredDomain() throws Exception {
        UrlFetcherConfig config = new UrlFetcherConfig();
        DomainConfig domainConfig = new DomainConfig();
        domainConfig.setStrategy(FetchStrategy.HTTP);
        domainConfig.setUserAgent("Open4GoodsTestAgent/1.0");
        domainConfig.setTimeout(5000);

        UrlFetchingService service = new UrlFetchingService(config, new SimpleMeterRegistry());
        Method method = UrlFetchingService.class.getDeclaredMethod("withRuntimeOverrides", DomainConfig.class, Map.class);
        method.setAccessible(true);

        DomainConfig overridden = (DomainConfig) method.invoke(service, domainConfig,
                Map.of(UrlFetchingService.FETCH_TIMEOUT_MS_HEADER, "15000"));

        assertThat(overridden.getStrategy()).isEqualTo(FetchStrategy.HTTP);
        assertThat(overridden.getTimeout()).isEqualTo(15000);
        assertThat(domainConfig.getTimeout()).isEqualTo(5000);
    }

    private HttpServer startServer() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/product", exchange -> {
            String body = """
                    <html>
                      <head>
                        <script type="application/ld+json">
                          {"@context":"https://schema.org","@type":"Product","name":"Test","gtin13":"1234567890123"}
                        </script>
                      </head>
                      <body><main><h1>Test product</h1></main></body>
                    </html>
                    """;
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "text/html; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream outputStream = exchange.getResponseBody()) {
                outputStream.write(bytes);
            }
        });
        server.createContext("/redirect", exchange -> {
            exchange.getResponseHeaders().add("Location", "/product");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
        server.start();
        return server;
    }
}
