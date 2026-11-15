package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.ImpactScoreV2Config;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.LoggerFactory;

/**
 * GOU-1883: robust bounds of the consumption criteria (design {@code docs/impactscore/v2-design.md}, 2.6). The
 * attribute is {@code MINMAX_OBSERVED}; {@code v2.criteriaScoring} switches it to {@code MINMAX_QUANTILE}, in V2 only.
 */
class RobustBoundsV2Test {

    private static final String CRITERION = "POWER_CONSUMPTION_SDR";

    /** 100 ordinary values, 50 to 99 W, the 100th product being the one under test. */
    private static final int ORDINARY = 100;

    @Test
    void outlierDoesNotMoveTheOtherProductsInV2() {
        double[] withoutOutlier = relativ(cohort(Double.NaN), 2, true);
        double[] withOutlier = relativ(cohort(180.0), 2, true);

        for (int i = 0; i < ORDINARY; i++) {
            // only the rounding of the quantile index moves (101 values instead of 100)
            assertThat(withOutlier[i]).as("product %s", i).isCloseTo(withoutOutlier[i], within(0.1));
        }
    }

    @Test
    void outlierIsClampedAndLowerIsBetterIsRespected() {
        double[] relativ = relativ(cohort(180.0), 2, true);

        // lowest consumption, below the 2 % quantile: best note; highest: worst note
        assertThat(relativ[0]).isEqualTo(5.0);
        assertThat(relativ[ORDINARY]).as("outlier, clamped").isEqualTo(0.0);
        assertThat(relativ[ORDINARY - 1]).as("99 W, above the 98 % quantile").isEqualTo(0.0);
        assertThat(relativ[50]).as("mid cohort, between the bounds").isBetween(1.0, 4.0);
    }

    @Test
    void outlierShiftsTheOthersInV1() {
        double[] withoutOutlier = relativ(cohort(Double.NaN), 1, true);
        double[] withOutlier = relativ(cohort(180.0), 1, true);

        // the 180 W outlier stretches the observed max from 149 W to 180 W
        assertThat(Math.abs(withOutlier[50] - withoutOutlier[50])).as("MINMAX_OBSERVED, V1 unchanged").isGreaterThan(0.3);
    }

    @Test
    void overrideIsIgnoredWithoutV2Block() {
        double[] observed = relativ(cohort(180.0), 1, false);
        double[] v1WithBlock = relativ(cohort(180.0), 1, true);

        assertThat(v1WithBlock).containsExactly(observed);
    }

    /** NaN as outlier means no outlier product. Product 0 has the lowest value. */
    private static List<Product> cohort(double outlier) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < ORDINARY; i++) {
            products.add(product(i + 1, 50.0 + i));
        }
        if (!Double.isNaN(outlier)) {
            products.add(product(ORDINARY + 1, outlier));
        }
        return products;
    }

    private static double[] relativ(List<Product> products, int methodologyVersion, boolean withV2Block) {
        Attribute2ScoreAggregationService service = new Attribute2ScoreAggregationService(
                LoggerFactory.getLogger(RobustBoundsV2Test.class));
        VerticalConfig vertical = vertical(methodologyVersion, withV2Block);
        service.init(products);
        products.forEach(product -> service.onProduct(product, vertical));
        service.done(products, vertical);
        return products.stream().mapToDouble(p -> p.getScores().get(CRITERION).getRelativ().getValue()).toArray();
    }

    private static Product product(long id, double value) {
        Product product = new Product(id);
        product.getAttributes().getIndexed().put(CRITERION, new IndexedAttribute(CRITERION, String.valueOf(value)));
        return product;
    }

    private static VerticalConfig vertical(int methodologyVersion, boolean withV2Block) {
        ScoreScoringConfig scoring = new ScoreScoringConfig();
        scoring.getNormalization().setMethod(ScoreNormalizationMethod.MINMAX_OBSERVED);

        AttributeConfig attribute = new AttributeConfig();
        attribute.setKey(CRITERION);
        attribute.setAsScore(true);
        attribute.setImpactBetterIs(AttributeComparisonRule.LOWER);
        attribute.setUserBetterIs(AttributeComparisonRule.LOWER);
        attribute.setFilteringType(AttributeType.NUMERIC);
        attribute.setScoring(scoring);

        AttributesConfig attributes = new AttributesConfig();
        attributes.setConfigs(List.of(attribute));

        ImpactScoreConfig impactScore = new ImpactScoreConfig();
        impactScore.setMethodologyVersion(methodologyVersion);
        if (withV2Block) {
            ScoreScoringConfig override = new ScoreScoringConfig();
            override.getNormalization().setMethod(ScoreNormalizationMethod.MINMAX_QUANTILE);
            override.getNormalization().getParams().setQuantileLow(0.02);
            override.getNormalization().getParams().setQuantileHigh(0.98);
            ImpactScoreV2Config v2 = new ImpactScoreV2Config();
            v2.getCriteriaScoring().put(CRITERION, override);
            impactScore.setV2(v2);
        }

        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("tv");
        vertical.setAttributesConfig(attributes);
        vertical.setImpactScoreConfig(impactScore);
        return vertical;
    }
}
