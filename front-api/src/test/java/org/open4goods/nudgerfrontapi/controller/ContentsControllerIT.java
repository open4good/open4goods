package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
class ContentsControllerIT {

    private static final String SHARED_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private XwikiFacadeService xwikiFacadeService;

    @MockBean
    private XWikiHtmlService xwikiHtmlService;

    @Test
    void blocEndpointUsesRequestedLanguageWhenAvailable() throws Exception {
        given(xwikiFacadeService.getLocalizedBloc(eq("Main"), eq("fr"), any(Locale.class)))
                .willReturn(new XwikiFacadeService.LocalizedHtml("<p>traduction</p>", "fr"));
        given(xwikiHtmlService.getEditPageUrl("Main")).willReturn("https://wiki/edit/Main");

        mockMvc.perform(get("/blocs/{blocId}", "Main")
                        .queryParam("lang", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.htmlContent").value("<p>traduction</p>"))
                .andExpect(jsonPath("$.resolvedLanguage").value("fr"));
    }

    @Test
    void blocEndpointFallsBackToDefaultWhenLangMissing() throws Exception {
        given(xwikiFacadeService.getLocalizedBloc(eq("Main"), isNull(), any(Locale.class)))
                .willReturn(new XwikiFacadeService.LocalizedHtml("<p>default</p>", "default"));
        given(xwikiHtmlService.getEditPageUrl("Main")).willReturn("https://wiki/edit/Main");

        mockMvc.perform(get("/blocs/{blocId}", "Main")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.htmlContent").value("<p>default</p>"))
                .andExpect(jsonPath("$.resolvedLanguage").value("default"));
    }
}
