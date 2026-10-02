package org.open4goods.ui.config.properties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for the Google OIDC single sign-on used as ui's interactive login
 * (replaces the former XWiki password login, per ADR-0009).
 * <p>
 * Deny-by-default: an email absent from {@link #roleAllowlist} is refused, and no role is ever
 * derived from an email domain.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "ui.security.google-sso")
public class UiGoogleSsoProperties {

    /**
     * Deny-by-default email to role list allowlist. Keys are lower-cased emails; values are the
     * granted authorities. An email missing from this map is refused.
     */
    private Map<String, List<String>> roleAllowlist = new HashMap<>();

    public Map<String, List<String>> getRoleAllowlist() {
        return roleAllowlist;
    }

    public void setRoleAllowlist(Map<String, List<String>> roleAllowlist) {
        this.roleAllowlist = roleAllowlist == null ? new HashMap<>() : roleAllowlist;
    }

    /**
     * Resolve the roles granted to an email, independently of casing.
     *
     * @param email candidate email, as extracted from a verified Google identity
     * @return the allowlisted roles, or an empty list when the email is absent
     */
    public List<String> rolesFor(String email) {
        if (email == null) {
            return List.of();
        }
        List<String> roles = roleAllowlist.get(email.toLowerCase(Locale.ROOT));
        return roles == null ? List.of() : new ArrayList<>(roles);
    }
}
