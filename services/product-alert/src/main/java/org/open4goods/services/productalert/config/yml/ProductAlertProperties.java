package org.open4goods.services.productalert.config.yml;

import java.time.Duration;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the product alert service.
 */
@ConfigurationProperties(prefix = "product-alert")
public class ProductAlertProperties
{
    /**
     * Security-related properties for the service.
     */
    private final Security security = new Security();

    /**
     * Internal API integration properties.
     */
    private final Internal internal = new Internal();

    /**
     * Duration during which the same price should not trigger a duplicate
     * candidate for a subscription.
     */
    @NotNull
    private Duration dedupWindow = Duration.ofHours(24);

    public Security getSecurity()
    {
        return security;
    }

    public Internal getInternal()
    {
        return internal;
    }

    public Duration getDedupWindow()
    {
        return dedupWindow;
    }

    public void setDedupWindow(Duration dedupWindow)
    {
        this.dedupWindow = dedupWindow;
    }

    /**
     * Security toggle.
     */
    public static class Security
    {
        /**
         * Enables internal API key checks when set to true.
         */
        private boolean enabled = false;

        public boolean isEnabled()
        {
            return enabled;
        }

        public void setEnabled(boolean enabled)
        {
            this.enabled = enabled;
        }
    }

    /**
     * Internal API configuration.
     */
    public static class Internal
    {
        /**
         * API key expected on the internal ingestion endpoint when security is
         * enabled.
         */
        private String apiKey;

        public String getApiKey()
        {
            return apiKey;
        }

        public void setApiKey(String apiKey)
        {
            this.apiKey = apiKey;
        }
    }
}
