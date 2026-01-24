package org.open4goods.nudgerfrontapi.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;

import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;
import org.open4goods.nudgerfrontapi.config.properties.GoogleIndexingProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

/**
 * HTTP client that authenticates with Google and publishes URL notifications.
 */
@Service
public class GoogleIndexingHttpClient implements GoogleIndexingClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexingHttpClient.class);
    private static final String INDEXING_SCOPE = "https://www.googleapis.com/auth/indexing";

    private final GoogleIndexingProperties properties;
    private final RestClient restClient;
    private final Clock clock;

    private GoogleCredentials credentials;
    private Instant tokenExpiry;

    /**
     * Create the Indexing API client.
     *
     * @param properties configuration properties for Google Indexing
     * @param clock      clock used for token expiry
     */
    public GoogleIndexingHttpClient(GoogleIndexingProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Publish a URL notification to Google.
     *
     * @param url absolute URL to publish
     * @return {@code true} when the request succeeds
     */
    @Override
    public boolean publish(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            AccessToken accessToken = getAccessToken();
            restClient.post()
                    .uri(properties.getApiEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken.getTokenValue())
                    .body(new GoogleIndexingRequest(url, "URL_UPDATED"))
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception ex) {
            LOGGER.warn("Failed to publish URL {} to Google Indexing API: {}", url, ex.getMessage());
            return false;
        }
    }

    /**
     * Resolve a valid OAuth access token for the service account.
     *
     * @return access token used to authenticate API calls
     * @throws IOException when credentials cannot be loaded
     */
    private AccessToken getAccessToken() throws IOException {
        if (credentials == null) {
            credentials = loadCredentials();
        }
        if (tokenExpiry == null || tokenExpiry.isBefore(clock.instant().plusSeconds(60))) {
            credentials.refreshIfExpired();
            AccessToken token = credentials.getAccessToken();
            if (token != null) {
                tokenExpiry = token.getExpirationTime() != null
                        ? token.getExpirationTime().toInstant()
                        : clock.instant().plusSeconds(300);
            }
        }
        AccessToken token = credentials.getAccessToken();
        if (token == null) {
            token = credentials.refreshAccessToken();
            tokenExpiry = token.getExpirationTime() != null
                    ? token.getExpirationTime().toInstant()
                    : clock.instant().plusSeconds(300);
        }
        return token;
    }

    /**
     * Load Google credentials from JSON or file path.
     *
     * @return configured Google credentials
     * @throws IOException when the credentials cannot be loaded
     */
    private GoogleCredentials loadCredentials() throws IOException {
        String json = properties.getServiceAccountJson();
        String path = properties.getServiceAccountPath();
        if (StringUtils.hasText(json)) {
            return GoogleCredentials.fromStream(
                    new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(INDEXING_SCOPE);
        }
        if (StringUtils.hasText(path)) {
            Path jsonPath = Path.of(path);
            return GoogleCredentials.fromStream(Files.newInputStream(jsonPath))
                    .createScoped(INDEXING_SCOPE);
        }
        throw new IOException("Missing service account credentials for Google Indexing API");
    }

    /**
     * Payload sent to the Indexing API.
     *
     * @param url  absolute URL to notify
     * @param type notification type
     */
    private record GoogleIndexingRequest(String url, String type) {
    }
}
