package org.open4goods.api.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.api.config.yml.VerticalsGenerationConfig;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.NudgeToolConfig;
import org.open4goods.model.vertical.NudgeToolScore;
import org.open4goods.model.vertical.SubsetCriteriaOperator;
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

    @Test
    void generateEcoscoreYamlConfigWithAiResultTest() throws Exception {
        // Setup
        VerticalConfig vConf = verticalConfig("tv");
        org.open4goods.model.vertical.ProductI18nElements frI18n = new org.open4goods.model.vertical.ProductI18nElements();
        frI18n.setVerticalHomeTitle("TV");
        vConf.setI18n(Map.of("fr", frI18n));

        PromptService promptService = mock(PromptService.class);
        SerialisationService serialisationService = mock(SerialisationService.class);
        
        org.open4goods.services.prompt.config.PromptConfig promptConfig = new org.open4goods.services.prompt.config.PromptConfig();
        
        org.open4goods.model.ai.ImpactScoreAiResult aiResult = new org.open4goods.model.ai.ImpactScoreAiResult();
        org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight cw1 = new org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight();
        cw1.criterion = "SCORE_1";
        cw1.weight = 0.3;
        org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight cw2 = new org.open4goods.model.ai.ImpactScoreAiResult.CriteriaWeight();
        cw2.criterion = "SCORE_2";
        cw2.weight = 0.7;
        aiResult.setCriteriaWeights(List.of(cw1, cw2));
        
        org.open4goods.services.prompt.dto.PromptResponse<org.open4goods.model.ai.ImpactScoreAiResult> response = new org.open4goods.services.prompt.dto.PromptResponse<>();
        response.setBody(aiResult);
        response.setPrompt(promptConfig);

        when(promptService.objectPrompt(
                org.mockito.ArgumentMatchers.eq("impactscore-generation"), 
                org.mockito.ArgumentMatchers.anyMap(), 
                org.mockito.ArgumentMatchers.eq(org.open4goods.model.ai.ImpactScoreAiResult.class)))
            .thenReturn(response);

        when(serialisationService.toYaml(org.mockito.ArgumentMatchers.any(ImpactScoreConfig.class))).thenAnswer(invocation -> {
            ImpactScoreConfig config = invocation.getArgument(0);
            return "criteriasPonderation: " + config.getCriteriasPonderation();
        });
        
        when(serialisationService.toYaml(org.mockito.ArgumentMatchers.any(org.open4goods.services.prompt.config.PromptConfig.class))).thenReturn("PROMPT");
        when(serialisationService.toJson(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyBoolean())).thenReturn("JSON");

        VerticalsGenerationService service = new VerticalsGenerationService(
                new VerticalsGenerationConfig(),
                mock(ProductRepository.class),
                serialisationService,
                mock(GoogleTaxonomyService.class),
                mock(VerticalsConfigService.class),
                mock(ResourcePatternResolver.class),
                mock(EvaluationService.class),
                mock(IcecatService.class),
                promptService);

        // Execute
        String result = service.generateEcoscoreYamlConfig(vConf);

        // Verify
        assertThat(result).contains("SCORE_1=0.3");
        assertThat(result).contains("SCORE_2=0.7");
    }

    @Test
    void updateVerticalFileWithCategoriesHandlesEndOfFile(@TempDir java.nio.file.Path tempDir) throws Exception {
        // Setup
        String verticalId = "test-vertical";
        File tempFile = tempDir.resolve(verticalId + ".yml").toFile();
        String initialContent = "id: " + verticalId + "\nmatchingCategories:\n  all: []";
        FileUtils.writeStringToFile(tempFile, initialContent, Charset.defaultCharset());

        VerticalConfig vConf = verticalConfig(verticalId);
        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        when(verticalsConfigService.getConfigById(verticalId)).thenReturn(vConf);

        ProductRepository repository = mock(ProductRepository.class);
        when(repository.exportVerticalWithOffersCountGreater(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.anyInt()))
            .thenReturn(Stream.empty());

        SerialisationService serialisationService = mock(SerialisationService.class);
        when(serialisationService.toYaml(org.mockito.ArgumentMatchers.anyMap())).thenReturn("matchingCategories:\n  all: [\"NEW_CAT\"]\n");

        VerticalsGenerationService service = new VerticalsGenerationService(
                new VerticalsGenerationConfig(),
                repository,
                serialisationService,
                mock(GoogleTaxonomyService.class),
                verticalsConfigService,
                mock(ResourcePatternResolver.class),
                mock(EvaluationService.class),
                mock(IcecatService.class),
                mock(PromptService.class));

        // Execute
        String result = service.updateVerticalFileWithCategories(1, tempFile.getAbsolutePath());

        // Verify
        assertThat(result).isEqualTo("id: test-vertical\nmatchingCategories:\n  all: [\"NEW_CAT\"]\n");
        String fileContent = FileUtils.readFileToString(tempFile, Charset.defaultCharset());
        assertThat(fileContent).isEqualTo(result);
    }

    @Test
    void updateVerticalFileWithNudgeToolConfigUpdatesThresholds(@TempDir java.nio.file.Path tempDir) throws Exception {
        String verticalId = "tv";
        File tempFile = tempDir.resolve(verticalId + ".yml").toFile();
        String initialContent = ""
                + "id: " + verticalId + "\n"
                + "nudgeToolConfig:\n"
                + "  scores:\n"
                + "    - scoreName: \"ENERGY_CONSUMPTION\"\n"
                + "      scoreMinValue: 2.5\n"
                + "  subsets:\n"
                + "    - id: \"impact_high\"\n"
                + "      group: \"impactscore\"\n"
                + "      criterias:\n"
                + "        - field: \"scores.ECOSCORE.value\"\n"
                + "          operator: \"LOWER_THAN\"\n"
                + "          value: \"2\"\n"
                + "    - id: \"impact_medium\"\n"
                + "      group: \"impactscore\"\n"
                + "      criterias:\n"
                + "        - field: \"scores.ECOSCORE.value\"\n"
                + "          operator: \"GREATER_THAN\"\n"
                + "          value: \"2\"\n"
                + "        - field: \"scores.ECOSCORE.value\"\n"
                + "          operator: \"LOWER_THAN\"\n"
                + "          value: \"4\"\n"
                + "    - id: \"impact_low\"\n"
                + "      group: \"impactscore\"\n"
                + "      criterias:\n"
                + "        - field: \"scores.ECOSCORE.value\"\n"
                + "          operator: \"GREATER_THAN\"\n"
                + "          value: \"4\"\n"
                + "subsets:\n"
                + "  - id: \"impact_high\"\n"
                + "    group: \"impactscore\"\n"
                + "    criterias:\n"
                + "      - field: \"scores.ECOSCORE.value\"\n"
                + "        operator: \"LOWER_THAN\"\n"
                + "        value: \"2\"\n"
                + "  - id: \"impact_medium\"\n"
                + "    group: \"impactscore\"\n"
                + "    criterias:\n"
                + "      - field: \"scores.ECOSCORE.value\"\n"
                + "        operator: \"GREATER_THAN\"\n"
                + "        value: \"2\"\n"
                + "      - field: \"scores.ECOSCORE.value\"\n"
                + "        operator: \"LOWER_THAN\"\n"
                + "        value: \"4\"\n"
                + "  - id: \"impact_low\"\n"
                + "    group: \"impactscore\"\n"
                + "    criterias:\n"
                + "      - field: \"scores.ECOSCORE.value\"\n"
                + "        operator: \"GREATER_THAN\"\n"
                + "        value: \"4\"\n";
        FileUtils.writeStringToFile(tempFile, initialContent, Charset.defaultCharset());

        VerticalConfig vConf = verticalConfig(verticalId);
        NudgeToolScore score = new NudgeToolScore();
        score.setScoreName("ENERGY_CONSUMPTION");
        NudgeToolConfig nudgeToolConfig = new NudgeToolConfig();
        nudgeToolConfig.setScores(List.of(score));
        vConf.setNudgeToolConfig(nudgeToolConfig);

        VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
        when(verticalsConfigService.getConfigById(verticalId)).thenReturn(vConf);

        ProductRepository repository = mock(ProductRepository.class);
        when(repository.countMainIndexHavingScoreWithFilters(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(verticalId)))
                .thenReturn(50L);
        when(repository.countMainIndexHavingScoreThreshold(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(verticalId),
                org.mockito.ArgumentMatchers.any(SubsetCriteriaOperator.class), org.mockito.ArgumentMatchers.anyDouble()))
                .thenAnswer(invocation -> {
                    SubsetCriteriaOperator operator = invocation.getArgument(2);
                    double threshold = invocation.getArgument(3);
                    if (operator == SubsetCriteriaOperator.GREATER_THAN) {
                        return Math.round((5.0 - threshold) * 10);
                    }
                    if (operator == SubsetCriteriaOperator.LOWER_THAN) {
                        return Math.round(threshold * 10);
                    }
                    return 0L;
                });

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

        String result = service.updateVerticalFileWithNudgeToolConfig(tempFile.getAbsolutePath());

        assertThat(result).contains("scoreMinValue: 3.3");
        assertThat(result).contains("value: \"1.7\"");
        assertThat(result).contains("value: \"3.3\"");
    }

    private VerticalConfig verticalConfig(String id) {
        VerticalConfig config = new VerticalConfig();
        config.setId(id);
        return config;
    }
}
