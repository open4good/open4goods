package org.open4goods.icecat.client;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.open4goods.icecat.client.exception.IcecatApiException;
import org.open4goods.icecat.client.exception.IcecatRateLimitException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.icecat.model.retailer.RetailerCategoriesResponse;
import org.open4goods.icecat.model.retailer.RetailerCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestClient;

/**
 * Client for Icecat Retailer API with OAuth 2.0 authentication and rate limiting.
 *
 * <p>All API calls require:
 * <ul>
 *   <li>Authorization: Bearer {token}</li>
 *   <li>organizationId: {configured org ID}</li>
 * </ul>
 *
 * <p>Rate limit: 5 requests/second. HTTP 429 responses are handled by respecting Retry-After header.
 */
public class IcecatRetailerApiClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatRetailerApiClient.class);

    private static final String CATEGORIES_ENDPOINT = "/TradeItem/GetCategories";
    private static final String ORGANIZATION_ID_HEADER = "organizationId";

    private final IcecatConfiguration config;
    private final IcecatOAuthTokenManager tokenManager;
    private final IcecatRateLimiter rateLimiter;
    private final RestClient restClient;

    /**
     * Constructor for IcecatRetailerApiClient.
     *
     * @param config       the Icecat configuration
     * @param httpClient   the base HTTP client
     * @param tokenManager the OAuth token manager
     * @param rateLimiter  the rate limiter
     */
    public IcecatRetailerApiClient(IcecatConfiguration config,
                                    IcecatHttpClient httpClient,
                                    IcecatOAuthTokenManager tokenManager,
                                    IcecatRateLimiter rateLimiter) {
        this.config = config;
        this.tokenManager = tokenManager;
        this.rateLimiter = rateLimiter;
        this.restClient = RestClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        LOGGER.info("IcecatRetailerApiClient initialized for base URL: {}", config.getRetailerApiBaseUrl());
    }

    /**
     * Gets all categories from the Icecat Retailer API.
     *
     * @return list of retailer categories
     * @throws IcecatApiException if the API call fails
     */
    public List<RetailerCategory> getCategories() {
        String url = buildUrl(CATEGORIES_ENDPOINT);
        LOGGER.info("Fetching categories from Icecat Retailer API: {}", url);

        RetailerCategoriesResponse response = executeWithRateLimitAndAuth(
                url,
                RetailerCategoriesResponse.class
        );

        if (response == null || response.getCategories() == null) {
            LOGGER.warn("Empty categories response from Icecat Retailer API");
            return List.of();
        }

        LOGGER.info("Fetched {} categories from Icecat Retailer API", response.getCategories().size());
        return response.getCategories();
    }

    /**
     * Executes an API call with rate limiting and OAuth authentication.
     *
     * @param url          the URL to call
     * @param responseType the expected response type
     * @param <T>          the response type
     * @return the deserialized response
     * @throws IcecatApiException if the call fails
     */
    private <T> T executeWithRateLimitAndAuth(String url, Class<T> responseType) {
        // Apply rate limiting
        rateLimiter.acquire();

        String token = tokenManager.getValidToken();

        try {
            return restClient.get()
                    .uri(URI.create(url))
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .header(ORGANIZATION_ID_HEADER, config.getOrganizationId())
                    .exchange((request, response) -> {
                        HttpStatusCode status = response.getStatusCode();

                        // Handle rate limiting
                        if (status.value() == 429) {
                            String retryAfter = response.getHeaders().getFirst("Retry-After");
                            Duration waitDuration = parseRetryAfter(retryAfter);
                            throw new IcecatRateLimitException(url, waitDuration);
                        }

                        // Handle authentication errors
                        if (status.value() == 401) {
                            // Invalidate token and retry once
                            tokenManager.invalidateToken();
                            throw new IcecatApiException("Authentication failed, token invalidated", 401, null, url);
                        }

                        if (status.value() == 403) {
                            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                            throw new IcecatApiException("Access forbidden: " + body, 403, body, url);
                        }

                        // Handle other errors
                        if (!status.is2xxSuccessful()) {
                            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                            throw new IcecatApiException("API error: " + status, status.value(), body, url);
                        }

                        // Parse successful response
                        return response.bodyTo(responseType);
                    });

        } catch (IcecatRateLimitException e) {
            // Handle rate limit by waiting and retrying
            rateLimiter.handleRateLimitResponse(e.getRetryAfter());
            return executeWithRateLimitAndAuth(url, responseType);

        } catch (IcecatApiException e) {
            // Re-throw Icecat exceptions
            throw e;

        } catch (Exception e) {
            throw new IcecatApiException("Failed to call Icecat Retailer API: " + e.getMessage(), e, url);
        }
    }

    /**
     * Parses the Retry-After header value.
     *
     * @param retryAfter the header value (seconds or HTTP date)
     * @return the duration to wait
     */
    private Duration parseRetryAfter(String retryAfter) {
        if (retryAfter == null || retryAfter.isEmpty()) {
            return Duration.ofSeconds(1);
        }
        try {
            return Duration.ofSeconds(Long.parseLong(retryAfter));
        } catch (NumberFormatException e) {
            // If not a number, default to 1 second
            return Duration.ofSeconds(1);
        }
    }

    /**
     * Builds the full URL for an API endpoint.
     *
     * @param endpoint the API endpoint path
     * @return the full URL
     */
    private String buildUrl(String endpoint) {
        String baseUrl = config.getRetailerApiBaseUrl();
        if (baseUrl.endsWith("/")) {
            baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
        }
        if (!endpoint.startsWith("/")) {
            endpoint = "/" + endpoint;
        }
        return baseUrl + endpoint;
    }

    /**
     * Checks if the client is properly configured.
     *
     * @return true if all required configuration is present
     */
    public boolean isConfigured() {
        return config.getRetailerApiBaseUrl() != null
                && config.getOrganizationId() != null
                && tokenManager.isConfigured();
    }
}
