package org.open4goods.nudgerfrontapi.controller.api;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.service.OpenDataMappingService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestService;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot;
import org.open4goods.nudgerfrontapi.service.opendata.OpenDataManifestSnapshot.State;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * {@code /opendata/*} answers 200 with null manifest values when the manifest is unreadable (GOU-2290), and the
 * overview no longer carries {@code downloadLimits}.
 */
class OpenDataControllerTest {

    private OpenDataManifestService manifestService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        manifestService = mock(OpenDataManifestService.class);
        OpenDataConfig openDataConfig = new OpenDataConfig();
        openDataConfig.setPublicBaseUrl("https://data.nudger.fr");
        OpenDataMappingService mappingService = new OpenDataMappingService(manifestService, openDataConfig,
                mock(org.open4goods.nudgerfrontapi.service.opendata.OpenDataSampleService.class));
        mockMvc = MockMvcBuilders.standaloneSetup(new OpenDataController(mappingService)).build();
    }

    @Test
    void unreadableManifestGives200WithNulls() throws Exception {
        given(manifestService.snapshot()).willReturn(
                new OpenDataManifestSnapshot(State.UNAVAILABLE, null, null, "HTTP 404"));

        mockMvc.perform(get("/opendata").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProductCount").value(nullValue()))
                .andExpect(jsonPath("$.totalDatasetSize").value(nullValue()))
                .andExpect(jsonPath("$.downloadLimits").doesNotExist());

        mockMvc.perform(get("/opendata/gtin").param("domainLanguage", "fr"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GTIN"))
                .andExpect(jsonPath("$.recordCount").value(nullValue()))
                .andExpect(jsonPath("$.recordCountValue").value(nullValue()))
                .andExpect(jsonPath("$.lastUpdated").value(nullValue()))
                .andExpect(jsonPath("$.sha256").value(nullValue()))
                .andExpect(jsonPath("$.downloadUrl").value("https://data.nudger.fr/gtin-open-data.zip"));
    }

    @Test
    void notConfiguredGives200() throws Exception {
        given(manifestService.snapshot()).willReturn(OpenDataManifestSnapshot.NOT_CONFIGURED);

        mockMvc.perform(get("/opendata/isbn").param("domainLanguage", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ISBN"))
                .andExpect(jsonPath("$.fileSize").value(nullValue()));
    }
}
