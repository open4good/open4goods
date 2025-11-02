package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.localization.DomainLanguage;
import org.open4goods.nudgerfrontapi.service.SearchService;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchHit;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchResult;
import org.open4goods.nudgerfrontapi.service.SearchService.GlobalSearchVerticalGroup;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link SearchController}.
 */
class SearchControllerTest {

    private SearchService searchService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        searchService = mock(SearchService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new SearchController(searchService))
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    @Test
    void globalSearchReturnsGroupedPayload() throws Exception {
        GlobalSearchHit hit = new GlobalSearchHit(1L, "phones", "Fairphone 4", "Fairphone", "Fairphone 4", 5, 7.2d);
        GlobalSearchVerticalGroup group = new GlobalSearchVerticalGroup("phones", List.of(hit));
        GlobalSearchResult serviceResult = new GlobalSearchResult(List.of(group), List.of(), false);
        when(searchService.globalSearch("fairphone", DomainLanguage.fr)).thenReturn(serviceResult);

        mockMvc.perform(get("/search/global")
                        .param("query", "fairphone")
                        .param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$.verticalGroups[0].verticalId").value("phones"))
                .andExpect(jsonPath("$.verticalGroups[0].results[0].gtin").value(1))
                .andExpect(jsonPath("$.fallbackTriggered").value(false));
    }

    @Test
    void globalSearchIncludesFallbackResults() throws Exception {
        GlobalSearchHit fallbackHit = new GlobalSearchHit(2L, null, "Universal Charger", null, null, 2, 3.5d);
        GlobalSearchResult serviceResult = new GlobalSearchResult(List.of(), List.of(fallbackHit), true);
        when(searchService.globalSearch("chargeur", DomainLanguage.en)).thenReturn(serviceResult);

        mockMvc.perform(get("/search/global")
                        .param("query", "chargeur")
                        .param("domainLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "en"))
                .andExpect(jsonPath("$.verticalGroups").isEmpty())
                .andExpect(jsonPath("$.fallbackResults[0].title").value("Universal Charger"))
                .andExpect(jsonPath("$.fallbackTriggered").value(true));
    }
}
