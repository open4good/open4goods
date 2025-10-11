package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryBreadcrumbDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategoryDto;
import org.open4goods.nudgerfrontapi.dto.category.GoogleCategorySummaryDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.GoogleCategoryNavigationService;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Controller tests covering the Google taxonomy navigation endpoints.
 */
@ExtendWith(MockitoExtension.class)
class GoogleCategoriesControllerTest {

    private MockMvc mockMvc;

    @Mock
    private GoogleCategoryNavigationService navigationService;

    @BeforeEach
    void setUp() {
        GoogleCategoriesController controller = new GoogleCategoriesController(navigationService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private GoogleCategoryDto sampleCategoryDto() {
        VerticalConfigDto vertical = new VerticalConfigDto(
                "tv",
                true,
                1234,
                5678,
                1,
                "/images/verticals/tv-small.jpg",
                "/images/verticals/tv-medium.jpg",
                "/images/verticals/tv-large.jpg",
                "Téléviseurs",
                "Description",
                "televiseurs");

        GoogleCategorySummaryDto child = new GoogleCategorySummaryDto(
                567,
                "Téléviseurs LED",
                Map.of("fr", "Téléviseurs LED"),
                "televiseurs-led",
                "televiseurs/televiseurs-led",
                List.of("televiseurs", "televiseurs-led"),
                false,
                true,
                true,
                true,
                vertical);

        GoogleCategoryBreadcrumbDto breadcrumb = new GoogleCategoryBreadcrumbDto(
                1234,
                "Téléviseurs",
                "televiseurs",
                "televiseurs");

        return new GoogleCategoryDto(
                1234,
                "Téléviseurs",
                Map.of("fr", "Téléviseurs"),
                "televiseurs",
                "televiseurs",
                List.of("televiseurs"),
                true,
                false,
                true,
                true,
                vertical,
                List.of(breadcrumb),
                List.of(child),
                List.of());
    }

    @Test
    void getCategoryByIdReturnsPayload() throws Exception {
        GoogleCategoryDto dto = sampleCategoryDto();
        given(navigationService.getCategoryById(eq(1234), any(DomainLanguage.class)))
                .willReturn(Optional.of(dto));

        mockMvc.perform(get("/categories/taxonomy/{taxonomyId}", 1234)
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$.taxonomyId").value(1234))
                .andExpect(jsonPath("$.children").isArray());
    }

    @Test
    void getCategoryByIdReturns404WhenMissing() throws Exception {
        given(navigationService.getCategoryById(eq(9999), any(DomainLanguage.class)))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/categories/taxonomy/{taxonomyId}", 9999)
                        .param("domainLanguage", "fr"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getCategoryByPathReturnsPayload() throws Exception {
        GoogleCategoryDto dto = sampleCategoryDto();
        given(navigationService.getCategoryByPath(eq("televiseurs"), any(DomainLanguage.class)))
                .willReturn(Optional.of(dto));

        mockMvc.perform(get("/categories/taxonomy/path/{categoryPath}", "televiseurs")
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.path").value("televiseurs"));
    }

    @Test
    void getChildrenByIdReturnsList() throws Exception {
        GoogleCategorySummaryDto child = sampleCategoryDto().children().get(0);
        given(navigationService.getChildrenById(eq(1234), any(DomainLanguage.class)))
                .willReturn(Optional.of(List.of(child)));

        mockMvc.perform(get("/categories/taxonomy/{taxonomyId}/children", 1234)
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].taxonomyId").value(child.taxonomyId()));
    }

    @Test
    void getChildrenByPathReturns404WhenCategoryMissing() throws Exception {
        given(navigationService.getChildrenByPath(eq("unknown"), any(DomainLanguage.class)))
                .willReturn(Optional.empty());

        mockMvc.perform(get("/categories/taxonomy/path/{categoryPath}/children", "unknown")
                        .param("domainLanguage", "fr"))
                .andExpect(status().isNotFound());
    }
}
