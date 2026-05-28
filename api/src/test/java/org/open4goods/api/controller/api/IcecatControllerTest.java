package org.open4goods.api.controller.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.icecat.model.IcecatCategoryFeatureDocument;
import org.open4goods.icecat.model.IcecatCategoryFeatureGroupDocument;
import org.open4goods.icecat.model.IcecatCategoryDocument;
import org.open4goods.icecat.model.IcecatFeatureDocument;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.icecat.services.IcecatIndexService;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Tests for the stable Icecat admin API contract.
 */
@ExtendWith(MockitoExtension.class)
class IcecatControllerTest {

    private MockMvc mockMvc;

    @Mock
    private IcecatService icecatService;

    @Mock
    private VerticalsConfigService verticalsService;

    @Mock
    private IcecatIndexService icecatIndexService;

    @Mock
    private IcecatFeatureResolver icecatFeatureResolver;

    @BeforeEach
    void setUp() {
        IcecatController controller = new IcecatController(
                icecatService,
                verticalsService,
                icecatIndexService,
                icecatFeatureResolver);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void candidateCategoriesForVerticalReturnsConfiguredAndSearchCandidates() throws Exception {
        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("washing-machines");
        vertical.setIcecatTaxonomyId(123);
        ProductI18nElements i18n = new ProductI18nElements();
        i18n.setPageTitle("Washing machines");
        vertical.getI18n().put("en", i18n);

        IcecatCategoryDocument configured = category(123, "Washing Machines");
        IcecatCategoryDocument searched = category(456, "Washer Dryers");

        when(verticalsService.getConfigById("washing-machines")).thenReturn(vertical);
        when(icecatIndexService.findCategory(123)).thenReturn(Optional.of(configured));
        when(icecatIndexService.searchCategories(eq("washing-machines"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(searched)));
        when(icecatIndexService.searchCategories(eq("washing machines"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(icecatIndexService.searchCategories(eq("Washing machines"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/icecat/verticals/washing-machines/candidate-categories")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(123))
                .andExpect(jsonPath("$[0].source").value("configured"))
                .andExpect(jsonPath("$[1].id").value(456))
                .andExpect(jsonPath("$[1].source").value("search:washing-machines"));
    }

    @Test
    void getCategoryAttributesReturnsCategoryScopedAndGlobalFeatureMetadata() throws Exception {
        IcecatCategoryDocument category = category(123, "Washing Machines");

        IcecatCategoryFeatureGroupDocument group = new IcecatCategoryFeatureGroupDocument();
        group.setId(77);
        group.setFeatureGroupIds(List.of(88));
        category.setFeatureGroups(List.of(group));

        IcecatCategoryFeatureDocument categoryFeature = new IcecatCategoryFeatureDocument();
        categoryFeature.setId(42);
        categoryFeature.setCategoryFeatureGroupId(77);
        categoryFeature.setCategoryFeatureId(9001);
        categoryFeature.setMandatory(1);
        categoryFeature.setSearchable(1);
        categoryFeature.setDefaultDisplayUnit("kg");
        category.setFeatures(List.of(categoryFeature));

        IcecatFeatureDocument feature = new IcecatFeatureDocument();
        feature.setId(42);
        feature.setEnglishName("Weight");
        feature.setType("numerical");
        feature.setNormalizedNames(Set.of("weight"));
        feature.setLangNames(List.of("1:Weight", "3:Poids"));

        when(icecatIndexService.findCategory(123)).thenReturn(Optional.of(category));
        when(icecatIndexService.findCategoryFeatureDocuments(category)).thenReturn(Map.of(42, feature));

        mockMvc.perform(get("/icecat/categories/123/attributes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryId").value(123))
                .andExpect(jsonPath("$.featureGroups[0].id").value(77))
                .andExpect(jsonPath("$.featureGroups[0].featureGroupIds[0]").value(88))
                .andExpect(jsonPath("$.attributes[0].id").value(42))
                .andExpect(jsonPath("$.attributes[0].englishName").value("Weight"))
                .andExpect(jsonPath("$.attributes[0].globalType").value("numerical"))
                .andExpect(jsonPath("$.attributes[0].categoryFeatureGroupId").value(77))
                .andExpect(jsonPath("$.attributes[0].categoryFeatureId").value(9001))
                .andExpect(jsonPath("$.attributes[0].mandatory").value(1))
                .andExpect(jsonPath("$.attributes[0].localizedNames.1").value("Weight"))
                .andExpect(jsonPath("$.attributes[0].localizedNames.3").value("Poids"));
    }

    private IcecatCategoryDocument category(Integer id, String name) {
        IcecatCategoryDocument category = new IcecatCategoryDocument();
        category.setId(id);
        category.setEnglishName(name);
        category.setScore(100);
        category.setLangNames(List.of("1:" + name));
        return category;
    }
}
