package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * GOU-1882: accessors of the active ImpactScore methodology (design {@code docs/impactscore/v2-design.md}, 1.2).
 */
class ImpactScoreConfigTest {

    private static ImpactScoreConfig config() {
        ImpactScoreConfig config = new ImpactScoreConfig();
        config.setCriteriasPonderation(new HashMap<>(Map.of("A", 0.5, "B", 0.3, "DATA_QUALITY", 0.2)));
        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(new HashMap<>(Map.of("A", 0.6, "B", 0.4)));
        v2.setMinRealWeightCoverage(0.6);
        v2.setEprelRequired(false);
        v2.setPositionLabels(List.of(new ImpactScorePositionLabel("excellent", 0.9),
                new ImpactScorePositionLabel("poor", 0.0)));
        v2.setCriteriaScoring(Map.of("A", new ScoreScoringConfig()));
        config.setV2(v2);
        return config;
    }

    @Test
    void versionOneIsTheDefaultAndExposesTheV1Methodology() {
        ImpactScoreConfig config = config();
        config.setMethodologyVersion(null);

        assertThat(new ImpactScoreConfig().effectiveMethodologyVersion()).isEqualTo(1);
        assertThat(config.effectiveMethodologyVersion()).isEqualTo(1);
        assertThat(config.isV2Active()).isFalse();
        assertThat(config.activeCriteriasPonderation()).containsOnlyKeys("A", "B", "DATA_QUALITY");
        assertThat(config.isEprelRequired()).as("always true in V1").isTrue();
        assertThat(config.minRealWeightCoverage()).isNull();
        assertThat(config.positionLabels()).isEmpty();
        assertThat(config.criteriaScoring("A")).isNull();
    }

    @Test
    void versionTwoExposesTheV2Block() {
        ImpactScoreConfig config = config();
        config.setMethodologyVersion(2);

        assertThat(config.effectiveMethodologyVersion()).isEqualTo(2);
        assertThat(config.activeCriteriasPonderation()).containsOnlyKeys("A", "B");
        assertThat(config.isEprelRequired()).isFalse();
        assertThat(config.minRealWeightCoverage()).isEqualTo(0.6);
        assertThat(config.positionLabels()).extracting(ImpactScorePositionLabel::getLevel)
                .containsExactly("excellent", "poor");
        assertThat(config.criteriaScoring("A")).isNotNull();
        assertThat(config.criteriaScoring("B")).isNull();
    }

    @Test
    void eprelIsRequiredByDefaultInV2() {
        ImpactScoreConfig config = config();
        config.getV2().setEprelRequired(null);
        config.setMethodologyVersion(2);
        assertThat(config.isEprelRequired()).isTrue();
        assertThat(new ImpactScoreV2Config().getEprelRequired()).isTrue();
    }

    @Test
    void environmentOverrideWinsOverTheFile() {
        ImpactScoreConfig config = config();
        config.setMethodologyVersion(2);
        config.setMethodologyVersionOverride(1);
        assertThat(config.effectiveMethodologyVersion()).isEqualTo(1);
        assertThat(config.activeCriteriasPonderation()).containsKey("DATA_QUALITY");

        config.setMethodologyVersion(1);
        config.setMethodologyVersionOverride(2);
        assertThat(config.effectiveMethodologyVersion()).isEqualTo(2);
        assertThat(config.activeCriteriasPonderation()).containsOnlyKeys("A", "B");
    }

    @Test
    void versionTwoWithoutV2BlockHasNoMatrix() {
        ImpactScoreConfig config = config();
        config.setV2(null);
        config.setMethodologyVersion(2);
        assertThat(config.activeCriteriasPonderation()).isEmpty();
        assertThat(config.isEprelRequired()).isTrue();
    }

    @Test
    void revisionIsStableAndFollowsTheActiveMatrix() {
        ImpactScoreConfig config = config();
        String v1 = config.getMethodologyRevision();
        assertThat(v1).matches("[0-9a-f]{8}");

        // Same matrix, other insertion order and number notation: same revision
        ImpactScoreConfig same = config();
        Map<String, Double> reordered = new java.util.LinkedHashMap<>();
        reordered.put("DATA_QUALITY", 0.20);
        reordered.put("B", 0.3);
        reordered.put("A", 0.5);
        same.setCriteriasPonderation(reordered);
        assertThat(same.getMethodologyRevision()).isEqualTo(v1);

        // The v2 block does not change the revision while V1 is active
        config.getV2().getCriteriasPonderation().put("A", 0.7);
        config.refreshMethodologyRevision();
        assertThat(config.getMethodologyRevision()).isEqualTo(v1);

        // A weight change or the switch to V2 does
        same.getCriteriasPonderation().put("A", 0.4);
        same.refreshMethodologyRevision();
        assertThat(same.getMethodologyRevision()).isNotEqualTo(v1);
        config.setMethodologyVersionOverride(2);
        assertThat(config.getMethodologyRevision()).isNotEqualTo(v1);
    }
}
