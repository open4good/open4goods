package org.open4goods.api.config.yml;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for the outbound product-alert HTTP integration.
 */
@Configuration
@ConfigurationProperties(prefix = "price-alerting")
public class PriceAlertingProperties
{
    /**
     * Enables outbound publication of price-drop events.
     */
    private boolean enabled = false;

    /**
     * Base URL of the product-alert service.
     */
    private String baseUrl = "http://localhost:8087";

    /**
     * Connection timeout for outbound HTTP calls.
     */
    private Duration connectTimeout = Duration.ofSeconds(2);

    /**
     * Read timeout for outbound HTTP calls.
     */
    private Duration readTimeout = Duration.ofSeconds(5);

    /**
     * Optional internal API key sent to the product-alert service.
     */
    private String apiKey;

    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public Duration getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    public String getApiKey()
    {
        return apiKey;
    }

    public void setApiKey(String apiKey)
    {
        this.apiKey = apiKey;
    }
}
