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
}
