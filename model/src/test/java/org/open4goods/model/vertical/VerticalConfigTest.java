package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

class VerticalConfigTest {

    @Test
    void getAggregatedScoresReturnsDistinctAggregatesFromScoreAttributes() {
        AttributeConfig energyEfficiency = new AttributeConfig();
        energyEfficiency.setKey("ENERGY");
        energyEfficiency.setAsScore(true);
        energyEfficiency.setParticipateInScores(Set.of("GLOBAL", "ECO"));

        AttributeConfig noiseLevel = new AttributeConfig();
        noiseLevel.setKey("NOISE");
        noiseLevel.setAsScore(true);
        noiseLevel.setParticipateInScores(Set.of("GLOBAL", "COMFORT"));

        AttributeConfig ignored = new AttributeConfig();
        ignored.setKey("DIMENSION");
        ignored.setAsScore(false);
        ignored.setParticipateInScores(Set.of("GLOBAL"));

        AttributesConfig attributesConfig = new AttributesConfig(List.of(energyEfficiency, noiseLevel, ignored));

        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setAttributesConfig(attributesConfig);

        assertThat(verticalConfig.getAggregatedScores())
                .containsExactly("COMFORT", "ECO", "GLOBAL");
    }
}
