package org.open4goods.api.config.yml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Google OIDC single sign-on used as the api's interactive login
 * (GOU-1392, GOU-1393), on the model of the admin console's {@code admin.security.google-sso}.
 * <p>
 * Deny-by-default: an email absent from {@link #roleAllowlist} is refused, and no role is ever
 * derived from an email domain. The granted values are api authorities as checked by
 * {@code @PreAuthorize("hasAuthority(...)")}: {@code ROLE_USER}, {@code XWIKIADMINGROUP}
 * ({@code RolesConstants.ROLE_ADMIN}), {@code XWIKIALLGROUP}, {@code ROLE_TESTER}.
 * </p>
 * <p>
 * Each YAML key must be bracketed and quoted ({@code "[jane@example.com]": [ROLE_USER]}): Spring Boot
 * drops the {@code @} of an unbracketed map key, and {@link #malformedAllowlistKeys()} exposes such
 * keys for the startup check.
 * </p>
 */
@ConfigurationProperties(prefix = "api.security.google-sso")
public class ApiGoogleSsoProperties {

    /**
     * Deny-by-default email to authority list allowlist. Keys are emails, lower-cased on binding;
     * values are the granted api authorities. An email missing from this map is refused.
     */
    private Map<String, List<String>> roleAllowlist = new HashMap<>();

    public Map<String, List<String>> getRoleAllowlist() {
        return roleAllowlist;
    }

    /**
     * Replace the allowlist, normalising each key (trimmed, lower-cased).
     *
     * @param roleAllowlist email to authorities map; {@code null} clears the allowlist
     */
    public void setRoleAllowlist(Map<String, List<String>> roleAllowlist) {
        Map<String, List<String>> normalized = new HashMap<>();
        if (roleAllowlist != null) {
            roleAllowlist.forEach((email, roles) -> {
                if (email != null && roles != null) {
                    normalized.put(normalize(email), List.copyOf(roles));
                }
            });
        }
        this.roleAllowlist = normalized;
    }

    /**
     * Resolve the authorities granted to an email, independently of casing.
     *
     * @param email candidate email, as extracted from a verified Google identity
     * @return the allowlisted authorities, or an empty list when the email is absent
     */
    public List<String> rolesFor(String email) {
        if (email == null) {
            return List.of();
        }
        List<String> roles = roleAllowlist.get(normalize(email));
        return roles == null ? List.of() : new ArrayList<>(roles);
    }

    /**
     * Keys that cannot match a login because they are not email addresses, typically an unbracketed
     * YAML key whose {@code @} was dropped on binding.
     *
     * @return the malformed keys, in no particular order; empty when all keys are valid
     */
    public List<String> malformedAllowlistKeys() {
        return roleAllowlist.keySet().stream()
                .filter(key -> key.indexOf('@') <= 0 || key.endsWith("@"))
                .toList();
    }

    private static String normalize(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }
}
