package org.open4goods.b2bapi.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.dto.AuthResponse;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.service.AuthService;
import org.open4goods.b2bapi.service.AuthTokenResolver;
import org.open4goods.b2bapi.service.DashboardSessionService;
import org.open4goods.b2bapi.service.JwtCookieService;
import org.open4goods.b2bapi.service.ProvisionedAccount;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller tests for dashboard auth endpoints.
 */
class AuthControllerTest {

    private final AuthService authService = org.mockito.Mockito.mock(AuthService.class);
    private final DashboardSessionService dashboardSessionService = org.mockito.Mockito.mock(DashboardSessionService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        final B2bApiProperties properties = new B2bApiProperties();
        properties.getSecurity().setCookieDomain(".product-data-api.com");
        properties.getSecurity().setCookieSecure(true);
        final JwtCookieService cookieService = new JwtCookieService(properties);
        final AuthController controller = new AuthController(
                authService,
                dashboardSessionService,
                new AuthTokenResolver(),
                cookieService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void loginWritesSessionCookiesAndReturnsSessionPayload() throws Exception {
        when(authService.login(eq(OidcProvider.GOOGLE), eq("provider-token"))).thenReturn(authResponse());

        mockMvc.perform(post("/api/v1/auth/oidc")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginJson("GOOGLE", "provider-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.user.email").value("user@example.com"))
                .andExpect(jsonPath("$.organization.balanceCredits").value(2500))
                .andExpect(cookie().value(JwtCookieService.ACCESS_COOKIE, "access-token"))
                .andExpect(cookie().value(JwtCookieService.REFRESH_COOKIE, "refresh-token"));
    }

    @Test
    void refreshUsesRefreshCookieAndWritesNewCookies() throws Exception {
        when(dashboardSessionService.refresh("refresh-cookie")).thenReturn(authResponse());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .cookie(new Cookie(JwtCookieService.REFRESH_COOKIE, "refresh-cookie")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(cookie().value(JwtCookieService.ACCESS_COOKIE, "access-token"));

        verify(dashboardSessionService).refresh("refresh-cookie");
    }

    @Test
    void logoutClearsCookies() throws Exception {
        mockMvc.perform(post("/api/v1/auth/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge(JwtCookieService.ACCESS_COOKIE, 0))
                .andExpect(cookie().maxAge(JwtCookieService.REFRESH_COOKIE, 0));
    }

    @Test
    void meUsesBearerAccessTokenAndReturnsAccountWithoutTokenRotation() throws Exception {
        final User user = new User("user@example.com", OidcProvider.GOOGLE, "subject");
        user.setDisplayName("Test User");
        final Organization organization = new Organization("Test workspace", "test-workspace");
        final ProvisionedAccount account = new ProvisionedAccount(user, organization, OrganizationRole.OWNER, 2500L);
        when(dashboardSessionService.currentAccount("access-token")).thenReturn(account);
        when(dashboardSessionService.toAuthResponse(eq(account), org.mockito.ArgumentMatchers.any())).thenReturn(
                new AuthResponse(
                        null,
                        null,
                        null,
                        null,
                        new AuthResponse.AuthUserDto(user.getId(), user.getEmail(), user.getDisplayName(), null, false),
                        new AuthResponse.AuthOrganizationDto(organization.getId(), organization.getName(), organization.getSlug(), 2500L),
                        OrganizationRole.OWNER));

        mockMvc.perform(get("/api/v1/auth/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.user.displayName").value("Test User"))
                .andExpect(jsonPath("$.role").value("OWNER"));
    }

    private AuthResponse authResponse() {
        return new AuthResponse(
                "access-token",
                Instant.parse("2026-06-15T12:15:00Z"),
                "refresh-token",
                Instant.parse("2026-07-15T12:00:00Z"),
                new AuthResponse.AuthUserDto(java.util.UUID.randomUUID(), "user@example.com", "Test User", null, false),
                new AuthResponse.AuthOrganizationDto(java.util.UUID.randomUUID(), "Test workspace", "test-workspace", 2500L),
                OrganizationRole.OWNER);
    }

    private record LoginJson(String provider, String idToken) {
    }
}
