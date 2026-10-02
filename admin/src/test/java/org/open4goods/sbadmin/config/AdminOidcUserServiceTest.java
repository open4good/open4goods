package org.open4goods.sbadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.sbadmin.config.properties.AdminGoogleSsoProperties;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class AdminOidcUserServiceTest {

    @Test
    void grantsTheAllowlistedRoleForAVerifiedEmail() {
        AdminGoogleSsoProperties properties = new AdminGoogleSsoProperties();
        properties.setRoleAllowlist(Map.of("owner@example.com", java.util.List.of("ROLE_XWIKIADMINGROUP")));

        OidcUserService delegate = mock(OidcUserService.class);
        OidcUser verifiedUser = oidcUser("owner@example.com", true);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(verifiedUser);

        OidcUser result = new AdminOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class));

        assertThat(result.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_XWIKIADMINGROUP");
    }

    @Test
    void refusesAnEmailAbsentFromTheAllowlist() {
        AdminGoogleSsoProperties properties = new AdminGoogleSsoProperties();
        OidcUserService delegate = mock(OidcUserService.class);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(oidcUser("stranger@example.com", true));

        assertThatThrownBy(() -> new AdminOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void refusesAnUnverifiedEmailEvenIfAllowlisted() {
        AdminGoogleSsoProperties properties = new AdminGoogleSsoProperties();
        properties.setRoleAllowlist(Map.of("owner@example.com", java.util.List.of("ROLE_XWIKIADMINGROUP")));
        OidcUserService delegate = mock(OidcUserService.class);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(oidcUser("owner@example.com", false));

        assertThatThrownBy(() -> new AdminOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    private static OidcUser oidcUser(String email, boolean emailVerified) {
        OidcIdToken idToken = OidcIdToken.withTokenValue("token")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("sub", "123")
                .claim("email", email)
                .claim("email_verified", emailVerified)
                .build();
        return new DefaultOidcUser(java.util.List.of(), idToken);
    }
}
