package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Google OIDC single sign-on.
 * <p>
 * Disabled by default: no beta or production rollout is authorized by this
 * configuration alone. The role allowlist is deny-by-default - an email absent
 * from {@link #roleAllowlist} is refused, and no role is ever derived from an
 * email domain.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "front.security.google-sso")
public class GoogleSsoProperties {

    /**
     * Master switch for the Google SSO login endpoint. Must stay {@code false}
     * outside an explicitly authorized environment.
     */
    private boolean enabled = false;

    /**
     * OAuth client id issued by Google for this environment's loopback client.
     */
    private String clientId = "";

    /**
     * Expected {@code iss} claim of the Google ID token.
     */
    private String issuer = "https://accounts.google.com";

    /**
     * JWKS endpoint used to resolve Google's signing keys.
     */
    private String jwksUri = "https://www.googleapis.com/oauth2/v3/certs";

    /**
     * Deny-by-default email to role list allowlist. Keys are lower-cased emails;
     * values are the granted authorities (e.g. {@code ROLE_SITEEDITOR},
     * {@code XWIKIADMINGROUP}). An email missing from this map is refused.
     */
    private Map<String, List<String>> roleAllowlist = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Map<String, List<String>> getRoleAllowlist() {
        return roleAllowlist;
    }

    public void setRoleAllowlist(Map<String, List<String>> roleAllowlist) {
        this.roleAllowlist = roleAllowlist == null ? new HashMap<>() : roleAllowlist;
    }

    /**
     * Resolve the roles granted to an email, independently of casing.
     *
     * @param email candidate email, as extracted from a verified ID token
     * @return the allowlisted roles, or an empty list when the email is absent
     */
    public List<String> rolesFor(String email) {
        if (email == null) {
            return List.of();
        }
        List<String> roles = roleAllowlist.get(email.toLowerCase(java.util.Locale.ROOT));
        return roles == null ? List.of() : new ArrayList<>(roles);
    }
}
