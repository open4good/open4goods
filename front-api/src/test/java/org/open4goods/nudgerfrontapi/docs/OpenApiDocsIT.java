package org.open4goods.nudgerfrontapi.docs;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.mockito.BDDMockito.given;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import org.open4goods.xwiki.services.XWikiAuthenticationService;

@SpringBootTest(properties = "front.cache.path=${java.io.tmpdir}")
@AutoConfigureMockMvc
class OpenApiDocsIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XWikiAuthenticationService authService;

    @Test
    void unauthenticatedRequestIsRejected() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void apiDocsAccessibleWithBasicAuth() throws Exception {
        given(authService.login("user", "pass")).willReturn(List.of("XWiki.XWikiUsers"));
        mockMvc.perform(get("/v3/api-docs").with(httpBasic("user", "pass")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists());
    }
}
