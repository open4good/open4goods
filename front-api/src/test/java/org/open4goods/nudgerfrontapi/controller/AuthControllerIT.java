package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.open4goods.nudgerfrontapi.config.TestTextEmbeddingConfig;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "front.cache.path=${java.io.tmpdir}",
        "front.security.jwt-secret=0123456789ABCDEF0123456789ABCDEF"})
@AutoConfigureMockMvc
@Import(TestTextEmbeddingConfig.class)
class AuthControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XWikiAuthenticationService authService;

    @Autowired
    private JwtService jwtService;

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
