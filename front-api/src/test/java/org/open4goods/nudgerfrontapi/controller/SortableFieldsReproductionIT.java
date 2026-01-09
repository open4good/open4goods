package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.TestTextEmbeddingConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
@Import(TestTextEmbeddingConfig.class)
class SortableFieldsReproductionIT {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private VerticalsConfigService verticalsConfigService;

    private static final String SHARED_TOKEN = "test-token";

    @Test
    void sortableFieldsForVerticalReturnsOk() throws Exception {
        VerticalConfig config = new VerticalConfig();
        config.setId("oven");
        given(verticalsConfigService.getConfigById("oven")).willReturn(config);

        // This is expected to FAIL before the fix
        mockMvc.perform(get("/products/fields/sortable/oven")
                        .param("domainLanguage", "fr")
                        .header("X-Shared-Token", SHARED_TOKEN)
                        .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_FRONTEND)))))
                .andExpect(status().isOk());
    }
}
