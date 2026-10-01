package org.open4goods.ui.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.ui.config.properties.UiGoogleSsoProperties;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

/**
 * Resolves the application roles granted to a Google-authenticated ui user.
 * <p>
 * Deny-by-default per ADR-0009: the verified email must both have {@code email_verified=true}
 * and be present in {@link UiGoogleSsoProperties#getRoleAllowlist()}, or authentication is
 * refused.
 */
public class UiOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    private final OidcUserService delegate;
    private final UiGoogleSsoProperties properties;

    public UiOidcUserService(UiGoogleSsoProperties properties) {
        this(properties, new OidcUserService());
    }

    UiOidcUserService(UiGoogleSsoProperties properties, OidcUserService delegate) {
        this.properties = properties;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_verified"), "Google account email is not verified");
        }

        List<String> roles = properties.rolesFor(oidcUser.getEmail());
        if (roles.isEmpty()) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("email_not_allowlisted"), "Email is not allowlisted for ui access");
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
