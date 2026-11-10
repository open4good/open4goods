package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

class ParticipatingScoreHelperTest {

    @Test
    void shouldNormalizeParticipatingScores() {
        AttributeConfig first = new AttributeConfig();
        first.setKey("SCORE_A");
        first.setAsScore(true);
        first.setParticipateInScores(Set.of("AGG"));

        AttributeConfig second = new AttributeConfig();
        second.setKey("SCORE_B");
        second.setAsScore(true);
        second.setParticipateInScores(Set.of("AGG"));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("SCORE_A", 0.2, "SCORE_B", 0.3));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(first, second));
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(attributesConfig);
        verticalConfig.setAvailableImpactScoreCriterias(List.of("SCORE_A", "SCORE_B"));

        Map<String, Map<String, Double>> aggregates = ParticipatingScoreHelper
                .buildNormalizedParticipatingScores(verticalConfig);

        assertThat(aggregates).containsKey("AGG");
        assertThat(aggregates.get("AGG")).containsEntry("SCORE_A", 0.4)
                .containsEntry("SCORE_B", 0.6);
    }

    @Test
    void shouldFailWhenPonderationMissingForParticipatingScore() {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("UNWEIGHTED");
        attributeConfig.setAsScore(true);
        attributeConfig.setParticipateInScores(Set.of("AGG"));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of());

        AttributesConfig attributesConfig = new AttributesConfig(List.of(attributeConfig));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(attributesConfig);
        verticalConfig.setAvailableImpactScoreCriterias(List.of("UNWEIGHTED"));

        assertThatThrownBy(() -> ParticipatingScoreHelper.buildNormalizedParticipatingScores(verticalConfig))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("UNWEIGHTED");
    }

    @Test
    void shouldIgnoreCompositeUsingTheImpactScoreName() {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("REFRIGERANT_GWP");
        attributeConfig.setAsScore(true);
        attributeConfig.setParticipateInScores(Set.of("ECOSCORE", "CLIMATE"));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("REFRIGERANT_GWP", 0.1));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(new AttributesConfig(List.of(attributeConfig)));
        verticalConfig.setAvailableImpactScoreCriterias(List.of("REFRIGERANT_GWP"));

        Map<String, Map<String, Double>> aggregates = ParticipatingScoreHelper
                .buildNormalizedParticipatingScores(verticalConfig);

        assertThat(aggregates).doesNotContainKey("ECOSCORE");
        assertThat(aggregates.get("CLIMATE")).containsEntry("REFRIGERANT_GWP", 1.0);
    }

    @Test
    void shouldIgnoreParticipatingScoreNotDeclaredInAvailableImpactScoreCriterias() {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("IGNORED");
        attributeConfig.setAsScore(true);
        attributeConfig.setParticipateInScores(Set.of("AGG"));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("IGNORED", 0.5));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(attributeConfig));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(attributesConfig);
        verticalConfig.setAvailableImpactScoreCriterias(List.of("OTHER"));

        Map<String, Map<String, Double>> aggregates = ParticipatingScoreHelper
                .buildNormalizedParticipatingScores(verticalConfig);

        assertThat(aggregates).isEmpty();
    }
    @Test
    void shouldIgnoreAggregateWhenTotalPonderationIsZero() {
        AttributeConfig attributeConfig = new AttributeConfig();
        attributeConfig.setKey("ZERO_WEIGHT");
        attributeConfig.setAsScore(true);
        attributeConfig.setParticipateInScores(Set.of("AGG_ZERO"));

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("ZERO_WEIGHT", 0.0));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(attributeConfig));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(attributesConfig);
        verticalConfig.setAvailableImpactScoreCriterias(List.of("ZERO_WEIGHT"));

        Map<String, Map<String, Double>> aggregates = ParticipatingScoreHelper
                .buildNormalizedParticipatingScores(verticalConfig);

        assertThat(aggregates).doesNotContainKey("AGG_ZERO");
    }

    /**
     * GOU-2041: in V2 a derived criterion takes the place of the source it replaces in a composite, while V1 keeps
     * the source.
     */
    @Test
    void v2DerivedCriterionReplacesItsSourceInComposites() {
        AttributeConfig source = new AttributeConfig();
        source.setKey("SOURCE");
        source.setAsScore(true);
        source.setParticipateInScores(Set.of("AGG"));

        AttributeConfig derived = new AttributeConfig();
        derived.setKey("DERIVED");
        derived.setAsScore(true);
        derived.setScoreSources(List.of("OTHER_SOURCE", "SOURCE"));
        derived.setParticipateInScores(Set.of("AGG"));

        AttributeConfig other = new AttributeConfig();
        other.setKey("OTHER");
        other.setAsScore(true);
        other.setParticipateInScores(Set.of("AGG"));

        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(Map.of("DERIVED", 0.25, "OTHER", 0.75));
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("SOURCE", 0.5, "OTHER", 0.5));
        impactScoreConfig.setV2(v2);

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setImpactScoreConfig(impactScoreConfig);
        verticalConfig.setAttributesConfig(new AttributesConfig(List.of(source, derived, other)));
        verticalConfig.setAvailableImpactScoreCriterias(List.of("SOURCE", "OTHER"));

        assertThat(ParticipatingScoreHelper.buildNormalizedParticipatingScores(verticalConfig).get("AGG"))
                .containsOnlyKeys("SOURCE", "OTHER");

        impactScoreConfig.setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
        assertThat(ParticipatingScoreHelper.buildNormalizedParticipatingScores(verticalConfig).get("AGG"))
                .containsOnlyKeys("DERIVED", "OTHER")
                .containsEntry("DERIVED", 0.25);
    }
}
