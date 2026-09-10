package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.open4goods.icecat.config.yml.IcecatCompletionConfig;
import org.open4goods.model.localization.DomainLanguage;
import org.springframework.web.client.RestClient;

import com.sun.net.httpserver.HttpServer;

import tools.jackson.databind.ObjectMapper;

/**
 * Verifies that {@link IcecatLiveClient} owns HTTP transport, tolerant JSON parsing and maps
 * each outcome (found, not found, restricted, malformed) into the neutral
 * {@link IcecatLiveLookupResult} without leaking HTTP/JSON exception types to callers.
 */
class IcecatLiveClientTest {

    private static final String FOUND_RESPONSE = """
            {
              "msg": "OK",
              "data": {
                "GeneralInfo": {
                  "IcecatId": 12345,
                  "Title": "Test Product",
                  "Brand": "TestBrand",
                  "ProductName": "TP-100"
                }
              }
            }
            """;

    private static final String EMPTY_DATA_RESPONSE = """
            { "msg": "OK" }
            """;

    @Test
    void fetchProductReturnsFoundWithParsedData() throws Exception {
        HttpServer server = startServer(200, FOUND_RESPONSE);
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProduct(12345L, DomainLanguage.en);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.FOUND);
            assertThat(result.product()).isPresent();
            assertThat(result.product().get().generalInfo.icecatId).isEqualTo(12345);
            assertThat(result.product().get().generalInfo.title).isEqualTo("Test Product");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductReturnsNotFoundWhenDataMissing() throws Exception {
        HttpServer server = startServer(200, EMPTY_DATA_RESPONSE);
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.NOT_FOUND);
            assertThat(result.product()).isEmpty();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductReturnsNotFoundOn404() throws Exception {
        HttpServer server = startServer(404, "not found");
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.NOT_FOUND);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductReturnsRestrictedOn403() throws Exception {
        HttpServer server = startServer(403, "forbidden");
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.RESTRICTED);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductReturnsErrorOnMalformedJson() throws Exception {
        HttpServer server = startServer(200, "not json at all");
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.ERROR);
            assertThat(result.errorMessage()).isPresent();
        } finally {
            server.stop(0);
        }
    }

    @Test
    void buildUrlSubstitutesRequestedLanguage() {
        IcecatCompletionConfig config = new IcecatCompletionConfig();
        config.setIceCatUrlPrefix("https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=");
        IcecatLiveClient client = new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());

        String url = client.buildUrl(42L, DomainLanguage.en);

        assertThat(url).isEqualTo("https://live.icecat.biz/api?UserName=openIcecat-live&Language=en&GTIN=42");
    }

    @Test
    void buildIdUrlSubstitutesLanguageAndIcecatIdParam() {
        IcecatCompletionConfig config = new IcecatCompletionConfig();
        config.setIceCatUrlPrefix("https://live.icecat.biz/api?UserName=openIcecat-live&Language=fr&GTIN=");
        IcecatLiveClient client = new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());

        String url = client.buildIdUrl("12345", DomainLanguage.en);

        assertThat(url).isEqualTo("https://live.icecat.biz/api?UserName=openIcecat-live&Language=en&icecat_id=12345");
    }

    @Test
    void fetchProductByIcecatIdReturnsFoundWithParsedData() throws Exception {
        HttpServer server = startServer(200, FOUND_RESPONSE);
        try {
            IcecatLiveClient client = clientFor(server);

            IcecatLiveLookupResult result = client.fetchProductByIcecatId("12345", DomainLanguage.en);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.FOUND);
            assertThat(result.product().get().generalInfo.icecatId).isEqualTo(12345);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductRetriesOnServerErrorThenSucceeds() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = startCountingServer(hits, 2, 500, "server error", 200, FOUND_RESPONSE);
        try {
            IcecatCompletionConfig config = configFor(server);
            config.setMaxRetryAttempts(3);
            config.setRetryBackoffMs(5);
            IcecatLiveClient client = new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.FOUND);
            assertThat(hits.get()).isEqualTo(3);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductReturnsErrorAfterExhaustingRetriesOnServerError() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = startCountingServer(hits, Integer.MAX_VALUE, 500, "server error", 200, FOUND_RESPONSE);
        try {
            IcecatCompletionConfig config = configFor(server);
            config.setMaxRetryAttempts(2);
            config.setRetryBackoffMs(5);
            IcecatLiveClient client = new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.ERROR);
            assertThat(hits.get()).isEqualTo(2);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void fetchProductDoesNotRetryOn404() throws Exception {
        AtomicInteger hits = new AtomicInteger();
        HttpServer server = startCountingServer(hits, Integer.MAX_VALUE, 404, "not found", 200, FOUND_RESPONSE);
        try {
            IcecatCompletionConfig config = configFor(server);
            config.setMaxRetryAttempts(3);
            config.setRetryBackoffMs(5);
            IcecatLiveClient client = new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());

            IcecatLiveLookupResult result = client.fetchProduct(1L, DomainLanguage.fr);

            assertThat(result.status()).isEqualTo(IcecatLiveLookupResult.Status.NOT_FOUND);
            assertThat(hits.get()).isEqualTo(1);
        } finally {
            server.stop(0);
        }
    }

    private static IcecatCompletionConfig configFor(HttpServer server) {
        IcecatCompletionConfig config = new IcecatCompletionConfig();
        config.setIceCatUrlPrefix(
                "http://localhost:" + server.getAddress().getPort() + "/api?UserName=test&Language=fr&GTIN=");
        return config;
    }

    private static IcecatLiveClient clientFor(HttpServer server) {
        return new IcecatLiveClient(configFor(server), RestClient.create(), new ObjectMapper());
    }

    private static HttpServer startServer(int status, String body) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api", exchange -> {
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return server;
    }

    /**
     * Responds with {@code (failStatus, failBody)} for the first {@code failCount} requests, then
     * {@code (okStatus, okBody)} for every request after.
     */
    private static HttpServer startCountingServer(AtomicInteger hits, int failCount, int failStatus, String failBody,
            int okStatus, String okBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/api", exchange -> {
            int hit = hits.incrementAndGet();
            boolean fail = hit <= failCount;
            byte[] bytes = (fail ? failBody : okBody).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(fail ? failStatus : okStatus, bytes.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(bytes);
            }
        });
        server.start();
        return server;
    }
}
