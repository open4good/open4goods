package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.model.Localisable;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.wikidataservice.service.WikidataSearchService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
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

    @Test
    void attributeIcecatCandidatesMatchesFrenchKeyViaTokenOverlap()
            throws Exception
    {
        VerticalConfig vc = buildTvVerticalWithAttribute("DIAGONALE_POUCES", null, null);

        IcecatFeatureDocument feature = new IcecatFeatureDocument();
        feature.setId(1464);
        feature.setEnglishName("Screen diagonal (inch)");
        feature.setType("numerical");

        IcecatCategoryDocument category = tvCategoryWithFeatures(feature);

        when(verticalsService.getConfigById("tv")).thenReturn(vc);
        when(icecatIndexService.findCategory(1584)).thenReturn(Optional.of(category));
        when(icecatIndexService.findCategoryFeatureDocuments(category))
                .thenReturn(Map.of(1464, feature));

        mockMvc.perform(get("/api/referentials/attribute/icecat/candidates")
                        .param("vertical", "tv")
                        .param("attribute", "DIAGONALE_POUCES"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featureId").value(1464))
                .andExpect(jsonPath("$[0].matchSource").value("token-overlap"))
                .andExpect(jsonPath("$[0].categoryId").value(1584));
    }

    @Test
    void attributeIcecatCandidatesMatchesViaFrenchLangName()
            throws Exception
    {
        VerticalConfig vc = buildTvVerticalWithAttribute(
                "FREQUENCY_RATE",
                Map.of("fr", "Taux de rafraichissement"),
                null);

        IcecatFeatureDocument feature = new IcecatFeatureDocument();
        feature.setId(2222);
        feature.setEnglishName("Refresh rate");
        feature.setType("numerical");
        feature.setLangNames(List.of("1:Refresh rate", "3:Taux de rafraichissement (Hz)"));

        IcecatCategoryDocument category = tvCategoryWithFeatures(feature);

        when(verticalsService.getConfigById("tv")).thenReturn(vc);
        when(icecatIndexService.findCategory(1584)).thenReturn(Optional.of(category));
        when(icecatIndexService.findCategoryFeatureDocuments(category))
                .thenReturn(Map.of(2222, feature));

        mockMvc.perform(get("/api/referentials/attribute/icecat/candidates")
                        .param("vertical", "tv")
                        .param("attribute", "FREQUENCY_RATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featureId").value(2222))
                .andExpect(jsonPath("$[0].categoryId").value(1584));
    }

    @Test
    void attributeIcecatCandidatesFallsBackToGlobalSearchWhenCategoryEmpty()
            throws Exception
    {
        VerticalConfig vc = buildTvVerticalWithAttribute(
                "HDMI_PORTS_QUANTITY",
                Map.of("fr", "Nombre de ports HDMI"),
                null);

        IcecatCategoryDocument emptyCategory = new IcecatCategoryDocument();
        emptyCategory.setId(1584);
        emptyCategory.setEnglishName("TVs");
        emptyCategory.setFeatures(Collections.emptyList());

        IcecatFeatureDocument hdmiFeature = new IcecatFeatureDocument();
        hdmiFeature.setId(7777);
        hdmiFeature.setEnglishName("Number of HDMI ports");
        hdmiFeature.setType("numerical");

        Page<IcecatFeatureDocument> page = new PageImpl<>(List.of(hdmiFeature));

        when(verticalsService.getConfigById("tv")).thenReturn(vc);
        when(icecatIndexService.findCategory(1584)).thenReturn(Optional.of(emptyCategory));
        when(icecatIndexService.findCategoryFeatureDocuments(emptyCategory))
                .thenReturn(Collections.emptyMap());
        when(icecatIndexService.searchFeatures(anyString(), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/referentials/attribute/icecat/candidates")
                        .param("vertical", "tv")
                        .param("attribute", "HDMI_PORTS_QUANTITY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].featureId").value(7777))
                .andExpect(jsonPath("$[0].matchSource").value("global-fallback"))
                .andExpect(jsonPath("$[0].categoryId").doesNotExist());
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

    private VerticalConfig buildTvVerticalWithAttribute(String attrKey, Map<String, String> localNames, Set<Integer> featureIds)
    {
        VerticalConfig vc = buildMinimalVertical("tv");
        vc.setIcecatTaxonomyId(1584);

        AttributeConfig attr = new AttributeConfig();
        attr.setKey(attrKey);
        Localisable<String, String> name = new Localisable<>();
        if (localNames != null)
        {
            name.putAll(localNames);
        }
        attr.setName(name);

        if (featureIds != null)
        {
            org.open4goods.model.vertical.referential.AttributeReferentials refs = new org.open4goods.model.vertical.referential.AttributeReferentials();
            List<org.open4goods.model.vertical.referential.IcecatFeatureReferential> icecatList = new ArrayList<>();
            for (Integer fid : featureIds)
            {
                icecatList.add(new org.open4goods.model.vertical.referential.IcecatFeatureReferential(fid, null, null));
            }
            refs.setIcecat(icecatList);
            attr.setReferentials(refs);
        }

        AttributesConfig attrsConfig = new AttributesConfig(List.of(attr));
        vc.setAttributesConfig(attrsConfig);

        return vc;
    }

    private IcecatCategoryDocument tvCategoryWithFeatures(IcecatFeatureDocument... features)
    {
        IcecatCategoryDocument category = new IcecatCategoryDocument();
        category.setId(1584);
        category.setEnglishName("TVs");
        List<IcecatCategoryFeatureDocument> catFeatures = new ArrayList<>();
        for (IcecatFeatureDocument f : features)
        {
            IcecatCategoryFeatureDocument cf = new IcecatCategoryFeatureDocument();
            cf.setId(f.getId());
            cf.setType(f.getType());
            catFeatures.add(cf);
        }
        category.setFeatures(catFeatures);
        return category;
    }
}
