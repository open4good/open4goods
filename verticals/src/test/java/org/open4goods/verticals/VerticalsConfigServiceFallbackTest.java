package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Validates that an {@link ImpactScoreConfig} loaded from a single YAML file
 * exposes both the manual fields (minDistinctValuesForSigma, criteriasPonderation)
 * and the AI-result audit graph. The legacy JSON overlay was removed in favour
 * of a single source of truth (impactscores/{v}.yml).
 */
class VerticalsConfigServiceFallbackTest {

    private static VerticalsConfigService verticalsConfigService;

    @BeforeAll
    static void setUp() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        GoogleTaxonomyService googleTaxonomyService = mock(GoogleTaxonomyService.class);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        verticalsConfigService = new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);

        verticalsConfigService.addConfigPath("classpath:/verticals_test/*.yml");
        verticalsConfigService.addConfigPath("classpath:/verticals_test/test_json_fallback.yml");
        verticalsConfigService.addImpactScorePath("classpath:/verticals_test/impactscores/*.yml");

        verticalsConfigService.loadConfigs();
    }

    @Test
    void shouldLoadFullImpactScoreConfigFromSingleYaml() {
        VerticalConfig verticalConfig = verticalsConfigService.getConfigById("test_json_fallback");

        assertThat(verticalConfig).isNotNull();
        ImpactScoreConfig impactScoreConfig = verticalConfig.getImpactScoreConfig();

        assertThat(impactScoreConfig).isNotNull();

        assertThat(impactScoreConfig.getMinDistinctValuesForSigma())
            .as("minDistinctValuesForSigma carried by the YAML")
            .isEqualTo(5);

        assertThat(impactScoreConfig.getCriteriasPonderation())
            .as("criteriasPonderation carried by the YAML")
            .containsEntry("TEST_CRITERIA", 0.5)
            .containsEntry("ANOTHER_CRITERIA", 0.5);

        assertThat(impactScoreConfig.getAiResult())
            .as("AI result carried by the YAML (formerly the JSON overlay)")
            .isNotNull();

        assertThat(impactScoreConfig.getAiResult().getUseCase())
            .as("useCase from the YAML's aiResult")
            .isEqualTo("consumer_comparison");

        assertThat(impactScoreConfig.getAiResult().getVertical())
            .as("vertical from the YAML's aiResult")
            .isEqualTo("test_vertical");
    }
}
