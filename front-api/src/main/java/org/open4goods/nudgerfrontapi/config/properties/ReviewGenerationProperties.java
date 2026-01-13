package org.open4goods.nudgerfrontapi.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

/**
 * Configuration describing how the front API calls the back-office review generation endpoints.
 */
@Validated
@ConfigurationProperties("front.review-generation")
public class ReviewGenerationProperties {

    /**
     * Base URL of the back-office API exposing review generation endpoints.
     */
    @NotBlank(message = "front.review-generation.api-base-url must be provided")
    private String apiBaseUrl;

    /**
     * API key forwarded to authenticate against the back-office API.
     */
    @NotBlank(message = "front.review-generation.api-key must be provided")
    private String apiKey;

    /**
     * Path appended to the base URL when triggering or polling review generation.
     */
    private String reviewPath = "/review";

    /**
     * Quota settings applied to review generation requests.
     */
    private final Quota quota = new Quota();

    /**
     * Return the configured base URL for review generation requests.
     *
     * @return base URL of the back-office API
     */
    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    /**
     * Define the base URL for review generation requests.
     *
     * @param apiBaseUrl back-office API base URL
     */
    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    /**
     * Return the API key used to authenticate with the back-office API.
     *
     * @return API key value
     */
    public String getApiKey() {
        return apiKey;
    }

    /**
     * Define the API key used to authenticate with the back-office API.
     *
     * @param apiKey API key value
     */
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    /**
     * Return the relative path appended to the review generation base URL.
     *
     * @return review path
     */
    public String getReviewPath() {
        return reviewPath;
    }

    /**
     * Define the relative path appended to the review generation base URL.
     *
     * @param reviewPath path to use for review generation endpoints
     */
    public void setReviewPath(String reviewPath) {
        this.reviewPath = reviewPath;
    }

    /**
     * Return the quota configuration applied to review generation requests.
     *
     * @return quota configuration object
     */
    public Quota getQuota() {
        return quota;
    }

    /**
     * Timeout for the connection to the back-office API.
     */
    private Duration connectTimeout = Duration.ofSeconds(5);

    /**
     * Timeout for reading the response from the back-office API.
     */
    private Duration readTimeout = Duration.ofSeconds(30);

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Configuration properties controlling IP-based quotas for review generation.
     */
    public static class Quota {

        /**
         * Maximum number of review generations allowed per IP during the quota window.
         */
        @Min(1)
        private int maxPerIp = 3;

        /**
         * Duration of the quota window used to count review generations per IP.
         */
        @NotNull
        private Duration window = Duration.ofHours(24);

        /**
         * Return the maximum number of review generations allowed per IP.
         *
         * @return maximum allowed review generations
         */
        public int getMaxPerIp() {
            return maxPerIp;
        }

        /**
         * Define the maximum number of review generations allowed per IP.
         *
         * @param maxPerIp maximum allowed review generations
         */
        public void setMaxPerIp(int maxPerIp) {
            this.maxPerIp = maxPerIp;
        }

        /**
         * Return the quota window duration.
         *
         * @return duration of the quota window
         */
        public Duration getWindow() {
            return window;
        }

        /**
         * Define the quota window duration.
         *
         * @param window duration of the quota window
         */
        public void setWindow(Duration window) {
            this.window = window;
        }
    }
}
