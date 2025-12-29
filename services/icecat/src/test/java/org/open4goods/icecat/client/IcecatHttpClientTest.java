package org.open4goods.icecat.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.client.exception.IcecatAuthenticationException;
import org.open4goods.icecat.client.exception.IcecatRateLimitException;
import org.open4goods.icecat.client.exception.IcecatResourceNotFoundException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * WireMock-based integration tests for IcecatHttpClient.
 */
class IcecatHttpClientTest {

    private WireMockServer wireMockServer;
    private IcecatHttpClient client;
    private IcecatConfiguration config;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        config = new IcecatConfiguration();
        config.setUser("testuser");
        config.setPassword("testpass");
        config.setConnectTimeoutMs(5000);
        config.setReadTimeoutMs(10000);

        client = new IcecatHttpClient(config, tempDir.toString(), 2);
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldDownloadAndDecompressGzipFile() throws IOException {
        String content = "<xml>Test content</xml>";
        byte[] gzippedContent = gzip(content);

        wireMockServer.stubFor(post(urlEqualTo("/features.xml.gz"))
                .withBasicAuth("testuser", "testpass")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(gzippedContent)));

        String url = wireMockServer.baseUrl() + "/features.xml.gz";
        File result = client.downloadAndDecompressGzip(url, "features-test");

