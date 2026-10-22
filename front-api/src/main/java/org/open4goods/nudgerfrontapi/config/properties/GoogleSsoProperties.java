package org.open4goods.nudgerfrontapi.config.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
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
 * <p>
 * Allowlist keys are e-mail addresses and must be bracketed in YAML
 * ({@code "[john.doe@example.com]": [ROLE_SITEEDITOR]}): Spring Boot drops the
 * {@code @} of an unbracketed map key, so the entry would never match a login.
 * {@link #malformedAllowlistKeys()} exposes such keys for the startup check.
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
     * Deny-by-default email to role list allowlist. Keys are emails, normalized
     * to lower case on binding; values are the granted authorities (e.g.
     * {@code ROLE_SITEEDITOR}, {@code XWIKIADMINGROUP}). An email missing from
     * this map is refused. In YAML each key must be bracketed
     * ({@code "[john.doe@example.com]"}) to keep its {@code @}.
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

    /**
     * Replace the allowlist, normalizing every key (trimmed, lower case with
     * {@link Locale#ROOT}) so that lookups are case-insensitive. Two keys that
     * only differ by case are merged into the union of their roles.
     *
     * @param roleAllowlist email to roles map; {@code null} clears the allowlist
     */
    public void setRoleAllowlist(Map<String, List<String>> roleAllowlist) {
        Map<String, List<String>> normalized = new HashMap<>();
        if (roleAllowlist != null) {
            roleAllowlist.forEach((email, roles) -> {
                if (email == null) {
                    return;
                }
                LinkedHashSet<String> merged = new LinkedHashSet<>(
                        normalized.getOrDefault(normalize(email), List.of()));
                if (roles != null) {
                    merged.addAll(roles);
                }
                normalized.put(normalize(email), new ArrayList<>(merged));
            });
        }
        this.roleAllowlist = normalized;
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
        List<String> roles = roleAllowlist.get(normalize(email));
        return roles == null ? List.of() : new ArrayList<>(roles);
    }

    /**
     * List the allowlist keys that cannot be e-mail addresses (no {@code @}, or
     * nothing before or after it). Such a key usually comes from a YAML map key
     * written without brackets, whose {@code @} Spring Boot dropped on binding;
     * it never matches a login.
     *
     * @return the malformed keys, in no particular order; empty when all keys are valid
     */
    public List<String> malformedAllowlistKeys() {
        return roleAllowlist.keySet().stream()
                .filter(key -> !looksLikeEmail(key))
                .toList();
    }

    private static boolean looksLikeEmail(String key) {
        int at = key.indexOf('@');
        return at > 0 && at < key.length() - 1;
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
