package org.open4goods.services.googleindexation.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for Google Indexing API access.
 */
@ConfigurationProperties(prefix = "google-indexation")
public class GoogleIndexationConfig {

    /**
     * Toggle the Google Indexation client on or off.
     */
    private boolean enabled = false;

    /**
     * Raw service account JSON content used to authenticate with the Indexing API.
     */
    private String serviceAccountJson;

    /**
     * Path to the service account JSON file used to authenticate with the Indexing API.
     */
    private String serviceAccountPath;

    /**
     * Google Indexing API endpoint.
     */
    private String apiUrl = "https://indexing.googleapis.com/v3/urlNotifications:publish";

    /**
     * Timeout applied to outbound HTTP requests.
     */
    private Duration requestTimeout = Duration.ofSeconds(10);

    /**
     * Default batch size used when publishing multiple URLs.
     */
    private int batchSize = 50;

    /**
     * Return whether the indexation client is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the indexation client.
     *
     * @param enabled whether the client is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the raw service account JSON payload.
     *
     * @return service account JSON
     */
    public String getServiceAccountJson() {
        return serviceAccountJson;
    }

    /**
     * Set the raw service account JSON payload.
     *
     * @param serviceAccountJson service account JSON
     */
    public void setServiceAccountJson(String serviceAccountJson) {
        this.serviceAccountJson = serviceAccountJson;
    }

    /**
     * Return the configured service account JSON file path.
     *
     * @return service account JSON file path
     */
    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    /**
     * Set the service account JSON file path.
     *
     * @param serviceAccountPath service account JSON file path
     */
    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    /**
     * Return the Google Indexing API endpoint URL.
     *
     * @return API URL
     */
    public String getApiUrl() {
        return apiUrl;
    }

    /**
     * Set the Google Indexing API endpoint URL.
     *
     * @param apiUrl API URL
     */
    public void setApiUrl(String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Return the request timeout.
     *
     * @return request timeout
     */
    public Duration getRequestTimeout() {
        return requestTimeout;
    }

    /**
     * Set the request timeout.
     *
     * @param requestTimeout request timeout
     */
    public void setRequestTimeout(Duration requestTimeout) {
        this.requestTimeout = requestTimeout;
    }

    /**
     * Return the batch size.
     *
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Set the batch size.
     *
     * @param batchSize batch size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }
}
