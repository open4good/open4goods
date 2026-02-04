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

class VerticalsConfigServiceFallbackTest {

    private static VerticalsConfigService verticalsConfigService;

    @BeforeAll
    static void setUp() throws Exception {
        SerialisationService serialisationService = new SerialisationService();
        GoogleTaxonomyService googleTaxonomyService = mock(GoogleTaxonomyService.class);
        ResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();
        verticalsConfigService = new VerticalsConfigService(serialisationService, googleTaxonomyService, resourceResolver);
        
        // Add test specific paths
        verticalsConfigService.addConfigPath("classpath:/verticals_test/*.yml");
        verticalsConfigService.addConfigPath("classpath:/verticals_test/test_json_fallback.yml"); // Try specific
        verticalsConfigService.addImpactScorePath("classpath:/verticals_test/impactscores/*.yml");
        verticalsConfigService.addImpactScoreJsonPath("classpath:/verticals_test/impactscores/*.json");
        
        System.out.println("DEBUG PROBE: Resolving classpath:/verticals_test/*.yml");
        try {
            org.springframework.core.io.Resource[] res = resourceResolver.getResources("classpath:/verticals_test/*.yml");
            System.out.println("Found: " + res.length);
            for(org.springframework.core.io.Resource r : res) System.out.println(" - " + r.getFilename() + " (" + r.getURI() + ")");
        } catch(Exception e) { e.printStackTrace(); }

        System.out.println("DEBUG PROBE: Resolving classpath:/verticals/*.yml");
        try {
            org.springframework.core.io.Resource[] res = resourceResolver.getResources("classpath:/verticals/*.yml");
            System.out.println("Found wildcards: " + res.length);
            for(org.springframework.core.io.Resource r : res) System.out.println(" - " + r.getFilename() + " (" + r.getURI() + ")");
        } catch(Exception e) { e.printStackTrace(); }

        System.out.println("DEBUG PROBE: Resolving SPECIFIC FILE classpath:/verticals_test/test_json_fallback.yml");
        try {
            org.springframework.core.io.Resource[] res = resourceResolver.getResources("classpath:/verticals_test/test_json_fallback.yml");
            System.out.println("Found specific: " + res.length);
            if (res.length > 0) System.out.println(" - URI: " + res[0].getURI());
        } catch(Exception e) { e.printStackTrace(); }

        verticalsConfigService.loadConfigs();
    }

    @Test
    void shouldInjectAiResultFromJson() {
        VerticalConfig verticalConfig = verticalsConfigService.getConfigById("test_json_fallback");
        
        assertThat(verticalConfig).isNotNull();
        ImpactScoreConfig impactScoreConfig = verticalConfig.getImpactScoreConfig();
        
        assertThat(impactScoreConfig).isNotNull();
        
        // Verify standard YAML properties are loaded
        assertThat(impactScoreConfig.getMinDistinctValuesForSigma())
            .as("Min distinct values for sigma from YAML")
            .isEqualTo(5);
            
        // Verify AI Result is injected
        assertThat(impactScoreConfig.getAiResult())
            .as("AI Result should be injected from JSON")
            .isNotNull();
            
        assertThat(impactScoreConfig.getAiResult().getUseCase())
            .as("UseCase from injected JSON")
            .isEqualTo("consumer_comparison");
            
        assertThat(impactScoreConfig.getAiResult().getVertical())
            .as("Vertical from injected JSON")
            .isEqualTo("test_vertical");
    }
}
