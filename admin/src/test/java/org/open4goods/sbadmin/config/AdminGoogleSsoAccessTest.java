package org.open4goods.sbadmin.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.oidcLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ObservationAuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.authentication.OidcAuthorizationCodeAuthenticationProvider;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Shows that reusing the site's Google SSO client (GOOGLE_SSO_*, GOU-1091) opens the admin console to no extra
 * account: a Google login, whatever client issued it, only reaches the console through {@link AdminOidcUserService}
 * (verified email in {@code admin.security.google-sso.role-allowlist}), which is the only source of the role the
 * console requires for an interactive session.
 */
@SpringBootTest(properties = {
        "spring.security.oauth2.client.registration.google.client-id=site-sso-client-id",
        "spring.security.oauth2.client.registration.google.client-secret=site-sso-client-secret",
        "admin.security.google-sso.role-allowlist[owner@example.com][0]=ROLE_XWIKIADMINGROUP" })
class AdminGoogleSsoAccessTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private ClientRegistrationRepository clientRegistrations;

    @Autowired
    private FilterChainProxy filterChainProxy;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void googleLoginGoesThroughTheAllowlist() {
        // openid in the scopes: Spring Security takes the OIDC path, whose user service is the allowlist.
        assertThat(clientRegistrations.findByRegistrationId("google").getScopes()).contains("openid");
        assertThat(oidcUserServices()).singleElement().isInstanceOf(AdminOidcUserService.class);
        // No mapper can add a role to a Google identity behind the allowlist's back.
        assertThat(context.getBeanNamesForType(GrantedAuthoritiesMapper.class)).isEmpty();
    }

    @Test
    void aGoogleAccountOutsideTheAllowlistIsForbidden() throws Exception {
        // What a verified site user of the shared client holds without the allowlist: Google's own authorities only.
        mockMvc.perform(get("/applications").accept(MediaType.APPLICATION_JSON)
                        .with(oidcLogin().idToken(token -> token.claim("email", "site-user@example.com")
                                .claim("email_verified", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void theAllowlistedRoleOpensTheConsole() throws Exception {
        mockMvc.perform(get("/applications").accept(MediaType.APPLICATION_JSON)
                        .with(oidcLogin().idToken(token -> token.claim("email", "owner@example.com")
                                .claim("email_verified", true))
                                .authorities(new SimpleGrantedAuthority("ROLE_XWIKIADMINGROUP"))))
                .andExpect(status().isOk());
    }

    @Test
    void anAnonymousRequestIsRefused() throws Exception {
        mockMvc.perform(get("/applications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    /** The user services of the OIDC providers that the oauth2Login filters authenticate with. */
    private List<Object> oidcUserServices() {
        List<Object> services = new ArrayList<>();
        filterChainProxy.getFilterChains().stream()
                .flatMap(chain -> chain.getFilters().stream())
                .filter(OAuth2LoginAuthenticationFilter.class::isInstance)
                .map(filter -> (AuthenticationManager) ReflectionTestUtils.getField(filter, "authenticationManager"))
                .forEach(manager -> collectOidcUserServices(manager, services));
        return services;
    }

    private static void collectOidcUserServices(AuthenticationManager manager, List<Object> services) {
        if (manager instanceof ObservationAuthenticationManager) {
            collectOidcUserServices((AuthenticationManager) ReflectionTestUtils.getField(manager, "delegate"), services);
            return;
        }
        if (!(manager instanceof ProviderManager providerManager)) {
            return;
        }
        for (AuthenticationProvider provider : providerManager.getProviders()) {
            if (provider instanceof OidcAuthorizationCodeAuthenticationProvider) {
                services.add(ReflectionTestUtils.getField(provider, "userService"));
            }
        }
        collectOidcUserServices((AuthenticationManager) ReflectionTestUtils.getField(providerManager, "parent"),
                services);
    }
}
