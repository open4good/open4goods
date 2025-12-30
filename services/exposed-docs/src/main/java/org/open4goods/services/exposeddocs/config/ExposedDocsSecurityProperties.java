package org.open4goods.services.exposeddocs.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties controlling optional authentication on exposed docs endpoints.
 */
@ConfigurationProperties(prefix = "exposed-docs.security")
public class ExposedDocsSecurityProperties
{

    private boolean enabled;

    /**
     * Indicates whether authentication is enabled.
     *
     * @return true when auth is enabled
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    /**
     * Enables or disables authentication for the exposed docs API.
     *
     * @param enabled flag to enable authentication
     */
    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }
}
