package org.open4goods.icecat.client;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.*;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.client.exception.IcecatRateLimitException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.retailer.RetailerCategory;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;

/**
 * WireMock-based integration tests for IcecatRetailerApiClient.
 */
class IcecatRetailerApiClientTest {

    private WireMockServer wireMockServer;
    private IcecatConfiguration config;
    private IcecatHttpClient httpClient;
    private IcecatOAuthTokenManager tokenManager;
    private IcecatRateLimiter rateLimiter;
    private IcecatRetailerApiClient client;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();

        config = new IcecatConfiguration();
        config.setRetailerApiBaseUrl(wireMockServer.baseUrl());
        config.setTokenEndpoint("/connect/token");
        config.setClientId("test-client");
        config.setClientSecret("test-secret");
        config.setOauthUsername("test-user");
        config.setOauthPassword("test-pass");
        config.setOrganizationId("test-org-123");

        httpClient = new IcecatHttpClient(config, tempDir.toString());
        tokenManager = new IcecatOAuthTokenManager(config, httpClient);
        rateLimiter = new IcecatRateLimiter(100.0); // High rate for tests

        client = new IcecatRetailerApiClient(config, httpClient, tokenManager, rateLimiter);

        // Stub token endpoint
        wireMockServer.stubFor(post(urlEqualTo("/connect/token"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "access_token": "test-token-abc123",
                                    "expires_in": 3600,
                                    "token_type": "Bearer"
                                }
                                """)));
    }

    @AfterEach
    void tearDown() {
        wireMockServer.stop();
    }

    @Test
    void shouldFetchCategoriesWithOAuth() {
        // Stub categories endpoint
        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .withHeader("Authorization", matching("Bearer .*"))
                .withHeader("organizationId", equalTo("test-org-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""
                                {
                                    "Categories": [
                                        {
                                            "CategoryId": 1,
                                            "CategoryName": "Electronics",
                                            "ParentCategoryId": null,
                                            "Level": 0
                                        },
                                        {
                                            "CategoryId": 234,
                                            "CategoryName": "Televisions",
                                            "ParentCategoryId": 1,
                                            "Level": 1
                                        }
                                    ],
                                    "TotalCount": 2
                                }
                                """)));

        List<RetailerCategory> categories = client.getCategories();

        assertThat(categories).hasSize(2);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("Electronics");
        assertThat(categories.get(1).getCategoryName()).isEqualTo("Televisions");
        assertThat(categories.get(1).getParentCategoryId()).isEqualTo(1L);
    }

    @Test
    void shouldIncludeOrganizationIdHeader() {
        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .withHeader("organizationId", equalTo("test-org-123"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"Categories\": []}")));

        client.getCategories();

        wireMockServer.verify(getRequestedFor(urlEqualTo("/TradeItem/GetCategories"))
                .withHeader("organizationId", equalTo("test-org-123")));
    }

    @Test
    void shouldHandleEmptyResponse() {
        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"Categories\": []}")));

        List<RetailerCategory> categories = client.getCategories();

        assertThat(categories).isEmpty();
    }

    @Test
    void shouldThrowOnForbiddenAccess() {
        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withBody("Access denied")));

        assertThatThrownBy(() -> client.getCategories())
                .isInstanceOf(IcecatApiException.class)
                .hasMessageContaining("forbidden");
    }

    @Test
    void shouldRetryOnRateLimiting() {
        // First call returns 429, second succeeds
        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .inScenario("RateLimit")
                .whenScenarioStateIs("Started")
                .willReturn(aResponse()
                        .withStatus(429)
                        .withHeader("Retry-After", "1"))
                .willSetStateTo("Retry"));

        wireMockServer.stubFor(get(urlEqualTo("/TradeItem/GetCategories"))
                .inScenario("RateLimit")
                .whenScenarioStateIs("Retry")
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"Categories\": [{\"CategoryId\": 1, \"CategoryName\": \"Test\"}]}")));

        List<RetailerCategory> categories = client.getCategories();

        assertThat(categories).hasSize(1);
        assertThat(categories.get(0).getCategoryName()).isEqualTo("Test");
    }

    @Test
    void shouldVerifyConfigured() {
        assertThat(client.isConfigured()).isTrue();

        // Remove required config
        config.setOrganizationId(null);
        assertThat(client.isConfigured()).isFalse();
    }
}
