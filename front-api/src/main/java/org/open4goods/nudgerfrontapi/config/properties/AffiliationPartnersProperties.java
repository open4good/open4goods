package org.open4goods.nudgerfrontapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties describing the affiliation partners backend used by the frontend API.
 */
@Validated
@ConfigurationProperties("front.affiliation-partners")
public class AffiliationPartnersProperties {

    /**
     * Base URL of the backend exposing affiliation partners information.
     */
    @NotBlank(message = "front.affiliation-partners.api-base-url must be provided")
    private String apiBaseUrl;

    /**
     * Path appended to the base URL to retrieve the partners collection.
     */
    @NotBlank(message = "front.affiliation-partners.partners-path must be provided")
    private String partnersPath = "/partners";

    /**
     * API key forwarded in the {@code Authorization} header when calling the backend.
     */
    @NotBlank(message = "front.affiliation-partners.api-key must be provided")
    private String apiKey;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getPartnersPath() {
        return partnersPath;
    }

    public void setPartnersPath(String partnersPath) {
        this.partnersPath = partnersPath;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
