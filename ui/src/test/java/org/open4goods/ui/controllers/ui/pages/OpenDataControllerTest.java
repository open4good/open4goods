package org.open4goods.ui.controllers.ui.pages;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.ui.config.properties.OpenDataRedirectProperties;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Legacy download URLs of {@link OpenDataController} (GOU-2290): 301 to R2 when configured, 404 otherwise, never a
 * file body.
 */
class OpenDataControllerTest {

    private OpenDataRedirectProperties properties;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        properties = new OpenDataRedirectProperties();
        mockMvc = MockMvcBuilders.standaloneSetup(new OpenDataController(properties)).build();
    }

    @Test
    void getAndHeadRedirectPermanentlyWhenTheBaseIsSet() throws Exception {
        properties.setRedirectBaseUrl("https://data.nudger.fr/");

        mockMvc.perform(get("/opendata/gtin-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/gtin-open-data.zip"))
                .andExpect(content().bytes(new byte[0]));
        mockMvc.perform(head("/opendata/gtin-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/gtin-open-data.zip"));
        mockMvc.perform(get("/opendata/isbn-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/isbn-open-data.zip"));
        mockMvc.perform(head("/opendata/isbn-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/isbn-open-data.zip"));
    }

    @Test
    void baseWithoutTrailingSlashGivesTheSameLocation() throws Exception {
        properties.setRedirectBaseUrl("https://data.nudger.fr");

        mockMvc.perform(get("/opendata/isbn-open-data.zip")).andExpect(status().isMovedPermanently())
                .andExpect(header().string("Location", "https://data.nudger.fr/isbn-open-data.zip"));
    }

    @Test
    void getAndHeadAnswer404WithoutABase() throws Exception {
        properties.setRedirectBaseUrl("");

        mockMvc.perform(get("/opendata/gtin-open-data.zip")).andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("Content-Disposition"))
                .andExpect(content().bytes(new byte[0]));
        mockMvc.perform(head("/opendata/isbn-open-data.zip")).andExpect(status().isNotFound())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void blankBaseCountsAsEmpty() throws Exception {
        properties.setRedirectBaseUrl("   ");

        mockMvc.perform(get("/opendata/gtin-open-data.zip")).andExpect(status().isNotFound());
    }
}
