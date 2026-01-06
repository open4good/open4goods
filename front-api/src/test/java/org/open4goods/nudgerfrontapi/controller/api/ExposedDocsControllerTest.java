package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsCategoryDto;
import org.open4goods.nudgerfrontapi.dto.exposed.ExposedDocsOverviewDto;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.exposed.ExposedDocsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for the exposed docs controller.
 */
public class ExposedDocsControllerTest
{

    private MockMvc mockMvc;
    private ExposedDocsService exposedDocsService;

    @BeforeEach
    void setUp() throws Exception
    {
        exposedDocsService = Mockito.mock(ExposedDocsService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ExposedDocsController(exposedDocsService)).build();
    }

    /**
     * Verifies that the overview endpoint responds successfully.
     *
     * @throws Exception when the request fails
     */
    @Test
    void shouldReturnOverview() throws Exception
    {
        when(exposedDocsService.getOverview(DomainLanguage.fr))
                .thenReturn(new ExposedDocsOverviewDto(List.of(
                        new ExposedDocsCategoryDto("docs", "Documentation", "/exposed/docs", "docs", List.of("md"), 1))));

        mockMvc.perform(get("/exposed")
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk());
    }
}
