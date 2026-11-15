package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.services.batch.scores.EcoScoreAggregationService.Coverage;
import org.open4goods.model.product.EcoScoreRanking;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.LoggerFactory;

/**
 * GOU-1879: ImpactScore methodology V2 on a synthetic matrix (design {@code docs/impactscore/v2-design.md}, 2.2 and
 * 2.4). Weights A 0.5, B 0.3, C 0.2, D 0 and a coverage threshold of 0.5.
 */
class EcoScoreAggregationServiceV2Test {

    private static final double THRESHOLD = 0.5;

    @Test
    void weightsAreRedistributedOverTheRealCriteria() {
        Product complete = product(1L, Map.of("A", 4.0, "B", 2.0, "C", 1.0));
        Product heavyOnly = product(2L, Map.of("A", 3.0));

        score(List.of(complete, heavyOnly));

        // 4 x (0.5 x 4 + 0.3 x 2 + 0.2 x 1) / 1
        assertRated(complete, 11.2, 1.0, 3);
        assertThat(complete.getImpactScoreMeta().getRealCriteria()).as("matrix order").containsExactly("A", "B", "C");
        // A alone: its weight is the whole real weight
        assertRated(heavyOnly, 12.0, 0.5, 1);
        assertThat(heavyOnly.getImpactScoreMeta().getRealCriteria()).containsExactly("A");
    }

    @Test
    void coverageThresholdIsInclusive() {
        Product atThreshold = product(1L, Map.of("B", 5.0, "C", 5.0));

        score(List.of(atThreshold));

        assertRated(atThreshold, 20.0, 0.5, 2);
    }

    @Test
    void productBelowTheThresholdIsNotRated() {
        Product below = product(1L, Map.of("B", 5.0));
        EcoScoreRanking stale = new EcoScoreRanking();
        stale.setGlobalPosition(7);
        stale.setGlobalCount(10);
        below.setRanking(stale);
        Product rated = product(2L, Map.of("A", 2.0));

        score(List.of(below, rated));

        assertThat(below.ecoscore()).as("no ImpactScore, not even virtual").isNull();
        assertThat(below.getScores()).as("real sub-scores are kept").containsKey("B");
        assertThat(below.getRanking().getGlobalPosition()).isZero();
        assertThat(below.getRanking().getGlobalCount()).isZero();
        assertThat(below.getRanking().getGlobalBest()).isNull();
        ImpactScoreMeta meta = below.getImpactScoreMeta();
        assertThat(meta.getStatus()).isEqualTo(ImpactScoreMeta.Status.NOT_RATED.name());
        assertThat(meta.getNotRatedReason()).isEqualTo(ImpactScoreMeta.NotRatedReason.LOW_COVERAGE.name());
        assertThat(meta.getRealWeightCoverage()).isCloseTo(0.3, within(1e-9));
        assertThat(meta.getRealCriteriaCount()).isEqualTo(1);
        assertThat(meta.getRealCriteria()).as("real criteria, even not rated").containsExactly("B");
        assertThat(meta.getTotalCriteriaCount()).isEqualTo(3);
        assertThat(rated.getRanking().getGlobalCount()).isEqualTo(1);
    }

    @Test
    void productWithoutRealCriterionIsNotRated() {
        Product none = product(1L, Map.of("D", 4.0));
        Score imputed = score("A", 4.0);
        imputed.setVirtual(true);
        none.getScores().put("A", imputed);
        Score unreadable = new Score("B", null);
        none.getScores().put("B", unreadable);

        score(List.of(none));

        assertThat(none.ecoscore()).isNull();
        ImpactScoreMeta meta = none.getImpactScoreMeta();
        assertThat(meta.getStatus()).isEqualTo(ImpactScoreMeta.Status.NOT_RATED.name());
        assertThat(meta.getNotRatedReason()).isEqualTo(ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA.name());
        assertThat(meta.getRealWeightCoverage()).isZero();
        assertThat(meta.getRealCriteriaCount()).isZero();
        assertThat(meta.getRealCriteria()).isEmpty();
        // D, weighted 0, is not a criterion
        assertThat(meta.getTotalCriteriaCount()).isEqualTo(3);
    }

    @Test
    void onlyRatedProductsAreRankedAndTiesShareARank() {
        Product worst = product(1L, Map.of("A", 4.0, "B", 2.0, "C", 1.0));
        Product tiedLow = product(2L, Map.of("A", 3.0));
        Product best = product(3L, Map.of("B", 5.0, "C", 5.0));
        Product notRated = product(4L, Map.of("C", 5.0));
        Product tiedHigh = product(6L, Map.of("A", 3.0));

        score(List.of(tiedHigh, best, notRated, worst, tiedLow));

        // Rated: worst 11.2, tiedLow and tiedHigh 12, best 20
        assertRanking(worst, 0, 4, tiedLow.getId());
        assertRanking(tiedLow, 1, 2, best.getId());
        assertRanking(tiedHigh, 1, 2, best.getId());
        assertRanking(best, 3, 1, null);
        for (Product product : List.of(worst, tiedLow, tiedHigh, best)) {
            assertThat(product.getRanking().getGlobalCount()).isEqualTo(4);
            assertThat(product.getRanking().getGlobalBest()).isEqualTo(best.getId());
            assertThat(product.ecoscore().getAbsolute().getCount()).isEqualTo(4);
            assertThat(product.ecoscore().getRelativ().getCount()).isEqualTo(4);
        }
        assertThat(notRated.ecoscore()).isNull();
        assertThat(notRated.getRanking().getGlobalPosition()).isZero();
    }

