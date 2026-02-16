package org.open4goods.nudgerfrontapi.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.RolesConstants;
import org.open4goods.nudgerfrontapi.controller.api.StatsController;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizChartPresetDto;
import org.open4goods.nudgerfrontapi.dto.stats.DatavizDefaultFilterDto;
import org.open4goods.nudgerfrontapi.dto.stats.VerticalDatavizPlanDto;
import org.open4goods.nudgerfrontapi.service.DatavizStatsService;
import org.open4goods.nudgerfrontapi.service.StatsService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link StatsController}.
 */
@ExtendWith(MockitoExtension.class)
class StatsControllerIT {

    private MockMvc mockMvc;

    @Mock
    private StatsService statsService;

    @Mock
    private DatavizStatsService datavizStatsService;

    /**
     * Build a standalone MockMvc instance with mocked dependencies.
     */
    @BeforeEach
    void setUp() {
        StatsController controller = new StatsController(statsService, datavizStatsService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    /**
     * Verify known verticals return the dataviz plan and one-hour cache headers.
     *
     * @throws Exception when the request fails
     */
    @Test
    void datavizPlanReturnsOneHourCacheForKnownVertical() throws Exception {
        VerticalDatavizPlanDto dto = new VerticalDatavizPlanDto(
                "televisions",
                List.of(new DatavizDefaultFilterDto("lastChange", "range", null, null, "now-2d")),
                List.of(new DatavizChartPresetDto("products-by-brand", "bar", "Produits par marque", "Top marques", "productsByBrand", RolesConstants.ROLE_FRONTEND, true)));
        given(datavizStatsService.getVerticalPlan("televisions", org.open4goods.nudgerfrontapi.localization.DomainLanguage.fr))
                .willReturn(dto);

        mockMvc.perform(get("/stats/verticals/televisions/dataviz/plan")
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "max-age=3600, public"))
                .andExpect(jsonPath("$.verticalId").value("televisions"))
                .andExpect(jsonPath("$.defaultFilters[0].minRelative").value("now-2d"))
                .andExpect(jsonPath("$.charts[0].id").value("products-by-brand"));
    }

    /**
     * Ensure unknown verticals are translated to HTTP 404.
     *
     * @throws Exception when the request fails
     */
    @Test
    void datavizPlanReturns404WhenVerticalIsUnknown() throws Exception {
        given(datavizStatsService.getVerticalPlan("unknown", org.open4goods.nudgerfrontapi.localization.DomainLanguage.en))
                .willReturn(null);

        mockMvc.perform(get("/stats/verticals/unknown/dataviz/plan")
                        .param("domainLanguage", "en"))
                .andExpect(status().isNotFound());
    }
}
