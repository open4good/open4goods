package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.model.ai.ImpactScoreAiResult;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScorePositionLabel;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.ProductCategory;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.config.properties.ApiProperties;
import org.open4goods.nudgerfrontapi.dto.category.CategoryBreadcrumbItemDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreAuditDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreConfigDto;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScorePositionLabelDto;
import org.open4goods.nudgerfrontapi.dto.category.VerticalConfigFullDto;
import org.open4goods.model.localization.DomainLanguage;
import org.open4goods.verticals.GoogleTaxonomyService;

class CategoryMappingServiceFullDtoTest {

    private CategoryMappingService service;
    private GoogleTaxonomyService googleTaxonomyService;

    @BeforeEach
    void setUp() {
        ApiProperties apiProperties = new ApiProperties();
        googleTaxonomyService = mock(GoogleTaxonomyService.class);
        service = new CategoryMappingService(apiProperties, googleTaxonomyService);
    }

    @Test
    void toVerticalConfigFullDtoBuildsFullBreadcrumbPaths() {
        ProductCategory electronics = new ProductCategory(1, "Appareils électroniques", "fr");

        ProductCategory video = new ProductCategory(2, "Vidéo", "fr");
        video.setParent(electronics);
        electronics.addChild(video);

        ProductCategory televisions = new ProductCategory(3, "Téléviseurs", "fr");
        televisions.setParent(video);
        video.addChild(televisions);

        when(googleTaxonomyService.byId(3)).thenReturn(televisions);

        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setEnabled(true);
        config.setGoogleTaxonomyId(3);

        VerticalConfigFullDto dto = service.toVerticalConfigFullDto(config, DomainLanguage.fr);

        assertThat(dto.breadCrumb())
                .extracting(CategoryBreadcrumbItemDto::title)
                .containsExactly("Appareils électroniques", "Vidéo", "Téléviseurs");
        assertThat(dto.breadCrumb())
                .extracting(CategoryBreadcrumbItemDto::link)
                .containsExactly(
                        "/categories/appareils-electroniques",
                        "/categories/appareils-electroniques/video",
                        "/categories/appareils-electroniques/video/televiseurs");
    }

    /** GOU-1882: the category exposes the vertical's current ImpactScore methodology. */
    @Test
    void impactScoreConfigExposesTheCurrentMethodology() {
        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(new HashMap<>(Map.of("ENERGY", 1.0)));
        v2.setMinRealWeightCoverage(0.6);
        v2.setPositionLabels(List.of(new ImpactScorePositionLabel("excellent", 0.9),
                new ImpactScorePositionLabel("poor", 0.0)));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(new HashMap<>(Map.of("ENERGY", 0.8, "DATA_QUALITY", 0.2)));
        impactScoreConfig.setV2(v2);
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setImpactScoreConfig(impactScoreConfig);

        ImpactScoreConfigDto v1 = service.toVerticalConfigFullDto(config, DomainLanguage.fr).impactScoreConfig();
        assertThat(v1.methodologyVersion()).isEqualTo(1);
        assertThat(v1.criteriasPonderation()).containsOnlyKeys("ENERGY", "DATA_QUALITY");
        assertThat(v1.minRealWeightCoverage()).isNull();
        assertThat(v1.positionLabels()).isEmpty();

        impactScoreConfig.setMethodologyVersionOverride(2);
        ImpactScoreConfigDto v2Dto = service.toVerticalConfigFullDto(config, DomainLanguage.fr).impactScoreConfig();
        assertThat(v2Dto.methodologyVersion()).isEqualTo(2);
        assertThat(v2Dto.criteriasPonderation()).containsOnlyKeys("ENERGY");
        assertThat(v2Dto.minRealWeightCoverage()).isEqualTo(0.6);
        assertThat(v2Dto.positionLabels()).containsExactly(new ImpactScorePositionLabelDto("excellent", 0.9),
                new ImpactScorePositionLabelDto("poor", 0.0));
    }

    /** GOU-1901: the useful texts of the stored AI audit reach the method page, without the raw prompt. */
    @Test
    void impactScoreConfigExposesTheUsefulTextsOfTheAudit() {
        ImpactScoreAiResult aiResult = new ImpactScoreAiResult();
        ImpactScoreAiResult.Positioning positioning = new ImpactScoreAiResult.Positioning();
        positioning.whatItMeansForUsers = "Compare within the category.";
        positioning.notAnAbsoluteFootprint = true;
        aiResult.setPositioning(positioning);
        ImpactScoreAiResult.GapAnalysis gap = new ImpactScoreAiResult.GapAnalysis();
        gap.whatIsMissingVsFullLca = "Manufacturing is not measured.";
        gap.mitigations = List.of("Stay within the category.");
        aiResult.setGapAnalysis(gap);
        ImpactScoreAiResult.CriteriaWeight criterion = new ImpactScoreAiResult.CriteriaWeight();
        criterion.criterion = "ENERGY";
        criterion.weight = 0.8;
        criterion.directionality = "lower_is_better";
        criterion.rationale = "Use phase dominates.";
        aiResult.setCriteriaWeights(List.of(criterion));
        ImpactScoreAiResult.Source used = new ImpactScoreAiResult.Source();
        used.id = "S1";
        used.title = "Energy label";
        used.used = true;
        ImpactScoreAiResult.Source unused = new ImpactScoreAiResult.Source();
        unused.id = "S2";
        unused.used = false;
        aiResult.setSources(List.of(used, unused));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(new HashMap<>(Map.of("ENERGY", 1.0)));
        impactScoreConfig.setAiResult(aiResult);
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setImpactScoreConfig(impactScoreConfig);

        ImpactScoreAuditDto audit = service.toVerticalConfigFullDto(config, DomainLanguage.fr).impactScoreConfig().audit();

        assertThat(audit.whatItMeansForUsers()).isEqualTo("Compare within the category.");
        assertThat(audit.notAnAbsoluteFootprint()).isTrue();
        assertThat(audit.whatIsMissingVsFullLca()).isEqualTo("Manufacturing is not measured.");
        assertThat(audit.mitigations()).containsExactly("Stay within the category.");
        assertThat(audit.criteria()).singleElement().satisfies(c -> {
            assertThat(c.key()).isEqualTo("ENERGY");
            assertThat(c.rationale()).isEqualTo("Use phase dominates.");
        });
        assertThat(audit.sources()).extracting("id").containsExactly("S1");

        impactScoreConfig.setAiResult(null);
        assertThat(service.toVerticalConfigFullDto(config, DomainLanguage.fr).impactScoreConfig().audit()).isNull();
    }
}
