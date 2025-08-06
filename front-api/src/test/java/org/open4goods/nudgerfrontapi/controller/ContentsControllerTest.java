package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashMap;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.api.ContentsController;
import org.open4goods.xwiki.model.FullPage;
import org.open4goods.xwiki.services.XWikiHtmlService;
import org.open4goods.xwiki.services.XwikiFacadeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"front.security.enabled=true", "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
class ContentsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ContentsController controller;

    @MockBean
    private XWikiHtmlService xwikiHtmlService;

    @MockBean
    private XwikiFacadeService xwikiFacadeService;

    @Test
    void pagesEndpointReturnsList() throws Exception {
        mockMvc.perform(get("/pages").with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void pageEndpointReturnsDto() throws Exception {
        var fp = new FullPage();
        fp.setHtmlContent("<p>Hi</p>");
        var props = new HashMap<String, String>();
        props.put("metaTitle", "title");
        props.put("metaDescription", "desc");
        props.put("pageTitle", "page");
        props.put("width", "full");
        fp.setProperties(props);
        given(xwikiFacadeService.getFullPage(anyString())).willReturn(fp);
        given(xwikiHtmlService.getEditPageUrl(anyString())).willReturn("/edit");

        mockMvc.perform(get("/pages/{id}", "Main.WebHome")
                        .header("X-Shared-Token", "test-token")
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metaTitle").value("title"))
                .andExpect(jsonPath("$.editLink").value("/edit"));
    }
}
