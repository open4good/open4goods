package org.open4goods.nudgerfrontapi.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.model.Localisable;
import org.open4goods.model.vertical.NudgeToolConfig;
import org.open4goods.model.vertical.NudgeToolSubsetGroup;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Unit tests for {@link AssistantConfigController}.
 */
@ExtendWith(MockitoExtension.class)
class AssistantConfigControllerTest {

    private MockMvc mockMvc;

    @Mock
    private VerticalsConfigService verticalsConfigService;

    @BeforeEach
    void setUp() {
        AssistantConfigController controller = new AssistantConfigController(verticalsConfigService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(new MappingJackson2HttpMessageConverter())
                .build();
    }

    /**
     * Verify list endpoint returns assistant configurations with localisation header.
     *
     * @throws Exception when the request fails
     */
    @Test
    void shouldListAssistantConfigs() throws Exception {
        NudgeToolConfig config = buildSampleConfig();
        Map<String, NudgeToolConfig> configs = new LinkedHashMap<>();
        configs.put("tv", config);
        when(verticalsConfigService.getAssistantConfigs()).thenReturn(configs);

        mockMvc.perform(get("/assistant-configs").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "fr"))
                .andExpect(jsonPath("$[0].id").value("tv"))
                .andExpect(jsonPath("$[0].config.subsetGroups[0].id").value("distance"));
    }

    /**
     * Verify a known assistant identifier returns the configuration.
     *
     * @throws Exception when the request fails
     */
    @Test
    void shouldGetAssistantConfig() throws Exception {
        NudgeToolConfig config = buildSampleConfig();
        when(verticalsConfigService.getAssistantConfigById("tv")).thenReturn(config);

        mockMvc.perform(get("/assistant-configs/tv").param("domainLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Locale", "en"))
                .andExpect(jsonPath("$.subsetGroups[0].id").value("distance"));
    }

    /**
     * Ensure unknown assistant identifiers return 404.
     *
     * @throws Exception when the request fails
     */
    @Test
    void shouldReturnNotFoundForUnknownAssistant() throws Exception {
        when(verticalsConfigService.getAssistantConfigById("unknown")).thenReturn(null);

        mockMvc.perform(get("/assistant-configs/unknown").param("domainLanguage", "fr"))
                .andExpect(status().isNotFound());
    }

    /**
     * Build a minimal assistant configuration for test scenarios.
     *
     * @return a sample nudge tool configuration
     */
    private NudgeToolConfig buildSampleConfig() {
        NudgeToolSubsetGroup group = new NudgeToolSubsetGroup();
        group.setId("distance");
        group.setMdiIcon("mdi-ruler");
        group.setLayout("grid");
        group.setTitle(localisable("fr", "À quelle distance ?", "en", "How far?"));

        VerticalSubset subset = new VerticalSubset();
        subset.setId("price_lower_500");
        subset.setGroup("price");
        subset.setTitle(localisable("fr", "< 500 €", "en", "< 500 €"));

        NudgeToolConfig config = new NudgeToolConfig();
        config.setSubsetGroups(List.of(group));
        config.setSubsets(List.of(subset));
        return config;
    }

    /**
     * Create a localisable payload with FR/EN entries.
     *
     * @param frKey key for the French value
     * @param frValue French label
     * @param enKey key for the English value
     * @param enValue English label
     * @return localisable map with both entries
     */
    private Localisable<String, String> localisable(String frKey, String frValue, String enKey, String enValue) {
        Localisable<String, String> localisable = new Localisable<>();
        localisable.put(frKey, frValue);
        localisable.put(enKey, enValue);
        return localisable;
    }
}
