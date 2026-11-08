package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeComparisonRule;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.AttributesConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;
import org.slf4j.LoggerFactory;

/**
 * Ranks must follow the real order of the values, including for extreme products whose normalized value is clamped
 * to the scale bounds (GOU-1894). Clamped products all share the same relativ value, so a ranking on relativ alone
 * orders them arbitrarily, by the order in which the batch received them.
 */
class RankingOfClampedScoresTest {

    private static final String CRITERION = "WARRANTY";

    /** 25 ordinary values, then three outliers far beyond two standard deviations, the best one first. */
    private static final double[] HIGH_OUTLIERS = { 50.0, 48.0, 49.0 };

    /** Same cohort with outliers far below, the lowest (best when lower is better) first. */
    private static final double[] LOW_OUTLIERS = { 1.0, 3.0, 2.0 };

    @Test
    void criterionRankFollowsTheRawValueWhenHigherIsBetter() {
        List<Product> products = criterionCohort(HIGH_OUTLIERS);

        rankCriterion(products, AttributeComparisonRule.GREATER);

        assertClampedTiesExist(products);
        assertRanksFollow(products, Comparator.comparingDouble(RankingOfClampedScoresTest::rawValue));
        assertThat(bestByRank(products).getScores().get(CRITERION).getValue()).isEqualTo(50.0);
    }

    @Test
    void criterionRankFollowsTheRawValueWhenLowerIsBetter() {
        List<Product> products = criterionCohort(LOW_OUTLIERS);

        rankCriterion(products, AttributeComparisonRule.LOWER);

        assertClampedTiesExist(products);
        assertRanksFollow(products, Comparator.comparingDouble(RankingOfClampedScoresTest::rawValue).reversed());
        assertThat(bestByRank(products).getScores().get(CRITERION).getValue()).isEqualTo(1.0);
    }

    @Test
    void impactScoreRankMatchesTheGlobalPositionForExtremeProducts() {
        List<Product> products = new ArrayList<>();
        long id = 100;
        // Outliers first and with the lowest GTINs, so that neither the input order nor a GTIN tie-break
        // can put the best product last by chance.
        for (double relativ : new double[] { 5.0, 4.8, 4.9 }) {
            products.add(productWithSubScore(id++, relativ));
        }
        for (int i = 0; i < 25; i++) {
            products.add(productWithSubScore(id++, 2.0 + i / 24.0));
        }

        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("tv");
        ImpactScoreConfig impactScoreConfig = new ImpactScoreConfig();
        impactScoreConfig.setCriteriasPonderation(Map.of(CRITERION, 1.0));
        vertical.setImpactScoreConfig(impactScoreConfig);

        EcoScoreAggregationService service = new EcoScoreAggregationService(
                LoggerFactory.getLogger(RankingOfClampedScoresTest.class));
        service.init(products);
        products.forEach(product -> service.onProduct(product, vertical));
        service.done(products, vertical);

        int count = products.size();
        for (Product product : products) {
            Score impactScore = product.ecoscore();
            assertThat(impactScore.getRanking()).as("rank of %s", product.getId())
                    .isEqualTo(count - product.getRanking().getGlobalPosition());
            assertThat(impactScore.getAbsolute().getCount()).isEqualTo(count);
        }
        Product best = products.getFirst();
        assertThat(best.getRanking().getGlobalPosition()).isEqualTo(1);
        assertThat(best.ecoscore().getRanking()).isEqualTo(count - 1);
    }

    private static void rankCriterion(List<Product> products, AttributeComparisonRule betterIs) {
        Attribute2ScoreAggregationService service = new Attribute2ScoreAggregationService(
                LoggerFactory.getLogger(RankingOfClampedScoresTest.class));
        VerticalConfig vertical = sigmaVertical(betterIs);
        service.init(products);
        products.forEach(product -> service.onProduct(product, vertical));
        service.done(products, vertical);
    }

    /** Outliers come first with the lowest GTINs, then 25 ordinary values between 20 and 30. */
    private static List<Product> criterionCohort(double[] outliers) {
        List<Product> products = new ArrayList<>();
        long id = 100;
        for (double outlier : outliers) {
            products.add(productWithAttribute(id++, outlier));
        }
        for (int i = 0; i < 25; i++) {
            products.add(productWithAttribute(id++, 20.0 + i * 10.0 / 24.0));
        }
        return products;
    }

    private static void assertClampedTiesExist(List<Product> products) {
        long distinctRelativs = products.stream()
                .map(p -> p.getScores().get(CRITERION).getRelativ().getValue())
                .distinct().count();
        assertThat(distinctRelativs).as("the cohort must contain clamped, tied relativ values")
                .isLessThan(products.size());
    }

    /** Ranks must be 0..n-1 in the given worst-to-best order of the raw values. */
    private static void assertRanksFollow(List<Product> products, Comparator<Product> worstToBest) {
        List<Product> expected = products.stream().sorted(worstToBest).toList();
        for (int i = 0; i < expected.size(); i++) {
            Product product = expected.get(i);
            assertThat(product.getScores().get(CRITERION).getRanking())
                    .as("rank of value %s", rawValue(product)).isEqualTo(i);
        }
    }

    private static Product bestByRank(List<Product> products) {
        return products.stream()
                .max(Comparator.comparingInt(p -> p.getScores().get(CRITERION).getRanking()))
                .orElseThrow();
    }

    private static double rawValue(Product product) {
        return product.getScores().get(CRITERION).getValue();
    }

    private static Product productWithAttribute(long id, double value) {
        Product product = new Product(id);
        product.getAttributes().getIndexed().put(CRITERION, new IndexedAttribute(CRITERION, String.valueOf(value)));
        return product;
    }

    private static Product productWithSubScore(long id, double relativ) {
        Product product = new Product(id);
        Score score = new Score(CRITERION, relativ);
        Cardinality relativCardinality = new Cardinality();
        relativCardinality.setValue(relativ);
        score.setRelativ(relativCardinality);
        product.getScores().put(CRITERION, score);
        return product;
    }

    private static VerticalConfig sigmaVertical(AttributeComparisonRule betterIs) {
        ScoreScoringConfig scoring = new ScoreScoringConfig();
        scoring.getNormalization().setMethod(ScoreNormalizationMethod.SIGMA);
        scoring.getNormalization().getParams().setSigmaK(2.0);

        AttributeConfig attribute = new AttributeConfig();
        attribute.setKey(CRITERION);
        attribute.setAsScore(true);
        attribute.setImpactBetterIs(betterIs);
        attribute.setUserBetterIs(betterIs);
        attribute.setFilteringType(AttributeType.NUMERIC);
        attribute.setScoring(scoring);

        AttributesConfig attributes = new AttributesConfig();
        attributes.setConfigs(List.of(attribute));
        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("tv");
        vertical.setAttributesConfig(attributes);
        return vertical;
    }
}
