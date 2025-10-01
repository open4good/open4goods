package org.open4goods.nudgerfrontapi.config.properties;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties to enable or disable Spring Security for the
 * frontend API.
 */
@Validated
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
    @NotBlank(message = "front.security.jwt-secret must be provided")
    @Size(min = 33, message = "front.security.jwt-secret must contain more than 32 characters")
    private String jwtSecret;

    /**
     * Shared secret expected in the {@code X-Shared-Token} header for
     * authenticated requests.
     */
    private String sharedToken;

    /**
     * Access token validity duration.
     */
    private Duration accessTokenExpiry = Duration.ofMinutes(15);

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

    public String getSharedToken() {
        return sharedToken;
    }

    public void setSharedToken(String sharedToken) {
        this.sharedToken = sharedToken;
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
