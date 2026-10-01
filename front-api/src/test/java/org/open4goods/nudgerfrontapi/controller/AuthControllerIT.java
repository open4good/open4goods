package org.open4goods.nudgerfrontapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.kohsuke.github.GHRepository;
import org.open4goods.brand.service.BrandService;
import org.open4goods.icecat.repository.IcecatCategoryRepository;
import org.open4goods.icecat.repository.IcecatFeatureGroupRepository;
import org.open4goods.icecat.repository.IcecatFeatureRepository;
import org.open4goods.icecat.repository.IcecatSupplierRepository;
import org.open4goods.services.contribution.repository.ContributionVoteRepository;
import org.open4goods.services.geocode.service.IpGeolocationService;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;


import tools.jackson.databind.ObjectMapper;

// The jwt-secret below is a deliberately low-entropy placeholder: SecurityProperties
// enforces @Size(min = 33), and a random-looking literal of that length is flagged by
// gitleaks' generic-api-key rule. Keep it word-shaped and keep it at least 33 characters.
@SpringBootTest(properties = {
        "front.cache.path=${java.io.tmpdir}",
        "front.security.jwt-secret=front-api-test-placeholder-jwt-signing-value"})
@AutoConfigureMockMvc

class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private XWikiAuthenticationService authService;

    @MockitoBean
    private GHRepository ghRepository;

    @MockitoBean
    private BrandService brandService;

    @MockitoBean
    private IpGeolocationService ipGeolocationService;

    @MockitoBean
    private IcecatFeatureRepository icecatFeatureRepository;

    @MockitoBean
    private IcecatCategoryRepository icecatCategoryRepository;

    @MockitoBean
    private IcecatFeatureGroupRepository icecatFeatureGroupRepository;

    @MockitoBean
    private IcecatSupplierRepository icecatSupplierRepository;

    @MockitoBean
    private ContributionVoteRepository contributionVoteRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void loginReturnsCookies() throws Exception {
        given(authService.login("user", "pass")).willReturn(List.of("XWiki.XWikiUsers"));
        LoginRequest req = new LoginRequest("user", "pass");
        var result = mockMvc.perform(post("/auth/login")
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
    void refreshIssuesNewAccessToken() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "N/A");
        String refresh = jwtService.generateRefreshToken(auth);
        var result = mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh-token", refresh))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"))
                .andReturn();
        assertSecureAndSameSite(result.getResponse().getHeaders("Set-Cookie"));
    }

    @Test
    void logoutClearsAuthCookies() throws Exception {
        var result = mockMvc.perform(post("/auth/logout")
                        .cookie(new jakarta.servlet.http.Cookie("access-token", "access"),
                                new jakarta.servlet.http.Cookie("refresh-token", "refresh"))
                        .param("domainLanguage", "FR"))
                .andExpect(status().isOk())
                .andExpect(cookie().value("access-token", ""))
                .andExpect(cookie().maxAge("access-token", 0))
                .andExpect(cookie().value("refresh-token", ""))
                .andExpect(cookie().maxAge("refresh-token", 0))
                .andReturn();
        assertSecureAndSameSite(result.getResponse().getHeaders("Set-Cookie"));
    }

    private static void assertSecureAndSameSite(List<String> setCookieHeaders) {
        assertThat(setCookieHeaders).isNotEmpty();
        assertThat(setCookieHeaders).allSatisfy(header -> {
            assertThat(header).containsIgnoringCase("Secure");
            assertThat(header).containsIgnoringCase("SameSite=Lax");
        });
    }
}
