package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
