package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.ApiGoogleSsoProperties;
import org.open4goods.model.RolesConstants;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import jakarta.servlet.ServletException;

/** Tests the deny-by-default Google allowlist of api and the answer to a refused login. */
class ApiOidcUserServiceTest {

    @Test
    void grantsTheAllowlistedAuthoritiesForAVerifiedEmail() {
        OidcUser result = service(allowlist(), oidcUser("Owner@Example.com", true)).loadUser(mock(OidcUserRequest.class));

        assertThat(result.getAuthorities()).extracting(GrantedAuthority::getAuthority)
                .contains(RolesConstants.ROLE_USER, RolesConstants.ROLE_ADMIN);
    }

    @Test
    void refusesAnEmailAbsentFromTheAllowlist() {
        assertThatThrownBy(() -> service(allowlist(), oidcUser("stranger@example.com", true))
                .loadUser(mock(OidcUserRequest.class)))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> assertThat(
                        exception.getError().getErrorCode()).isEqualTo(ApiOidcUserService.EMAIL_NOT_ALLOWLISTED));
    }

    @Test
    void refusesAnUnverifiedEmailEvenIfAllowlisted() {
        assertThatThrownBy(() -> service(allowlist(), oidcUser("owner@example.com", false))
                .loadUser(mock(OidcUserRequest.class)))
                .isInstanceOfSatisfying(OAuth2AuthenticationException.class, exception -> assertThat(
                        exception.getError().getErrorCode()).isEqualTo(ApiOidcUserService.EMAIL_NOT_VERIFIED));
    }

    @Test
    void refusesEveryoneWithAnEmptyAllowlist() {
        assertThatThrownBy(() -> service(new ApiGoogleSsoProperties(), oidcUser("owner@example.com", true))
                .loadUser(mock(OidcUserRequest.class)))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    void aRefusedGoogleIdentityGetsA403() throws IOException, ServletException {
        for (String code : ApiOidcUserService.REFUSAL_CODES) {
            MockHttpServletResponse response = new MockHttpServletResponse();

            WebSecurityConfig.loginFailureHandler().onAuthenticationFailure(new MockHttpServletRequest(), response,
                    new OAuth2AuthenticationException(new OAuth2Error(code)));

            assertThat(response.getStatus()).as(code).isEqualTo(403);
            assertThat(response.getRedirectedUrl()).as(code).isNull();
        }
    }

    @Test
    void anotherLoginErrorGetsA401WithoutRedirect() throws IOException, ServletException {
        MockHttpServletResponse response = new MockHttpServletResponse();

        WebSecurityConfig.loginFailureHandler().onAuthenticationFailure(new MockHttpServletRequest(), response,
                new OAuth2AuthenticationException(new OAuth2Error("invalid_state_parameter")));

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getRedirectedUrl()).isNull();
    }

    private static ApiGoogleSsoProperties allowlist() {
        ApiGoogleSsoProperties properties = new ApiGoogleSsoProperties();
        properties.setRoleAllowlist(Map.of("owner@example.com", List.of(RolesConstants.ROLE_USER, RolesConstants.ROLE_ADMIN)));
        return properties;
    }

    private static ApiOidcUserService service(ApiGoogleSsoProperties properties, OidcUser googleUser) {
        OidcUserService delegate = mock(OidcUserService.class);
        given(delegate.loadUser(any())).willReturn(googleUser);
        return new ApiOidcUserService(properties, delegate);
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
