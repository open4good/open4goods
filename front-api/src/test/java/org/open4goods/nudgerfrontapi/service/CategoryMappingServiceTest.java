package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.FeatureGroup;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreCriteria;
import org.open4goods.model.vertical.ProductI18nElements;
import org.open4goods.model.vertical.ResourcesAggregationConfig;
import org.open4goods.model.vertical.SiteNaming;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.VerticalSubset;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;

class CategoryMappingServiceTest {

    private CategoryMappingService service;

    @BeforeEach
    void setUp() {
        service = new CategoryMappingService();
    }

    @Test
    void toVerticalConfigDtoMapsBasicFields() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setOrder(1);
        config.setGoogleTaxonomyId(404);
        config.setIcecatTaxonomyId(1584);

        ProductI18nElements fr = new ProductI18nElements();
        fr.setVerticalHomeTitle("Téléviseurs");
        fr.setVerticalHomeDescription("Desc");
        fr.setVerticalHomeUrl("televiseurs");
        config.setI18n(Map.of("fr", fr));

        VerticalConfigDto dto = service.toVerticalConfigDto(config);

        assertThat(dto.id()).isEqualTo("tv");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.googleTaxonomyId()).isEqualTo(404);
        assertThat(dto.image()).isNull();
        assertThat(dto.singularName()).isNull();
        assertThat(dto.i18n()).containsKey("fr");
        assertThat(dto.i18n().get("fr").title()).isEqualTo("Téléviseurs");
        assertThat(dto.i18n().get("fr").description()).isEqualTo("Desc");
        assertThat(dto.i18n().get("fr").url()).isEqualTo("televiseurs");
    }

    @Test
    void toVerticalConfigFullDtoMirrorsFields() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setOrder(2);
        config.setGoogleTaxonomyId(404);
        config.setIcecatTaxonomyId(1584);
        config.setEcoFilters(List.of("eco"));
        config.setTechnicalFilters(List.of("tech"));
        config.setGlobalTechnicalFilters(List.of("global"));
        config.setMatchingCategories(Map.of("all", Set.of("category")));
        config.setExcludingTokensFromCategoriesMatching(Set.of("accessoire"));
        config.setGenerationExcludedFromCategoriesMatching(Set.of("fnac"));
        config.setGenerationExcludedFromAttributesMatching(Set.of("PRICE"));
        config.setRequiredAttributes(Set.of("ENERGY_CLASS"));
        config.setForceNameGeneration(true);
        config.setBrandsAlias(Map.of("LG ELECTRONICS", "LG"));
        config.setBrandsExclusion(Set.of("UNKNOWN"));
        config.setNamings(new SiteNaming());
        config.setResourcesConfig(new ResourcesAggregationConfig());
        config.setAvailableImpactScoreCriterias(Map.of("score", new ImpactScoreCriteria()));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        config.setImpactScoreConfig(impactScoreConfig);
        VerticalSubset subset = new VerticalSubset();
        config.setSubsets(List.of(subset));
        FeatureGroup featureGroup = new FeatureGroup();
        config.setFeatureGroups(List.of(featureGroup));

        ProductI18nElements fr = new ProductI18nElements();
        fr.setVerticalHomeTitle("Téléviseurs");
        fr.setVerticalHomeDescription("Desc");
        fr.setVerticalHomeUrl("televiseurs");
        Map<String, ProductI18nElements> i18n = Map.of("fr", fr);
        config.setI18n(i18n);

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(config);

        assertThat(dto.id()).isEqualTo("tv");
        assertThat(dto.enabled()).isTrue();
        assertThat(dto.rawI18n()).isSameAs(i18n);
        assertThat(dto.ecoFilters()).containsExactly("eco");
        assertThat(dto.technicalFilters()).containsExactly("tech");
        assertThat(dto.globalTechnicalFilters()).containsExactly("global");
        assertThat(dto.matchingCategories()).containsKey("all");
        assertThat(dto.excludingTokensFromCategoriesMatching()).contains("accessoire");
        assertThat(dto.generationExcludedFromCategoriesMatching()).contains("fnac");
        assertThat(dto.generationExcludedFromAttributesMatching()).contains("PRICE");
        assertThat(dto.requiredAttributes()).contains("ENERGY_CLASS");
        assertThat(dto.forceNameGeneration()).isTrue();
        assertThat(dto.brandsAlias()).containsEntry("LG ELECTRONICS", "LG");
        assertThat(dto.brandsExclusion()).contains("UNKNOWN");
        assertThat(dto.namings()).isNotNull();
        assertThat(dto.resourcesConfig()).isNotNull();
        assertThat(dto.availableImpactScoreCriterias()).containsKey("score");
        assertThat(dto.impactScoreConfig()).isSameAs(impactScoreConfig);
        assertThat(dto.subsets()).containsExactly(subset);
        assertThat(dto.featureGroups()).containsExactly(featureGroup);
        assertThat(dto.imageThumbnail()).isNull();
        assertThat(dto.singularName()).isNull();
    }
}
