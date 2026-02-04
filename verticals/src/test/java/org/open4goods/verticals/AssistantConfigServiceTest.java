package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.NudgeToolConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

/**
 * Tests loading assistant configurations from classpath resources.
 */
class AssistantConfigServiceTest {

    private static VerticalsConfigService verticalsConfigService;

    /**
     * Build a config service with the classpath resources to exercise assistant loading.
     *
     * @throws Exception when initialization fails
     */
    @BeforeAll
    static void setUp() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        GoogleTaxonomyService googleTaxonomyService = mock(GoogleTaxonomyService.class);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

        verticalsConfigService = new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
    }

    /**
     * Ensure the assistant configuration for tv is available and contains the distance question group.
     */
    @Test
    void shouldLoadAssistantConfigWithDistanceGroup() {
        NudgeToolConfig config = verticalsConfigService.getAssistantConfigById(
                "quelle-taille-de-tv-choisir-pour-son-salon");

        assertThat(config).as("TV assistant config should be loaded").isNotNull();
        assertThat(config.getSubsetGroups())
                .as("Subset groups should be available")
                .isNotEmpty();
        assertThat(config.getSubsetGroups().get(0).getId())
                .as("Distance group must be the first question")
                .isEqualTo("distance");
        assertThat(config.getSubsets())
                .as("Existing filters should be preserved")
                .extracting(subset -> subset.getId())
                .contains("price_lower_500", "large_screens");
    }
}
