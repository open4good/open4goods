package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.api.StatsController;
import org.open4goods.nudgerfrontapi.dto.stats.CategoriesStatsDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.open4goods.nudgerfrontapi.config.TestTextEmbeddingConfig;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
@Import(TestTextEmbeddingConfig.class)
class StatsControllerIT {

    private static final String SHARED_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StatsController controller;

    @MockBean
    private StatsService statsService;



    @Test
    void categoriesEndpointReturns403WhenMissingSharedToken() throws Exception {
        mockMvc.perform(get("/stats/categories")
                .param("domainLanguage", "FR")
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.title").value("Forbidden"))
                .andExpect(jsonPath("$.status").value(403));
    }
}
