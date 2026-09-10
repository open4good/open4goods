package org.open4goods.icecat.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

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

    private static IcecatLiveClient clientFor(HttpServer server) {
        IcecatCompletionConfig config = new IcecatCompletionConfig();
        config.setIceCatUrlPrefix(
                "http://localhost:" + server.getAddress().getPort() + "/api?UserName=test&Language=fr&GTIN=");
        return new IcecatLiveClient(config, RestClient.create(), new ObjectMapper());
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
}
