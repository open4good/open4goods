package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.api.dto.DatasourceCoverageDto;
import org.open4goods.api.dto.LeakageWarningDto;
import org.open4goods.api.dto.SignificantCategoryDto;
import org.open4goods.api.dto.UnmappedCategoryDto;
import org.open4goods.api.services.VerticalsGenerationService;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Contract tests for vertical generation and datasource maintenance endpoints.
 */
@ExtendWith(MockitoExtension.class)
class VerticalsGenerationControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VerticalsGenerationService verticalsGenerationService;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @BeforeEach
    void setUp() {
        VerticalsGenerationController controller = new VerticalsGenerationController(
                verticalsGenerationService, verticalsConfigService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void datasourceCoverageReturnsRows() throws Exception {
        VerticalConfig vertical = vertical("oven");
        when(verticalsConfigService.getConfigById("oven")).thenReturn(vertical);
        when(verticalsGenerationService.datasourceCoverage(vertical, 50)).thenReturn(List.of(
                new DatasourceCoverageDto("Darty FR", 110L, 5, 1, List.of("UNMAPPED"))));

        mockMvc.perform(get("/verticals/oven/datasources/stats/coverage").param("minVolume", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].datasource").value("Darty FR"))
                .andExpect(jsonPath("$[0].products").value(110));
    }

    @Test
    void unmappedCategoriesReturnsRows() throws Exception {
        VerticalConfig vertical = vertical("oven");
        when(verticalsConfigService.getConfigById("oven")).thenReturn(vertical);
        when(verticalsGenerationService.unmappedCategories(vertical, 50, 200)).thenReturn(List.of(
                new UnmappedCategoryDto(null, "FOUR", 110L)));

        mockMvc.perform(get("/verticals/oven/datasources/stats/unmapped"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOUR"))
                .andExpect(jsonPath("$[0].volume").value(110));
    }

    @Test
    void leakageReturnsRows() throws Exception {
        when(verticalsConfigService.getConfigById("oven")).thenReturn(vertical("oven"));
        when(verticalsGenerationService.categoryLeakage("oven", 50, 0.2)).thenReturn(List.of(
                new LeakageWarningDto("FOUR", 100L, "oven", 0.8, "cooktop", 0.2, true)));

        mockMvc.perform(get("/verticals/oven/datasources/stats/leakage"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOUR"))
                .andExpect(jsonPath("$[0].flagged").value(true));
    }

    @Test
    void significantReturnsRows() throws Exception {
        when(verticalsConfigService.getConfigById("oven")).thenReturn(vertical("oven"));
        when(verticalsGenerationService.significantCategories("oven", 50, 50)).thenReturn(List.of(
                new SignificantCategoryDto("FOUR", 42.5, 110L, 12L)));

        mockMvc.perform(get("/verticals/oven/datasources/stats/significant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("FOUR"))
                .andExpect(jsonPath("$[0].score").value(42.5));
    }

    @Test
    void statsEndpointReturns404WhenVerticalDoesNotExist() throws Exception {
        when(verticalsConfigService.getConfigById("missing")).thenReturn(null);

        mockMvc.perform(get("/verticals/missing/datasources/stats/coverage"))
                .andExpect(status().isNotFound());
    }

    private VerticalConfig vertical(String id) {
        VerticalConfig vertical = new VerticalConfig();
        vertical.setId(id);
        vertical.setMatchingCategories(Map.of("Darty FR", Set.of("FOUR")));
        return vertical;
    }
}
