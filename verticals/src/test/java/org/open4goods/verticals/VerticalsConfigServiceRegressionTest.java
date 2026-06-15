package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Regression tests for specific vertical configuration issues.
 */
class VerticalsConfigServiceRegressionTest {

    private static VerticalsConfigService verticalsConfigService;

    @BeforeAll
    static void setUp() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        GoogleTaxonomyService googleTaxonomyService = mock(GoogleTaxonomyService.class);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        verticalsConfigService = new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
    }

    @Test
    void shouldInheritAsScoreAndNameFromGlobalConfig() {
        // This test verifies that attributes defined in vertical YAMLs without full details
        // correctly inherit 'asScore', 'name', and other properties from the global attributes/*.yml files.
        // Previously, adding 'asScore: true' to the vertical YAML caused the name to be lost.

        checkAttributeInheritance("dishwasher", "WARRANTY");
        checkAttributeInheritance("refrigerator", "WARRANTY");
        checkAttributeInheritance("refrigerator", "REPAIRABILITY_INDEX");
        checkAttributeInheritance("oven", "WARRANTY");
        checkAttributeInheritance("tv", "WARRANTY");
        checkAttributeInheritance("washing-machine", "CLASSE_ENERGY");
        checkAttributeInheritance("washing-machine", "DATA_QUALITY");
    }

    private void checkAttributeInheritance(String verticalId, String attributeKey) {
        VerticalConfig config = verticalsConfigService.getConfigById(verticalId);
        assertThat(config).as("Config for " + verticalId).isNotNull();

        Optional<AttributeConfig> attr = config.getAttributesConfig().getConfigs().stream()
                .filter(c -> attributeKey.equals(c.getKey()))
                .findFirst();

        assertThat(attr).as("Attribute " + attributeKey + " in " + verticalId).isPresent();
        
        // Verify asScore is true (inherited from global config)
        assertThat(attr.get().isAsScore())
            .as(attributeKey + " should have asScore=true in " + verticalId)
            .isTrue();

        // Verify name is present (inherited from global config)
        // If the merge logic is broken (e.g. by overriding with a partial config), this is often null.
        assertThat(attr.get().getName())
            .as(attributeKey + " name should be present in " + verticalId)
            .isNotNull();
            
        // Verify localized name exists (spot check)
        assertThat(attr.get().getName().get("fr"))
            .as(attributeKey + " FR name should be present in " + verticalId)
            .isNotBlank();
    }


    @Test
    void shouldInheritCompositeScoresFromDefault() {
        VerticalConfig tvConfig = verticalsConfigService.getConfigById("tv");
        assertThat(tvConfig).isNotNull();
        
        assertThat(tvConfig.getCompositeScores())
            .as("TV vertical should inherit composite scores from _default.yml")
            .contains("ECOSCORE");
    }

    @Test
    void shouldInheritGlobalImpactScoreAggregationFromDefault() {
        // The Impact Score (ECOSCORE) histogram/slider scale is defined once in _default.yml and
        // must apply to every vertical, even those declaring their own aggregationConfiguration block.
        for (String verticalId : new String[] {"tv", "smartphones", "dishwasher", "oven"}) {
            VerticalConfig config = verticalsConfigService.getConfigById(verticalId);
            assertThat(config).as("Config for " + verticalId).isNotNull();

            var ecoscore = config.getAggregationConfigurationFor("scores.ECOSCORE.value");
            assertThat(ecoscore)
                .as(verticalId + " should inherit the global ECOSCORE aggregation config")
                .isNotNull();
            assertThat(ecoscore.getInterval())
                .as(verticalId + " ECOSCORE interval should come from _default.yml")
                .isEqualTo(0.2d);

            // Vertical-specific aggregation keys must survive the merge (default + own entries).
            assertThat(config.getAggregationConfiguration())
                .as(verticalId + " should keep its own aggregation keys alongside the inherited one")
                .hasSizeGreaterThan(1);
        }
    }

    @Test
    void shouldLoadExternalSubCategoriesByVerticalId() {
        VerticalConfig dishwasherConfig = verticalsConfigService.getConfigById("dishwasher");
        assertThat(dishwasherConfig).isNotNull();

        assertThat(dishwasherConfig.getSubCategories())
                .as("categories/dishwasher/*.yml files should populate dishwasher sub-categories")
                .anySatisfy(subCategory -> {
                    assertThat(subCategory.getId()).isEqualTo("dishwasher_builtin_60");
                    assertThat(subCategory.getDescription().i18n("fr"))
                            .contains("Comparez les **lave-vaisselles encastrables 60 cm**");
                    assertThat(subCategory.getMetaTitle().i18n("fr"))
                            .isEqualTo("Lave-vaisselle encastrable 60 cm | Nudger");
                    assertThat(subCategory.getMetaDescription().i18n("fr"))
                            .contains("Comparez les lave-vaisselles encastrables");
                    assertThat(subCategory.getHeroBlock()).isNotNull();
                    assertThat(subCategory.getHeroBlock().getTitle().i18n("fr")).isEqualTo("Verifiez la niche");
                    assertThat(subCategory.getHeroBlock().getMdiIcon()).isEqualTo("mdi-ruler-square");
                    assertThat(subCategory.getReadMore()).isNotNull();
                    assertThat(subCategory.getReadMore().getShortText().i18n("fr"))
                            .contains("contrainte d'installation");
                    assertThat(subCategory.getReadMore().getLongText().i18n("fr"))
                            .contains("consommation");
                    assertThat(subCategory.getActivatedFilters()).hasSize(3);
                });

        assertThat(dishwasherConfig.getSubCategories())
                .as("dishwasher category should expose a sibling sub-category without a hero block")
                .anySatisfy(subCategory -> {
                    assertThat(subCategory.getId()).isEqualTo("dishwasher_freestanding");
                    assertThat(subCategory.getHeroBlock()).isNull();
                    assertThat(subCategory.getReadMore()).isNotNull();
                    assertThat(subCategory.getMetaOpenGraphTitle().i18n("fr"))
                            .isEqualTo("Comparer les lave-vaisselles pose libre");
                    assertThat(subCategory.getActivatedFilters()).hasSize(1);
                });
    }
}
