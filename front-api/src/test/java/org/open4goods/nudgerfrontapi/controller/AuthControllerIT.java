package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.auth.LoginRequest;
import org.open4goods.nudgerfrontapi.service.auth.JwtService;
import org.open4goods.xwiki.services.XWikiAuthenticationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "front.cache.path=${java.io.tmpdir}",
        "front.security.jwt-secret=testsecret"})
@AutoConfigureMockMvc
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
                        .content(mapper.writeValueAsBytes(req)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"))
                .andExpect(cookie().exists("refresh-token"));
    }

    @Test
    void refreshIssuesNewAccessToken() throws Exception {
        var auth = new UsernamePasswordAuthenticationToken("user", "N/A");
        String refresh = jwtService.generateRefreshToken(auth);
        mockMvc.perform(post("/auth/refresh")
                        .cookie(new jakarta.servlet.http.Cookie("refresh-token", refresh)))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("access-token"));
    }
}
