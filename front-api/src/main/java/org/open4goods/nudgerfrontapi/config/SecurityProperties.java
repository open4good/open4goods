package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties to enable or disable Spring Security for the
 * frontend API.
 */
@Component
@ConfigurationProperties(prefix = "front.security")
public class SecurityProperties {

    /**
     * Whether Spring Security is enabled.
     */
    private boolean enabled = true;

    /**
     * List of origins allowed for CORS requests.
     */
    private List<String> corsAllowedHosts = new ArrayList<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public List<String> getCorsAllowedHosts() {
        return corsAllowedHosts;
    }

    public void setCorsAllowedHosts(List<String> corsAllowedHosts) {
        this.corsAllowedHosts = corsAllowedHosts;
    }
}
