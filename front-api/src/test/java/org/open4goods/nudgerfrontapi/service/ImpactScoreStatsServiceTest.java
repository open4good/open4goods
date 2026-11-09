package org.open4goods.nudgerfrontapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.rating.ImpactScoreVerticalStats;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreStatsDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

/** GOU-1901: the method page figures come from the repository's cached aggregation, never invented. */
class ImpactScoreStatsServiceTest {

    private final VerticalsConfigService verticals = mock(VerticalsConfigService.class);
    private final ProductRepository repository = mock(ProductRepository.class);
    private final ImpactScoreStatsService service = new ImpactScoreStatsService(verticals, repository);

    @Test
    void mapsTheMeasuredFiguresOfTheCurrentMethodology() {
        VerticalConfig config = new VerticalConfig();
        config.setId("tv");
        config.setImpactScoreConfig(new ImpactScoreConfig());
        when(verticals.getConfigById("tv")).thenReturn(config);
        when(repository.impactScoreVerticalStats("tv", 1)).thenReturn(new ImpactScoreVerticalStats(
                100, 20, 5, 2, 1, 12, Map.of("ENERGY", 90L), Map.of(1, 100L), List.of(0L, 100L), 2,
                1.0, 2.0, 6.0, 9.0, 10.0, 1700000000000L));

        ImpactScoreStatsDto dto = service.stats("tv").orElseThrow();

        assertThat(dto.verticalId()).isEqualTo("tv");
        assertThat(dto.methodologyVersion()).isEqualTo(1);
        assertThat(dto.ratedCount()).isEqualTo(100);
        assertThat(dto.notRatedLowCoverageCount()).isEqualTo(20);
        assertThat(dto.notRatedNoCriteriaCount()).isEqualTo(5);
        assertThat(dto.brandCount()).isEqualTo(12);
        assertThat(dto.coverageByCriterion()).containsEntry("ENERGY", 90L);
        assertThat(dto.scoreHistogram()).containsExactly(0L, 100L);
        assertThat(dto.scoreMedian()).isEqualTo(6.0);
    }

    @Test
    void unknownVerticalOrWithoutImpactScoreHasNoFigures() {
        when(verticals.getConfigById("unknown")).thenReturn(null);
        VerticalConfig noScore = new VerticalConfig();
        noScore.setId("none");
        noScore.setImpactScoreConfig(null);
        when(verticals.getConfigById("none")).thenReturn(noScore);

        assertThat(service.stats("unknown")).isEmpty();
        assertThat(service.stats("none")).isEmpty();
        verifyNoInteractions(repository);
    }
}
