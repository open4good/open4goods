package org.open4goods.nudgerfrontapi.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;
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

    /**
     * Secret key used to sign JWT tokens.
     */
    // TODO : aDD VALIDATION, MUST NOT BE BLANK, MUST BE > 32 CHARS
    private String jwtSecret;

    /**
     * Access token validity duration.
     */
    private Duration accessTokenExpiry = Duration.ofMinutes(30);

    /**
     * Refresh token validity duration.
     */
    private Duration refreshTokenExpiry = Duration.ofDays(7);

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

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public Duration getAccessTokenExpiry() {
        return accessTokenExpiry;
    }

    public void setAccessTokenExpiry(Duration accessTokenExpiry) {
        this.accessTokenExpiry = accessTokenExpiry;
    }

    public Duration getRefreshTokenExpiry() {
        return refreshTokenExpiry;
    }

    public void setRefreshTokenExpiry(Duration refreshTokenExpiry) {
        this.refreshTokenExpiry = refreshTokenExpiry;
    }
}
