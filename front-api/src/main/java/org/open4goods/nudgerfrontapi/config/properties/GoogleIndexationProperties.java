package org.open4goods.nudgerfrontapi.config.properties;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration settings controlling Google Indexation dispatch in the front API.
 */
@ConfigurationProperties(prefix = "front.google-indexation")
public class GoogleIndexationProperties {

    /**
     * Enable Google Indexation dispatch from the front API.
     */
    private boolean enabled = false;

    /**
     * Enable real-time dispatch when a review generation succeeds.
     */
    private boolean realtimeEnabled = true;

    /**
     * Enable scheduled batch dispatch of queued URLs.
     */
    private boolean batchEnabled = true;

    /**
     * Fixed delay between batch dispatches.
     */
    private Duration batchInterval = Duration.ofMinutes(30);

    /**
     * Maximum number of URLs to send per batch.
     */
    private int batchSize = 50;

    /**
     * Maximum number of attempts before giving up on a URL.
     */
    private int maxAttempts = 5;

    /**
     * Base URL of the public frontend used to build canonical product URLs.
     */
    private String siteBaseUrl;

    /**
     * Return whether Google indexation is enabled.
     *
     * @return {@code true} when enabled
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable Google indexation.
     *
     * @param enabled whether indexation is enabled
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Return whether realtime dispatch is enabled.
     *
     * @return {@code true} when realtime dispatch is enabled
     */
    public boolean isRealtimeEnabled() {
        return realtimeEnabled;
    }

    /**
     * Enable or disable realtime dispatch.
     *
     * @param realtimeEnabled whether realtime dispatch is enabled
     */
    public void setRealtimeEnabled(boolean realtimeEnabled) {
        this.realtimeEnabled = realtimeEnabled;
    }

    /**
     * Return whether batch dispatch is enabled.
     *
     * @return {@code true} when batch dispatch is enabled
     */
    public boolean isBatchEnabled() {
        return batchEnabled;
    }

    /**
     * Enable or disable batch dispatch.
     *
     * @param batchEnabled whether batch dispatch is enabled
     */
    public void setBatchEnabled(boolean batchEnabled) {
        this.batchEnabled = batchEnabled;
    }

    /**
     * Return the batch interval.
     *
     * @return batch interval
     */
    public Duration getBatchInterval() {
        return batchInterval;
    }

    /**
     * Set the batch interval.
     *
     * @param batchInterval batch interval
     */
    public void setBatchInterval(Duration batchInterval) {
        this.batchInterval = batchInterval;
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

    /**
     * Return the maximum number of attempts.
     *
     * @return max attempts
     */
    public int getMaxAttempts() {
        return maxAttempts;
    }

    /**
     * Set the maximum number of attempts.
     *
     * @param maxAttempts max attempts
     */
    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    /**
     * Return the site base URL.
     *
     * @return site base URL
     */
    public String getSiteBaseUrl() {
        return siteBaseUrl;
    }

    /**
     * Set the site base URL.
     *
     * @param siteBaseUrl site base URL
     */
    public void setSiteBaseUrl(String siteBaseUrl) {
        this.siteBaseUrl = siteBaseUrl;
    }
}
