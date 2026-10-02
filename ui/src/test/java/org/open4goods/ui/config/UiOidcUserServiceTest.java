package org.open4goods.ui.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.ui.config.properties.UiGoogleSsoProperties;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

class UiOidcUserServiceTest {

    @Test
    void grantsTheAllowlistedRoleForAVerifiedEmail() {
        UiGoogleSsoProperties properties = new UiGoogleSsoProperties();
        properties.setRoleAllowlist(Map.of("owner@example.com", List.of("ROLE_XWIKIADMINGROUP")));

        OidcUserService delegate = mock(OidcUserService.class);
        OidcUser verifiedUser = oidcUser("owner@example.com", true);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(verifiedUser);

        OidcUser result = new UiOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class));

        assertThat(result.getAuthorities())
                .extracting(a -> a.getAuthority())
                .contains("ROLE_XWIKIADMINGROUP");
    }

    @Test
    void refusesAnEmailAbsentFromTheAllowlist() {
        UiGoogleSsoProperties properties = new UiGoogleSsoProperties();
        OidcUserService delegate = mock(OidcUserService.class);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(oidcUser("stranger@example.com", true));

        assertThatThrownBy(() -> new UiOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void refusesAnUnverifiedEmailEvenIfAllowlisted() {
        UiGoogleSsoProperties properties = new UiGoogleSsoProperties();
        properties.setRoleAllowlist(Map.of("owner@example.com", List.of("ROLE_XWIKIADMINGROUP")));
        OidcUserService delegate = mock(OidcUserService.class);
        given(delegate.loadUser(org.mockito.ArgumentMatchers.any())).willReturn(oidcUser("owner@example.com", false));

        assertThatThrownBy(() -> new UiOidcUserService(properties, delegate).loadUser(mock(OidcUserRequest.class)))
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
        return new DefaultOidcUser(List.of(), idToken);
    }
}
