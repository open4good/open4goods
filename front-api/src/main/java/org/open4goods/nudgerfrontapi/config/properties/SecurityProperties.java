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

    /** Google OpenID Connect verification and explicit user-role assignments. */
    private Google google = new Google();

    /**
     * Google identity configuration. This is intentionally empty by default so
     * local startup has no dependency on a remote identity provider.
     */
    public static class Google {

        private String issuer = "https://accounts.google.com";
        private String audience;
        private List<EmailRoles> allowedRoles = new ArrayList<>();

        public String getIssuer() { return issuer; }
        public void setIssuer(String issuer) { this.issuer = issuer; }
        public String getAudience() { return audience; }
        public void setAudience(String audience) { this.audience = audience; }
        public List<EmailRoles> getAllowedRoles() { return allowedRoles; }
        public void setAllowedRoles(List<EmailRoles> allowedRoles) { this.allowedRoles = allowedRoles; }
    }

    /** Explicit environment-held role assignment for one verified email. */
    public static class EmailRoles {

        private String email;
        private List<String> roles = new ArrayList<>();

        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
    }

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

    public Google getGoogle() { return google; }
    public void setGoogle(Google google) { this.google = google; }
}
