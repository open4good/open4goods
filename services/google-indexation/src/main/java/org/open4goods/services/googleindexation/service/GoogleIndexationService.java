package org.open4goods.services.googleindexation.service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import org.open4goods.services.googleindexation.config.GoogleIndexationConfig;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResult;
import org.open4goods.services.googleindexation.dto.GoogleIndexationResultItem;
import org.open4goods.services.googleindexation.exception.GoogleIndexationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auth.oauth2.AccessToken;
import com.google.auth.oauth2.GoogleCredentials;

import io.micrometer.core.instrument.MeterRegistry;

/**
 * Service responsible for publishing URLs to the Google Indexing API.
 * <p>
 * The service exposes:
 * <ul>
 * <li>{@link #submitUrl(String)} for asynchronous submission (recommended for hooks/event listeners),</li>
 * <li>{@link #publishUrl(String)} for immediate synchronous publication,</li>
 * <li>{@link #publishUrls(List)} for synchronous batch publication.</li>
 * </ul>
 */
@Service
public class GoogleIndexationService implements HealthIndicator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleIndexationService.class);
    private static final String INDEXING_SCOPE = "https://www.googleapis.com/auth/indexing";

    private final GoogleIndexationConfig config;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;
    private final ExecutorService submitExecutor;
    private final Map<String, Instant> recentSubmissions = java.util.Collections.synchronizedMap(new LinkedHashMap<>());

    private final AtomicReference<GoogleCredentials> credentials = new AtomicReference<>();
    private final CredentialsProvider credentialsProvider;
    private volatile String lastErrorMessage;
    private volatile Instant lastSuccessAt;

    /**
     * Create the service with the default HTTP client.
     *
     * @param config configuration properties
     * @param meterRegistry meter registry for metrics
     */
    @Autowired
    public GoogleIndexationService(GoogleIndexationConfig config, MeterRegistry meterRegistry) {
        this(config, meterRegistry, HttpClient.newHttpClient(), null);
    }

    /**
     * Create the service with a custom HTTP client.
     *
     * @param config configuration properties
     * @param meterRegistry meter registry for metrics
     * @param httpClient HTTP client used for outbound calls
     * @param credentialsProvider credentials provider override
     */
    GoogleIndexationService(GoogleIndexationConfig config, MeterRegistry meterRegistry, HttpClient httpClient,
            CredentialsProvider credentialsProvider) {
        this.config = config;
        this.meterRegistry = meterRegistry;
        this.httpClient = httpClient;
        this.credentialsProvider = credentialsProvider != null ? credentialsProvider : this::loadCredentials;
        this.submitExecutor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "google-indexation-submit");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Submit a URL for asynchronous publication with deduplication and retry.
     *
     * @param url URL to submit
     */
    public void submitUrl(String url) {
        if (!StringUtils.hasText(url)) {
            return;
        }
        if (wasSubmittedRecently(url, Instant.now())) {
            meterRegistry.counter("google.indexation.submit.deduplicated").increment();
            return;
        }
        submitExecutor.submit(() -> {
            GoogleIndexationResultItem result = publishUrl(url);
            if (!result.success()) {
                GoogleIndexationResultItem retryResult = publishUrl(url);
                if (!retryResult.success()) {
                    LOGGER.warn("Google indexation submit failed for {} after retry: {}", url, retryResult.message());
                }
            }
        });
    }

    private boolean wasSubmittedRecently(String url, Instant now) {
        synchronized (recentSubmissions) {
            recentSubmissions.entrySet().removeIf(entry -> entry.getValue().isBefore(now.minusSeconds(300)));
            if (recentSubmissions.containsKey(url)) {
                return true;
            }
            recentSubmissions.put(url, now);
            return false;
        }
    }

    /**
     * Publish a list of URLs to the Google Indexing API.
     *
     * @param urls URLs to notify
     * @return summary result for the batch
     */
    public GoogleIndexationResult publishUrls(List<String> urls) {
        if (!config.isEnabled()) {
            LOGGER.debug("Google indexation is disabled; skipping {} URLs.", urls == null ? 0 : urls.size());
            return new GoogleIndexationResult(0, 0, 0, Instant.now(), List.of());
        }
        if (urls == null || urls.isEmpty()) {
            return new GoogleIndexationResult(0, 0, 0, Instant.now(), List.of());
        }

        List<GoogleIndexationResultItem> items = new ArrayList<>();
        int successCount = 0;
        int failureCount = 0;

        for (String url : urls) {
            GoogleIndexationResultItem item = publishUrl(url);
            items.add(item);
            if (item.success()) {
                successCount++;
            } else {
                failureCount++;
            }
        }

        return new GoogleIndexationResult(urls.size(), successCount, failureCount, Instant.now(), items);
    }

    /**
     * Publish a single URL to the Google Indexing API.
     *
     * @param url URL to notify
     * @return per-URL result details
     */
    public GoogleIndexationResultItem publishUrl(String url) {
        if (!config.isEnabled()) {
            return new GoogleIndexationResultItem(url, false, "Google indexation is disabled", Instant.now());
        }
        if (!StringUtils.hasText(url)) {
            return new GoogleIndexationResultItem(url, false, "URL is blank", Instant.now());
        }

        meterRegistry.counter("google.indexation.publish.attempt").increment();

        try {
            AccessToken token = accessToken();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(config.getApiUrl()))
                    .timeout(config.getRequestTimeout())
                    .header("Authorization", "Bearer " + token.getTokenValue())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(buildPayload(url)))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                lastSuccessAt = Instant.now();
                lastErrorMessage = null;
                meterRegistry.counter("google.indexation.publish.success").increment();
                return new GoogleIndexationResultItem(url, true, response.body(), Instant.now());
            }

            String message = String.format("HTTP %d: %s", status, response.body());
            lastErrorMessage = message;
            meterRegistry.counter("google.indexation.publish.failure").increment();
            return new GoogleIndexationResultItem(url, false, message, Instant.now());
        } catch (Exception ex) {
            String message = ex.getMessage();
            lastErrorMessage = message;
            meterRegistry.counter("google.indexation.publish.failure").increment();
            return new GoogleIndexationResultItem(url, false, message, Instant.now());
        }
    }

    /**
     * Resolve a valid OAuth access token for the Indexing API.
     *
     * @return access token
     */
    private AccessToken accessToken() {
        GoogleCredentials resolved = credentials.updateAndGet(existing -> existing != null ? existing : credentialsProvider.get());
        try {
            resolved.refreshIfExpired();
        } catch (IOException exception) {
            throw new GoogleIndexationException("Failed to refresh Google credentials", exception);
        }
        return Optional.ofNullable(resolved.getAccessToken())
                .orElseThrow(() -> new GoogleIndexationException("Google credentials did not return an access token"));
    }

    /**
     * Load Google credentials from the configured JSON payload or file path.
     *
     * @return resolved credentials
     */
    private GoogleCredentials loadCredentials() {
        try {
            if (StringUtils.hasText(config.getServiceAccountJson())) {
                return GoogleCredentials.fromStream(new ByteArrayInputStream(
                        config.getServiceAccountJson().getBytes(StandardCharsets.UTF_8)))
                        .createScoped(List.of(INDEXING_SCOPE));
            }
            if (StringUtils.hasText(config.getServiceAccountPath())) {
                byte[] content = Files.readAllBytes(Path.of(config.getServiceAccountPath()));
                return GoogleCredentials.fromStream(new ByteArrayInputStream(content))
                        .createScoped(List.of(INDEXING_SCOPE));
            }
        } catch (IOException exception) {
            throw new GoogleIndexationException("Unable to load Google credentials", exception);
        }
        throw new GoogleIndexationException("Missing Google Indexation credentials");
    }

    /**
     * Build the JSON payload sent to the Indexing API.
     *
     * @param url URL to publish
     * @return JSON payload
     */
    private String buildPayload(String url) throws IOException {
        return objectMapper.writeValueAsString(new Payload(url, "URL_UPDATED"));
    }

    /**
     * Return the last error message captured by the client.
     *
     * @return last error or {@code null}
     */
    public String getLastErrorMessage() {
        return lastErrorMessage;
    }

    /**
     * Return whether the Google Indexation client is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return config.isEnabled();
    }

    /**
     * Return the last success timestamp captured by the client.
     *
     * @return last success timestamp or {@code null}
     */
    public Instant getLastSuccessAt() {
        return lastSuccessAt;
    }

    /**
     * Report health information for the indexation client.
     *
     * @return health status
     */
    @Override
    public Health health() {
        if (!config.isEnabled()) {
            return Health.up().withDetail("enabled", false).build();
        }
        if (!StringUtils.hasText(config.getServiceAccountJson()) && !StringUtils.hasText(config.getServiceAccountPath())) {
            return Health.down().withDetail("reason", "Missing service account credentials").build();
        }
        Health.Builder builder = lastErrorMessage == null ? Health.up() : Health.down();
        builder.withDetail("enabled", true);
        if (lastSuccessAt != null) {
            builder.withDetail("lastSuccessAt", lastSuccessAt.toString());
        }
        if (lastErrorMessage != null) {
            builder.withDetail("lastError", lastErrorMessage);
        }
        return builder.build();
    }

    /**
     * Payload type sent to the Indexing API.
     *
     * @param url URL to notify
     * @param type notification type
     */
    private record Payload(String url, String type) {
    }

    /**
     * Provider used to resolve Google credentials, mainly for testing.
     */
    @FunctionalInterface
    interface CredentialsProvider {

        /**
         * Return resolved Google credentials.
         *
         * @return Google credentials instance
         */
        GoogleCredentials get();
    }
}