    @Test
    void storedSubScoresAreNotRecomputedInV2() {
        Product product = product(1L, Map.of("A", 4.0));

        assertThat(service().recomputeFromStoredSubScores(product, vertical())).isNull();
    }

    @Test
    void coverageReasonOnTheThresholdBounds() {
        // Rounding below an inclusive threshold stays rated
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(0.3 - 1e-12, 1.0, 1, 3, 1.0, List.of()), 0.3))
                .isNull();
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(0.3 - 1e-6, 1.0, 1, 3, 1.0, List.of()), 0.3))
                .isEqualTo(ImpactScoreMeta.NotRatedReason.LOW_COVERAGE);
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(1.0, 1.0, 3, 3, 1.0, List.of()), 1.0)).isNull();
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(0.0, 1.0, 0, 3, 0.0, List.of()), 0.3))
                .isEqualTo(ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA);
        // No threshold: only C = 0 is not rated
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(0.01, 1.0, 1, 3, 1.0, List.of()), null)).isNull();
        assertThat(EcoScoreAggregationService.v2NotRatedReason(new Coverage(0.0, 1.0, 0, 3, 0.0, List.of()), null))
                .isEqualTo(ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA);
    }

    private static void assertRated(Product product, double expected, double coverage, int realCount) {
        Score impactScore = product.ecoscore();
        assertThat(impactScore).as("ImpactScore of %s", product.getId()).isNotNull();
        assertThat(impactScore.getVirtual()).isFalse();
        assertThat(impactScore.getValue()).isCloseTo(expected, within(1e-9));
        assertThat(impactScore.getAbsolute().getValue()).isEqualTo(impactScore.getValue());
        assertThat(impactScore.getRelativ().getValue()).isCloseTo(expected / 4.0, within(1e-9));

        ImpactScoreMeta meta = product.getImpactScoreMeta();
        assertThat(meta.getMethodologyVersion()).isEqualTo(2);
        assertThat(meta.getStatus()).isEqualTo(ImpactScoreMeta.Status.RATED.name());
        assertThat(meta.getNotRatedReason()).isNull();
        assertThat(meta.getRealWeightCoverage()).isCloseTo(coverage, within(1e-9));
        assertThat(meta.getRealCriteriaCount()).isEqualTo(realCount);
        assertThat(meta.getRealCriteria()).hasSize(realCount);
        assertThat(meta.getTotalCriteriaCount()).isEqualTo(3);
    }

    private static void assertRanking(Product product, int ranking, long position, Long better) {
        assertThat(product.ecoscore().getRanking()).as("ranking of %s", product.getId()).isEqualTo(ranking);
        assertThat(product.getRanking().getGlobalPosition()).as("position of %s", product.getId()).isEqualTo(position);
        assertThat(product.getRanking().getGlobalBetter()).as("better than %s", product.getId()).isEqualTo(better);
    }

    private static void score(List<Product> products) {
        EcoScoreAggregationService service = service();
        VerticalConfig vertical = vertical();
        service.init(products);
        products.forEach(product -> service.onProduct(product, vertical));
        service.done(products, vertical);
    }

    private static EcoScoreAggregationService service() {
        return new EcoScoreAggregationService(LoggerFactory.getLogger(EcoScoreAggregationServiceV2Test.class));
    }

    private static VerticalConfig vertical() {
        Map<String, Double> weights = new LinkedHashMap<>();
        weights.put("A", 0.5);
        weights.put("B", 0.3);
        weights.put("C", 0.2);
        weights.put("D", 0.0);
        ImpactScoreV2Config v2 = new ImpactScoreV2Config();
        v2.setCriteriasPonderation(weights);
        v2.setMinRealWeightCoverage(THRESHOLD);

        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of("A", 0.4, "B", 0.4, "C", 0.2));
        impactScoreConfig.setV2(v2);
        impactScoreConfig.setMethodologyVersion(ImpactScoreConfig.METHODOLOGY_V2);

        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("v2-test");
        vertical.setImpactScoreConfig(impactScoreConfig);
        return vertical;
    }

    private static Product product(long id, Map<String, Double> relativeValues) {
        Product product = new Product(id);
        relativeValues.forEach((name, value) -> product.getScores().put(name, score(name, value)));
        return product;
    }

    private static Score score(String name, double relativeValue) {
        Score score = new Score(name, relativeValue);
        Cardinality relativ = new Cardinality();
        relativ.setValue(relativeValue);
        score.setRelativ(relativ);
        return score;
    }
}
