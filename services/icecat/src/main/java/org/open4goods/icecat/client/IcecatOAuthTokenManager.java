package org.open4goods.icecat.client;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.open4goods.icecat.client.exception.IcecatAuthenticationException;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Manages OAuth 2.0 tokens for Icecat Retailer API.
 * Handles token retrieval, caching, and automatic refresh before expiry.
 *
 * <p>Uses password grant type with the following flow:
 * <pre>
 * POST /cdm-cedemo-authenticationservice/connect/token
 * Content-Type: application/x-www-form-urlencoded
 *
 * client_id=xxx&client_secret=xxx&grant_type=password&username=xxx&password=xxx
 * </pre>
 */
public class IcecatOAuthTokenManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(IcecatOAuthTokenManager.class);

    /**
     * Refresh token 5 minutes before expiry to avoid edge cases.
     */
    private static final long TOKEN_REFRESH_BUFFER_SECONDS = 300;

    private final IcecatConfiguration config;
    private final IcecatHttpClient httpClient;
    private final ReentrantLock tokenLock = new ReentrantLock();

    private volatile String accessToken;
    private volatile Instant tokenExpiry;

    /**
     * Constructor for IcecatOAuthTokenManager.
     *
     * @param config     the Icecat configuration containing OAuth credentials
     * @param httpClient the HTTP client for making token requests
     */
    public IcecatOAuthTokenManager(IcecatConfiguration config, IcecatHttpClient httpClient) {
        this.config = config;
        this.httpClient = httpClient;
    }

    /**
     * Gets a valid OAuth access token.
     * If the current token is expired or about to expire, refreshes it automatically.
     *
     * @return a valid access token
     * @throws IcecatAuthenticationException if token retrieval fails
     */
    public String getValidToken() {
        if (isTokenValid()) {
            return accessToken;
        }

        tokenLock.lock();
        try {
            // Double-check after acquiring lock
            if (isTokenValid()) {
                return accessToken;
            }
            refreshToken();
            return accessToken;
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Checks if the current token is still valid.
     *
     * @return true if token exists and is not expired (with buffer)
     */
    private boolean isTokenValid() {
        if (accessToken == null || tokenExpiry == null) {
            return false;
        }
        return Instant.now().plusSeconds(TOKEN_REFRESH_BUFFER_SECONDS).isBefore(tokenExpiry);
    }

    /**
     * Refreshes the OAuth token by calling the token endpoint.
     *
     * @throws IcecatAuthenticationException if token retrieval fails
     */
    private void refreshToken() {
        LOGGER.info("Refreshing OAuth token for Icecat Retailer API");

        String tokenUrl = buildTokenUrl();
        Map<String, String> formData = Map.of(
                "grant_type", "password",
                "client_id", config.getClientId(),
                "client_secret", config.getClientSecret(),
                "username", config.getOauthUsername(),
                "password", config.getOauthPassword()
        );

        try {
            TokenResponse response = httpClient.postForm(tokenUrl, formData, TokenResponse.class);

            if (response == null || response.accessToken == null) {
                throw new IcecatAuthenticationException(
                        "Empty token response from OAuth endpoint",
                        401, tokenUrl);
            }

            this.accessToken = response.accessToken;
            this.tokenExpiry = Instant.now().plusSeconds(response.expiresIn);

            LOGGER.info("Successfully obtained OAuth token, expires in {} seconds", response.expiresIn);

        } catch (IcecatAuthenticationException e) {
            throw e;
        } catch (Exception e) {
            throw new IcecatAuthenticationException(
                    "Failed to obtain OAuth token: " + e.getMessage(),
                    401, tokenUrl);
        }
    }

    /**
     * Builds the full token endpoint URL.
     *
     * @return the complete token URL
     */
    private String buildTokenUrl() {
        String baseUrl = config.getRetailerApiBaseUrl();
        String tokenEndpoint = config.getTokenEndpoint();

        // Handle relative vs absolute token endpoint
        if (tokenEndpoint.startsWith("http")) {
            return tokenEndpoint;
        }

        // Ensure proper URL construction
        if (!baseUrl.endsWith("/") && !tokenEndpoint.startsWith("/")) {
            return baseUrl + "/" + tokenEndpoint;
        }
        return baseUrl + tokenEndpoint;
    }

    /**
     * Invalidates the current token, forcing a refresh on next access.
     */
    public void invalidateToken() {
        tokenLock.lock();
        try {
            this.accessToken = null;
            this.tokenExpiry = null;
            LOGGER.info("OAuth token invalidated");
        } finally {
            tokenLock.unlock();
        }
    }

    /**
     * Checks if the token manager is configured with required credentials.
     *
     * @return true if all required OAuth credentials are configured
     */
    public boolean isConfigured() {
        return config.getClientId() != null
                && config.getClientSecret() != null
                && config.getOauthUsername() != null
                && config.getOauthPassword() != null
                && config.getRetailerApiBaseUrl() != null;
    }

    /**
     * OAuth token response DTO.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TokenResponse {

        @JsonProperty("access_token")
        public String accessToken;

        @JsonProperty("expires_in")
        public long expiresIn;

        @JsonProperty("token_type")
        public String tokenType;

        @JsonProperty("refresh_token")
        public String refreshToken;

        @JsonProperty("scope")
        public String scope;
    }
}