        assertThat(result).exists();
        assertThat(Files.readString(result.toPath())).isEqualTo(content);
    }

    @Test
    void shouldReturnCachedFileOnSecondRequest() throws IOException {
        String content = "<xml>Cached content</xml>";
        byte[] gzippedContent = gzip(content);

        wireMockServer.stubFor(post(urlEqualTo("/cached.xml.gz"))
                .withBasicAuth("testuser", "testpass")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(gzippedContent)));

        String url = wireMockServer.baseUrl() + "/cached.xml.gz";

        File firstResult = client.downloadAndDecompressGzip(url, "cached-test");
        File secondResult = client.downloadAndDecompressGzip(url, "cached-test");

        assertThat(firstResult.getAbsolutePath()).isEqualTo(secondResult.getAbsolutePath());
        wireMockServer.verify(1, postRequestedFor(urlEqualTo("/cached.xml.gz")));
    }

    @Test
    void shouldPerformGetRequest() {
        String responseBody = "{\"status\": \"ok\"}";

        wireMockServer.stubFor(get(urlEqualTo("/api/test"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/api/test";
        String result = client.get(url);

        assertThat(result).isEqualTo(responseBody);
    }

    @Test
    void shouldPerformGetRequestWithHeaders() {
        String responseBody = "{\"data\": \"test\"}";

        wireMockServer.stubFor(get(urlEqualTo("/api/protected"))
                .withHeader("Authorization", equalTo("Bearer token123"))
                .withHeader("organizationId", equalTo("org-456"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/api/protected";
        Map<String, String> headers = Map.of(
                "Authorization", "Bearer token123",
                "organizationId", "org-456"
        );

        String result = client.get(url, headers);

        assertThat(result).isEqualTo(responseBody);
    }

    @Test
    void shouldRetryOnTransientFailure() throws IOException {
        String content = "<xml>Retry success</xml>";
        byte[] gzippedContent = gzip(content);

        wireMockServer.stubFor(post(urlEqualTo("/retry.xml.gz"))
                .inScenario("Retry")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse().withStatus(500))
                .willSetStateTo("First Failure"));

        wireMockServer.stubFor(post(urlEqualTo("/retry.xml.gz"))
                .inScenario("Retry")
                .whenScenarioStateIs("First Failure")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(gzippedContent)));

        String url = wireMockServer.baseUrl() + "/retry.xml.gz";
        File result = client.downloadAndDecompressGzip(url, "retry-test");

        assertThat(result).exists();
        assertThat(Files.readString(result.toPath())).isEqualTo(content);
        wireMockServer.verify(2, postRequestedFor(urlEqualTo("/retry.xml.gz")));
    }

    @Test
    void shouldThrowAfterMaxRetries() {
        wireMockServer.stubFor(post(urlEqualTo("/always-fail.xml.gz"))
                .willReturn(aResponse().withStatus(500)));

        String url = wireMockServer.baseUrl() + "/always-fail.xml.gz";

        assertThatThrownBy(() -> client.downloadAndDecompressGzip(url, "fail-test"))
                .isInstanceOf(IcecatApiException.class)
                .hasMessageContaining("failed after");

        wireMockServer.verify(3, postRequestedFor(urlEqualTo("/always-fail.xml.gz")));
    }

    @Test
    void shouldHandleRateLimiting() {
        wireMockServer.stubFor(get(urlEqualTo("/rate-limited"))
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "2")));

        String url = wireMockServer.baseUrl() + "/rate-limited";

        assertThatThrownBy(() -> client.get(url))
                .isInstanceOf(IcecatRateLimitException.class)
                .satisfies(e -> {
                    IcecatRateLimitException rle = (IcecatRateLimitException) e;
                    assertThat(rle.getRetryAfter()).isEqualTo(Duration.ofSeconds(2));
                    assertThat(rle.getStatusCode()).isEqualTo(429);
                });
    }

    @Test
    void shouldThrowAuthenticationExceptionOn401() {
        wireMockServer.stubFor(get(urlEqualTo("/unauthorized"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withBody("Invalid credentials")));

        String url = wireMockServer.baseUrl() + "/unauthorized";

        assertThatThrownBy(() -> client.get(url))
                .isInstanceOf(IcecatAuthenticationException.class)
                .satisfies(e -> {
                    IcecatAuthenticationException ae = (IcecatAuthenticationException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(401);
                });
    }

    @Test
    void shouldThrowAuthenticationExceptionOn403() {
        wireMockServer.stubFor(get(urlEqualTo("/forbidden"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Access denied")));

        String url = wireMockServer.baseUrl() + "/forbidden";

        assertThatThrownBy(() -> client.get(url))
                .isInstanceOf(IcecatAuthenticationException.class)
                .satisfies(e -> {
                    IcecatAuthenticationException ae = (IcecatAuthenticationException) e;
                    assertThat(ae.getStatusCode()).isEqualTo(403);
                });
    }

    @Test
    void shouldThrowResourceNotFoundExceptionOn404() {
        wireMockServer.stubFor(get(urlEqualTo("/not-found"))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withBody("Product not found")));

        String url = wireMockServer.baseUrl() + "/not-found";

        assertThatThrownBy(() -> client.get(url))
                .isInstanceOf(IcecatResourceNotFoundException.class);
    }

    @Test
    void shouldClearCache() throws IOException {
        String content = "<xml>To be cleared</xml>";
        byte[] gzippedContent = gzip(content);

        wireMockServer.stubFor(post(urlEqualTo("/clear-cache.xml.gz"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(gzippedContent)));

        String url = wireMockServer.baseUrl() + "/clear-cache.xml.gz";

        File result = client.downloadAndDecompressGzip(url, null);
        assertThat(result).exists();

        client.clearCache(url);
        assertThat(result).doesNotExist();
    }

    @Test
    void shouldPostFormData() {
        String responseBody = "{\"access_token\": \"abc123\", \"expires_in\": 3600}";

        wireMockServer.stubFor(post(urlEqualTo("/oauth/token"))
                .withHeader("Content-Type", containing("application/x-www-form-urlencoded"))
                .withRequestBody(containing("grant_type=password"))
                .withRequestBody(containing("client_id=test-client"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(responseBody)));

        String url = wireMockServer.baseUrl() + "/oauth/token";
        Map<String, String> formData = Map.of(
                "grant_type", "password",
                "client_id", "test-client",
                "client_secret", "secret",
                "username", "user",
                "password", "pass"
        );

        String result = client.postForm(url, formData, String.class);

        assertThat(result).contains("access_token");
    }

    /**
     * Helper method to gzip content.
     */
    private byte[] gzip(String content) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (GZIPOutputStream gos = new GZIPOutputStream(baos)) {
            gos.write(content.getBytes(StandardCharsets.UTF_8));
        }
        return baos.toByteArray();
    }
}
