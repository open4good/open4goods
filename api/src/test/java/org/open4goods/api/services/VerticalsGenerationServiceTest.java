package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.prompt.service.PromptService;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.core.io.support.ResourcePatternResolver;

class VerticalsGenerationServiceTest {

    @Test
    void generateAvailableImpactScoreCriteriasFragmentIncludesCoverageComments() {
        VerticalConfig target = verticalConfig("tv");
        target.setAvailableImpactScoreCriterias(List.of("TARGET_SCORE"));

        VerticalConfig other = verticalConfig("other");
        AttributeConfig scoreAttribute = new AttributeConfig();
        scoreAttribute.setKey("SCORE_ATTR");
        scoreAttribute.setAsScore(true);
        other.setAttributesConfig(new AttributesConfig(List.of(scoreAttribute)));
        other.setAvailableImpactScoreCriterias(List.of("SCORE_AVAILABLE"));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("SCORE_PONDER", 0.5));
        other.setImpactScoreConfig(impactScoreConfig);

        VerticalConfig defaultConfig = verticalConfig("_default");
        defaultConfig.setAvailableImpactScoreCriterias(List.of("DEFAULT_SCORE"));

        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        when(verticalsConfigService.getConfigsWithoutDefault()).thenReturn(List.of(target, other));
        when(verticalsConfigService.getDefaultConfig()).thenReturn(defaultConfig);

        ProductRepository repository = mock(ProductRepository.class);
        when(repository.countMainIndexHavingVertical("tv")).thenReturn(100L);
        when(repository.countMainIndexHavingScore("SCORE_ATTR", "tv")).thenReturn(15L);
        when(repository.countMainIndexHavingScore("SCORE_AVAILABLE", "tv")).thenReturn(10L);
        when(repository.countMainIndexHavingScore("SCORE_PONDER", "tv")).thenReturn(9L);
        when(repository.countMainIndexHavingScore("DEFAULT_SCORE", "tv")).thenReturn(50L);
        when(repository.countMainIndexHavingScore("TARGET_SCORE", "tv")).thenReturn(40L);

        VerticalsGenerationService service = new VerticalsGenerationService(
                new VerticalsGenerationConfig(),
                repository,
                mock(SerialisationService.class),
                mock(GoogleTaxonomyService.class),
                verticalsConfigService,
                mock(ResourcePatternResolver.class),
                mock(EvaluationService.class),
                mock(IcecatService.class),
                mock(PromptService.class));

        String fragment = service.generateAvailableImpactScoreCriteriasFragment(target, 10);

        assertThat(fragment).contains("availableImpactScoreCriterias:");
        assertThat(fragment).contains("# coverage: 15% (15/100)");
        assertThat(fragment).contains("- SCORE_ATTR");
        assertThat(fragment).contains("# coverage: 10% (10/100)");
        assertThat(fragment).contains("- SCORE_AVAILABLE");
        assertThat(fragment).contains("# coverage: 50% (50/100)");
        assertThat(fragment).contains("- DEFAULT_SCORE");
        assertThat(fragment).doesNotContain("SCORE_PONDER");
        assertThat(fragment).doesNotContain("TARGET_SCORE");
    }

    private VerticalConfig verticalConfig(String id) {
        VerticalConfig config = new VerticalConfig();
        config.setId(id);
        return config;
    }
}
