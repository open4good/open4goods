package org.open4goods.ui.controllers.ui.pages;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.File;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.services.opendata.config.OpenDataConfig;
import org.open4goods.services.opendata.service.OpenDataService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Download endpoints of {@link OpenDataController}: 301 to R2 when configured, no throttled stream on HEAD.
 */
class OpenDataControllerTest {

    private OpenDataService openDataService;
    private OpenDataConfig openDataConfig;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        openDataService = mock(OpenDataService.class);
        openDataConfig = mock(OpenDataConfig.class);
        when(openDataConfig.gtinZipFile()).thenReturn(new File("missing-gtin.zip"));
        when(openDataConfig.isbnZipFile()).thenReturn(new File("missing-isbn.zip"));
        mockMvc = MockMvcBuilders.standaloneSetup(new OpenDataController(openDataService, openDataConfig)).build();
    }

    @Test
    void redirectsGetAndHeadWhenFlagIsSet() throws Exception {
        when(openDataConfig.getRedirectBaseUrl()).thenReturn("https://data.nudger.fr/");

        mockMvc.perform(get("/opendata/gtin-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/gtin-open-data.zip"));
        mockMvc.perform(head("/opendata/isbn-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/isbn-open-data.zip"));

        verifyNoInteractions(openDataService);
    }

    @Test
    void headNeverStartsTheThrottledStreamWhenFlagIsEmpty() throws Exception {
        when(openDataConfig.getRedirectBaseUrl()).thenReturn("");

        mockMvc.perform(head("/opendata/gtin-open-data.zip")).andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"gtin-open-data.zip\""));

        verifyNoInteractions(openDataService);
    }

    @Test
    void getStreamsLocallyWhenFlagIsEmpty() throws Exception {
        when(openDataConfig.getRedirectBaseUrl()).thenReturn("");
        when(openDataService.limitedRateStream(new File("missing-gtin.zip").getAbsolutePath()))
                .thenReturn(new java.io.ByteArrayInputStream(new byte[] { 1, 2, 3 }));

        mockMvc.perform(get("/opendata/gtin-open-data.zip")).andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));
    }
}
