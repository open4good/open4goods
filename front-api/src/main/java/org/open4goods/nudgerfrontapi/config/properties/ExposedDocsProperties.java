package org.open4goods.nudgerfrontapi.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the exposed docs proxy in the front API.
 */
@ConfigurationProperties(prefix = "front.exposed-docs")
public class ExposedDocsProperties
{

    private String baseUrl = "http://localhost:8086";
    private boolean publicAccess = true;

    /**
     * Returns the base URL of the exposed docs microservice.
     *
     * @return base URL
     */
    public String getBaseUrl()
    {
        return baseUrl;
    }

    /**
     * Sets the base URL of the exposed docs microservice.
     *
     * @param baseUrl base URL to use
     */
    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    /**
     * Indicates whether exposed docs endpoints are publicly accessible.
     *
     * @return true when public access is enabled
     */
    public boolean isPublicAccess()
    {
        return publicAccess;
    }

    /**
     * Enables or disables public access for exposed docs endpoints.
     *
     * @param publicAccess whether to allow anonymous access
     */
    public void setPublicAccess(boolean publicAccess)
    {
        this.publicAccess = publicAccess;
    }
}
