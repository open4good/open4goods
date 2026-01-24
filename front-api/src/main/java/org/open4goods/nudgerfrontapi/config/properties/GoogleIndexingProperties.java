package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Google Indexing API integration.
 */
@Validated
@ConfigurationProperties(prefix = "front.google-indexing")
public class GoogleIndexingProperties {

    /**
     * Toggle Google Indexing API integration.
     */
    private boolean enabled = false;

    /**
     * Public base URL of the Nuxt frontend used to build canonical product URLs.
     */
    @NotBlank(message = "front.google-indexing.site-base-url must be provided")
    private String siteBaseUrl = "https://nudger.fr";

    /**
     * Endpoint used to publish URL notifications.
     */
    private String apiEndpoint = "https://indexing.googleapis.com/v3/urlNotifications:publish";

    /**
     * Service account JSON payload used to authenticate with Google APIs.
     */
    private String serviceAccountJson;

    /**
     * Path to a service account JSON file used to authenticate with Google APIs.
     */
    private String serviceAccountPath;

    /**
     * Maximum number of URLs processed per batch execution.
     */
    @Positive
    private int batchSize = 50;

    /**
     * Duration to wait before retrying a failed URL.
     */
    private Duration retryDelay = Duration.ofMinutes(30);

    /**
     * Maximum number of retry attempts before discarding a URL.
     */
    @Positive
    private int maxAttempts = 5;

    /**
     * Enable immediate processing when a URL is enqueued.
     */
    private boolean realtimeEnabled = true;

    /**
     * Retention window for indexed and failed URLs history.
     */
    private Duration historyRetention = Duration.ofDays(7);

    /**
     * Maximum queue size before the health indicator reports a degraded status.
     */
    @Positive
    private int maxQueueSize = 500;

    /**
     * Maximum age for the last successful indexing event before health degrades.
     */
    private Duration maxSuccessAge = Duration.ofDays(7);

    /**
     * File name used to persist pending queue entries on disk.
     */
    private String queueFileName = "google-indexing-queue.json";

    /**
     * Return whether Google Indexing integration is enabled.
     *
     * @return {@code true} when indexing is enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable Google Indexing integration.
     *
     * @param enabled flag to enable indexing
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return the public base URL of the Nuxt frontend.
     *
     * @return base URL used to build canonical URLs
     */
    public String getSiteBaseUrl() {
        return siteBaseUrl;
    }

    /**
     * Define the public base URL of the Nuxt frontend.
     *
     * @param siteBaseUrl base URL used to build canonical URLs
     */
    public void setSiteBaseUrl(String siteBaseUrl) {
        this.siteBaseUrl = siteBaseUrl;
    }

    /**
     * Return the Google Indexing API endpoint.
     *
     * @return API endpoint URL
     */
    public String getApiEndpoint() {
        return apiEndpoint;
    }

    /**
     * Define the Google Indexing API endpoint.
     *
     * @param apiEndpoint endpoint URL
     */
    public void setApiEndpoint(String apiEndpoint) {
        this.apiEndpoint = apiEndpoint;
    }

    /**
     * Return the service account JSON payload.
     *
     * @return JSON payload
     */
    public String getServiceAccountJson() {
        return serviceAccountJson;
    }

    /**
     * Define the service account JSON payload.
     *
     * @param serviceAccountJson JSON payload
     */
    public void setServiceAccountJson(String serviceAccountJson) {
        this.serviceAccountJson = serviceAccountJson;
    }

    /**
     * Return the path to the service account JSON file.
     *
     * @return JSON file path
     */
    public String getServiceAccountPath() {
        return serviceAccountPath;
    }

    /**
     * Define the path to the service account JSON file.
     *
     * @param serviceAccountPath JSON file path
     */
    public void setServiceAccountPath(String serviceAccountPath) {
        this.serviceAccountPath = serviceAccountPath;
    }

    /**
     * Return the batch size for submissions.
     *
     * @return batch size
     */
    public int getBatchSize() {
        return batchSize;
    }

    /**
     * Define the batch size for submissions.
     *
     * @param batchSize batch size
     */
    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    /**
     * Return the retry delay for failed submissions.
     *
     * @return retry delay
     */
    public Duration getRetryDelay() {
        return retryDelay;
    }

    /**
     * Define the retry delay for failed submissions.
     *
     * @param retryDelay retry delay
     */
    public void setRetryDelay(Duration retryDelay) {
        this.retryDelay = retryDelay;
    }

    /**
     * Return the maximum number of retry attempts.
     *
     * @return max attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Define the maximum number of retry attempts.
     *
     * @param maxAttempts max attempts
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Return whether realtime processing is enabled.
     *
     * @return realtime enabled flag
     */
    public boolean isRealtimeEnabled() {
        return realtimeEnabled;
    }

    /**
     * Define whether realtime processing is enabled.
     *
     * @param realtimeEnabled flag enabling realtime processing
     */
    public void setRealtimeEnabled(boolean realtimeEnabled) {
        this.realtimeEnabled = realtimeEnabled;
    }

    /**
     * Return the retention duration for indexing history.
     *
     * @return retention duration
     */
    public Duration getHistoryRetention() {
        return historyRetention;
    }

    /**
     * Define the retention duration for indexing history.
     *
     * @param historyRetention retention duration
     */
    public void setHistoryRetention(Duration historyRetention) {
        this.historyRetention = historyRetention;
    }

    /**
     * Return the maximum queue size tolerated by the health indicator.
     *
     * @return queue size limit
     */
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    /**
     * Define the maximum queue size tolerated by the health indicator.
     *
     * @param maxQueueSize queue size limit
     */
    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Return the maximum allowed age for the last success.
     *
     * @return max success age
     */
    public Duration getMaxSuccessAge() {
        return maxSuccessAge;
    }

    /**
     * Define the maximum allowed age for the last success.
     *
     * @param maxSuccessAge max success age
     */
    public void setMaxSuccessAge(Duration maxSuccessAge) {
        this.maxSuccessAge = maxSuccessAge;
    }

    /**
     * Return the queue file name used for persistence.
     *
     * @return queue file name
     */
    public String getQueueFileName() {
        return queueFileName;
    }

    /**
     * Define the queue file name used for persistence.
     *
     * @param queueFileName queue file name
     */
    public void setQueueFileName(String queueFileName) {
        this.queueFileName = queueFileName;
    }
}
