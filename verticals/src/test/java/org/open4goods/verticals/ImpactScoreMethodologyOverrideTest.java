package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-1882: the environment override {@code impactscore.methodology-version-overrides} (design
 * {@code docs/impactscore/v2-design.md}, section 5.2), parsed and applied at load time.
 */
class ImpactScoreMethodologyOverrideTest {

    private static VerticalsConfigService load(String overrides) {
        return new VerticalsConfigService(new SerialisationService(), mock(GoogleTaxonomyService.class),
                new PathMatchingResourcePatternResolver(), overrides);
    }

    @Test
    void blankOrNoneForcesNothing() {
        assertThat(ImpactScoreMethodology.parseOverrides(null)).isEmpty();
        assertThat(ImpactScoreMethodology.parseOverrides("  ")).isEmpty();
        assertThat(ImpactScoreMethodology.parseOverrides("none")).isEmpty();
    }

    @Test
    void pairsAreParsed() {
        assertThat(ImpactScoreMethodology.parseOverrides(" tv:2 , oven:1 ")).containsExactly(
                Map.entry("tv", 2), Map.entry("oven", 1));
    }

    @Test
    void malformedOverridesAreRefused() {
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv:")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv:two")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv:3"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("not one of");
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv:0")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> ImpactScoreMethodology.parseOverrides("tv:1,tv:2"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("twice");
    }

    @Test
    void versionOneOverrideIsApplied() {
        VerticalsConfigService service = load("tv:1");
        var impactScore = service.getConfigById("tv").getImpactScoreConfig();
        assertThat(impactScore.getMethodologyVersionOverride()).isEqualTo(1);
        assertThat(impactScore.effectiveMethodologyVersion()).isEqualTo(1);
        assertThat(impactScore.getMethodologyRevision()).isEqualTo(
                load(null).getConfigById("tv").getImpactScoreConfig().getMethodologyRevision());
    }

    @Test
    void versionTwoOverrideSelectsTheV2Block() {
        // Since GOU-1880 every vertical has a v2 block; without one, forcing V2 is refused
        // (VerticalYamlValidationTest.versionTwoWithoutV2BlockIsRefused)
        var impactScore = load("tv:2").getConfigById("tv").getImpactScoreConfig();
        assertThat(impactScore.effectiveMethodologyVersion()).isEqualTo(2);
        assertThat(impactScore.activeCriteriasPonderation()).isEqualTo(impactScore.getV2().getCriteriasPonderation())
                .doesNotContainKey("DATA_QUALITY");
        assertThat(impactScore.getMethodologyRevision()).isNotEqualTo(
                load(null).getConfigById("tv").getImpactScoreConfig().getMethodologyRevision());
    }

    @Test
    void unknownVerticalFailsTheStartup() {
        assertThatThrownBy(() -> load("not-a-vertical:1"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not-a-vertical");
    }

    @Test
    void outOfRangeVersionFailsTheStartup() {
        assertThatThrownBy(() -> load("tv:3")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void waveOneVerticalsAreInVersionTwo() {
        VerticalsConfigService service = load(null);
        var fridge = service.getConfigById("refrigerator").getImpactScoreConfig();
        var dishwasher = service.getConfigById("dishwasher").getImpactScoreConfig();
        assertThat(fridge.effectiveMethodologyVersion()).isEqualTo(2);
        assertThat(dishwasher.effectiveMethodologyVersion()).isEqualTo(2);
        assertThat(fridge.isEprelRequired()).isFalse();
        assertThat(dishwasher.isEprelRequired()).isTrue();
        assertThat(service.getConfigById("tv").getImpactScoreConfig().effectiveMethodologyVersion()).isEqualTo(1);
        assertThat(load("dishwasher:1").getConfigById("dishwasher").getImpactScoreConfig()
                .effectiveMethodologyVersion()).isEqualTo(1);
    }
}
