package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the geocode service integration.
 */
@ConfigurationProperties(prefix = "front.geocode")
public class GeocodeProperties
{
    /**
     * Base URL for the geocode microservice.
     */
    private String baseUrl = "http://localhost:8089";

    /**
     * Returns the base URL of the geocode service.
     *
     * @return base URL
     */
    public String getBaseUrl()
    {
        return baseUrl;
    }

    /**
     * Sets the base URL of the geocode service.
     *
     * @param baseUrl base URL
     */
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
}
