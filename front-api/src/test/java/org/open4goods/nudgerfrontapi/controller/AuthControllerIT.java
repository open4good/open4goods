package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.auth.GoogleSsoLoginRequest;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentity;
import org.open4goods.nudgerfrontapi.service.auth.GoogleIdentityService;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.nudgerfrontapi.service.exception.GoogleIdentityVerificationException;
import org.open4goods.nudgerfrontapi.service.exception.GoogleSsoDisabledException;
import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;


import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "front.cache.path=${java.io.tmpdir}",
        "front.security.jwt-secret=0123456789ABCDEF0123456789ABCDEF"})
@AutoConfigureMockMvc

class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XWikiAuthenticationService authService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private GoogleIdentityService googleIdentityService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void loginReturnsCookies() throws Exception {
        given(authService.login("user", "pass")).willReturn(List.of("XWiki.XWikiUsers"));
        LoginRequest req = new LoginRequest("user", "pass");
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"))
                .andExpect(cookie().exists("refresh-token"));
    }

    @Test
    void refreshIssuesNewAccessToken() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "N/A");
        String refresh = jwtService.generateRefreshToken(auth);
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh-token", refresh))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"));
    }

    @Test
    void refreshPreservesRoles() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("owner@example.com", "N/A",
                List.of(new SimpleGrantedAuthority("ROLE_SITEEDITOR"), new SimpleGrantedAuthority("XWIKIADMINGROUP")));
        String refresh = jwtService.generateRefreshToken(auth);

        var result = mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh-token", refresh))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andReturn();

        String accessCookie = result.getResponse().getCookie("access-token").getValue();
        List<String> roles = jwtDecoder.decode(accessCookie).getClaimAsStringList("roles");
        assertThat(roles).containsExactlyInAnyOrder("ROLE_SITEEDITOR", "XWIKIADMINGROUP");
    }

    @Test
    void googleLoginReturnsCookiesWhenIdentityVerified() throws Exception {
        given(googleIdentityService.verify("valid-id-token", "expected-nonce"))
                .willReturn(new GoogleIdentity("owner@example.com", List.of("ROLE_SITEEDITOR")));
        GoogleSsoLoginRequest req = new GoogleSsoLoginRequest("valid-id-token", "expected-nonce");

        var result = mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"))
                .andExpect(cookie().exists("refresh-token"))
                .andReturn();
        assertSecureAndSameSite(result.getResponse().getHeaders("Set-Cookie"));
    }

    @Test
    void googleLoginRejectsUnverifiedOrUnallowlistedIdentity() throws Exception {
        given(googleIdentityService.verify("tampered-token", "n"))
                .willThrow(new GoogleIdentityVerificationException("Google ID token verification failed"));
        GoogleSsoLoginRequest req = new GoogleSsoLoginRequest("tampered-token", "n");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void googleLoginReturnsNotFoundWhenFlagDisabled() throws Exception {
        given(googleIdentityService.verify("any-token", "n"))
                .willThrow(new GoogleSsoDisabledException());
        GoogleSsoLoginRequest req = new GoogleSsoLoginRequest("any-token", "n");

        mockMvc.perform(post("/auth/google")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsBytes(req))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isNotFound());
    }

    @Test
    void logoutClearsAuthCookies() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("access-token", "access"),
                                new jakarta.servlet.http.Cookie("refresh-token", "refresh"))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access-token", ""))
                .andExpect(cookie().maxAge("access-token", 0))
                .andExpect(cookie().value("refresh-token", ""))
                .andExpect(cookie().maxAge("refresh-token", 0));
    }
}
