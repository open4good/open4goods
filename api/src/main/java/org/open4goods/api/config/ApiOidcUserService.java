package org.open4goods.api.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.open4goods.api.config.yml.ApiGoogleSsoProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
 * Resolves the api authorities granted to a Google-authenticated user (GOU-1393).
 * <p>
 * Deny-by-default, as the admin console's {@code AdminOidcUserService}: the Google identity must have
 * {@code email_verified=true} and its email must be present in
 * {@link ApiGoogleSsoProperties#getRoleAllowlist()}, or authentication is refused with one of the
 * {@link #REFUSAL_CODES}.
 */
public class ApiOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    /** OAuth2 error code of a Google identity whose email is not verified. */
    static final String EMAIL_NOT_VERIFIED = "email_not_verified";

    /** OAuth2 error code of a verified email absent from the allowlist. */
    static final String EMAIL_NOT_ALLOWLISTED = "email_not_allowlisted";

    /** The error codes of a refusal by this service, answered with a 403 by the login failure handler. */
    static final Set<String> REFUSAL_CODES = Set.of(EMAIL_NOT_VERIFIED, EMAIL_NOT_ALLOWLISTED);

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiOidcUserService.class);

    private final OidcUserService delegate;
    private final ApiGoogleSsoProperties properties;

    public ApiOidcUserService(ApiGoogleSsoProperties properties) {
        this(properties, new OidcUserService());
    }

    ApiOidcUserService(ApiGoogleSsoProperties properties, OidcUserService delegate) {
        this.properties = properties;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);

        if (!Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            LOGGER.warn("Google SSO login refused (403): email not verified");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(EMAIL_NOT_VERIFIED), "Google account email is not verified");
        }

        List<String> roles = properties.rolesFor(oidcUser.getEmail());
        if (roles.isEmpty()) {
            LOGGER.warn("Google SSO login refused (403): email not in api.security.google-sso.role-allowlist");
            throw new OAuth2AuthenticationException(
                    new OAuth2Error(EMAIL_NOT_ALLOWLISTED), "Email is not allowlisted for api access");
        }

        Set<GrantedAuthority> authorities = new LinkedHashSet<>(oidcUser.getAuthorities());
        roles.forEach(role -> authorities.add(new SimpleGrantedAuthority(role)));

        return new DefaultOidcUser(authorities, oidcUser.getIdToken(), oidcUser.getUserInfo());
    }
}
