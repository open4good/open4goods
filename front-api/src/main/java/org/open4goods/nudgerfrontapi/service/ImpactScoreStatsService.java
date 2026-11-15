package org.open4goods.nudgerfrontapi.service;

import java.util.Optional;

import org.open4goods.model.rating.ImpactScoreVerticalStats;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.nudgerfrontapi.dto.category.ImpactScoreStatsDto;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.stereotype.Service;

/**
 * Measured figures of the ImpactScore of a vertical (counts, coverage per criterion, distribution of the scores) for
 * its method page. The aggregation is computed once an hour by the repository from the batch's per-product assessment,
 * never per request.
 */
@Service
public class ImpactScoreStatsService {

    private final VerticalsConfigService verticalsConfigService;
    private final ProductRepository productRepository;

    public ImpactScoreStatsService(VerticalsConfigService verticalsConfigService, ProductRepository productRepository) {
        this.verticalsConfigService = verticalsConfigService;
        this.productRepository = productRepository;
    }

    /**
     * Figures of the vertical's current methodology.
     *
     * @param verticalId vertical identifier
     * @return the figures, empty when the vertical is unknown or has no ImpactScore configuration
     */
    public Optional<ImpactScoreStatsDto> stats(String verticalId) {
        VerticalConfig vertical = verticalsConfigService.getConfigById(verticalId);
        if (vertical == null || vertical.getImpactScoreConfig() == null) {
            return Optional.empty();
        }
        ImpactScoreConfig config = vertical.getImpactScoreConfig();
        int version = config.effectiveMethodologyVersion();
        ImpactScoreVerticalStats stats = productRepository.impactScoreVerticalStats(vertical.getId(), version);
        return Optional.of(new ImpactScoreStatsDto(
                vertical.getId(),
                version,
                stats.ratedCount(),
                stats.notRatedLowCoverageCount(),
                stats.notRatedNoCriteriaCount(),
                stats.outdatedCount(),
                stats.excludedCount(),
                stats.brandCount(),
                stats.coverageByCriterion(),
                stats.realCriteriaCountDistribution(),
                stats.scoreHistogram(),
                stats.scoreBandWidth(),
                stats.scoreMin(),
                stats.scoreP5(),
                stats.scoreMedian(),
                stats.scoreP95(),
                stats.scoreMax(),
                stats.lastScoredAt()));
    }
}
