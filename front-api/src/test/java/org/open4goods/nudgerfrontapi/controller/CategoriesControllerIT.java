package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.open4goods.commons.services.VerticalsConfigService;
import org.open4goods.model.RolesConstants;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.BarcodeAggregationProperties;
import org.open4goods.model.vertical.DescriptionsAggregationConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.RecommandationsConfig;
import org.open4goods.model.vertical.ResourcesAggregationConfig;
import org.open4goods.model.vertical.ScoringAggregationConfig;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.nudgerfrontapi.controller.api.CategoriesController;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.nudgerfrontapi.service.CategoryMappingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = {"front.cache.path=${java.io.tmpdir}",
        "front.security.enabled=true",
        "front.security.shared-token=test-token"})
@AutoConfigureMockMvc
class CategoriesControllerIT {

    private static final String SHARED_TOKEN = "test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriesController controller;

    @MockBean
    private VerticalsConfigService verticalsConfigService;

    @MockBean
    private CategoryMappingService categoryMappingService;

    @Test
    void listCategoriesReturnsDtos() throws Exception {
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("tv");

        given(verticalsConfigService.getConfigsWithoutDefault(true)).willReturn(List.of(verticalConfig));

        VerticalConfigDto dto = new VerticalConfigDto("tv", true, 404, 1584, 1,
                null, null, null, null, Map.of());
        given(categoryMappingService.toVerticalConfigDto(verticalConfig)).willReturn(dto);

        mockMvc.perform(get("/category")
                .param("domainLanguage", "fr")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("tv"));
    }

    @Test
    void categoryDetailsReturnDto() throws Exception {
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setId("tv");

        given(verticalsConfigService.getConfigById("tv")).willReturn(verticalConfig);

        VerticalConfigFullDto fullDto = new VerticalConfigFullDto(
                "tv",
                true,
                404,
                1584,
                1,
                null,
                null,
                null,
                null,
                Map.of(),
                Map.of(),
                List.of(),
                List.of(),
                List.of(),
                Map.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                Set.of(),
                false,
                Map.of(),
                Set.of(),
                new SiteNaming(),
                new ResourcesAggregationConfig(),
                new AttributesConfig(),
                Map.of(),
                new ImpactScoreConfig(),
                List.of(),
                new VerticalSubset(),
                new BarcodeAggregationProperties(),
                new RecommandationsConfig(),
                new DescriptionsAggregationConfig(),
                new ScoringAggregationConfig(),
                List.of(),
                0,
                0);
        given(categoryMappingService.toVerticalConfigFullDto(verticalConfig)).willReturn(fullDto);

        mockMvc.perform(get("/category/tv")
                .param("domainLanguage", "fr")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("tv"));
    }

    @Test
    void categoryReturns404WhenMissing() throws Exception {
        given(verticalsConfigService.getConfigById("missing")).willReturn(null);

        mockMvc.perform(get("/category/missing")
                .param("domainLanguage", "fr")
                .header("X-Shared-Token", SHARED_TOKEN)
                .with(jwt().jwt(jwt -> jwt.claim("roles", List.of(RolesConstants.ROLE_XWIKI_ALL)))))
                .andExpect(status().isNotFound());
    }
}
