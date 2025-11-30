package org.open4goods.nudgerfrontapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

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
     * Maximum number of AI review generations allowed per IP and per day.
     */
    private int maxGenerationsPerIpPerDay = 3;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getReviewPath() {
        return reviewPath;
    }

    public void setReviewPath(String reviewPath) {
        this.reviewPath = reviewPath;
    }

    public int getMaxGenerationsPerIpPerDay() {
        return maxGenerationsPerIpPerDay;
    }

    public void setMaxGenerationsPerIpPerDay(int maxGenerationsPerIpPerDay) {
        this.maxGenerationsPerIpPerDay = maxGenerationsPerIpPerDay;
    }
}
