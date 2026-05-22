package org.open4goods.api.controller.api;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Contract tests for {@link ReferentialHelperController}.
 */
@ExtendWith(MockitoExtension.class)
class ReferentialHelperControllerTest
{
    private MockMvc mockMvc;

    @Mock
    private VerticalsConfigService verticalsService;

    @Mock
    private GoogleTaxonomyService googleTaxonomyService;

    @Mock
    private WikidataSearchService wikidataSearchService;

    @Mock
    private IcecatIndexService icecatIndexService;

    @BeforeEach
    void setUp()
    {
        ReferentialHelperController controller = new ReferentialHelperController(
                verticalsService, googleTaxonomyService, wikidataSearchService, icecatIndexService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void googleCandidatesReturns404WhenVerticalNotFound()
            throws Exception
    {
        when(verticalsService.getConfigById("unknown")).thenReturn(null);
        mockMvc.perform(get("/api/referentials/google/candidates").param("vertical", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void googleCandidatesReturnsOkForKnownVertical()
            throws Exception
    {
        VerticalConfig vc = buildMinimalVertical("air-conditioner");
        when(verticalsService.getConfigById("air-conditioner")).thenReturn(vc);
        when(googleTaxonomyService.getLastCategoriesId()).thenReturn(Collections.emptyMap());
        when(googleTaxonomyService.getFullCategoriesId()).thenReturn(Collections.emptyMap());

        mockMvc.perform(get("/api/referentials/google/candidates").param("vertical", "air-conditioner"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void etimCandidatesReturns404WhenVerticalNotFound()
            throws Exception
    {
        when(verticalsService.getConfigById("unknown")).thenReturn(null);
        mockMvc.perform(get("/api/referentials/etim/candidates").param("vertical", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wikidataCandidatesReturns404WhenVerticalNotFound()
            throws Exception
    {
        when(verticalsService.getConfigById("unknown")).thenReturn(null);
        mockMvc.perform(get("/api/referentials/wikidata/candidates").param("vertical", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void wikidataCandidatesReturnsOkForKnownVertical()
            throws Exception
    {
        VerticalConfig vc = buildMinimalVertical("tv");
        when(verticalsService.getConfigById("tv")).thenReturn(vc);
        when(wikidataSearchService.executeSparql(org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/referentials/wikidata/candidates").param("vertical", "tv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void attributeCoverageReturns404WhenVerticalNotFound()
            throws Exception
    {
        when(verticalsService.getConfigById("unknown")).thenReturn(null);
        mockMvc.perform(get("/api/referentials/attribute/coverage").param("vertical", "unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    void attributeCoverageReturnsEmptyListWhenAttributesConfigMissing()
            throws Exception
    {
        VerticalConfig vc = buildMinimalVertical("tv");
        when(verticalsService.getConfigById("tv")).thenReturn(vc);

        mockMvc.perform(get("/api/referentials/attribute/coverage").param("vertical", "tv"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void attributeIcecatCandidatesReturns404WhenAttributeMissing()
            throws Exception
    {
        VerticalConfig vc = buildMinimalVertical("tv");
        when(verticalsService.getConfigById("tv")).thenReturn(vc);
        mockMvc.perform(get("/api/referentials/attribute/icecat/candidates")
                        .param("vertical", "tv")
                        .param("attribute", "UNKNOWN"))
                .andExpect(status().isNotFound());
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private VerticalConfig buildMinimalVertical(String id)
    {
        VerticalConfig vc = new VerticalConfig();
        vc.setId(id);

        ProductI18nElements fr = new ProductI18nElements();
        fr.setVerticalHomeTitle("Test vertical");

        Map<String, ProductI18nElements> i18n = new HashMap<>();
        i18n.put("fr", fr);
        vc.setI18n(i18n);
        return vc;
    }
}
