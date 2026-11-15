package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.brand.service.BrandService;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * GOU-2041: the V2 criterion "repairability or durability" of the tv and washing-machine matrices takes
 * {@code DURABILITY_INDEX} when it is filled in, else {@code REPAIRABILITY_INDEX}, on a fixed 0-10 scale, and is
 * absent, its weight redistributed, without any of them. Runs the real scoring chain, in production order, on the
 * real configurations and on one real product per vertical.
 */
class RepairabilityOrDurabilityCriterionTest {

    private static final String CRITERION = "REPAIRABILITY_OR_DURABILITY_INDEX";
    private static final String DURABILITY = "DURABILITY_INDEX";
    private static final String REPAIRABILITY = "REPAIRABILITY_INDEX";
    private static final String COMPOSITE = "REPARABILITY";

    private static final long DURABILITY_ONLY = 1L;
    private static final long REPAIRABILITY_ONLY = 2L;
    private static final long BOTH = 3L;
    private static final long NONE = 4L;
    private static final long UNPARSEABLE_DURABILITY = 5L;

    private static final String FIXTURES = "impactscore/repairability-or-durability-products.properties";

    static Stream<VerticalConfig> verticals() throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        return Stream.of("tv", "washing-machine").map(id -> {
            VerticalConfig vertical = service.getConfigById(id);
            assertThat(vertical).as("vertical %s", id).isNotNull();
            return vertical;
        });
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("verticals")
    void v2TakesTheDurabilityIndexElseTheRepairabilityIndexOnAFixedScale(VerticalConfig vertical) throws Exception {
        Properties fixtures = fixtures();
        long realId = Long.parseLong(fixtures.getProperty(vertical.getId() + ".gtin"));
        List<Product> products = cohort(vertical, fixtures, realId);

        withV2(vertical, () -> score(vertical, products));

        assertDerived(product(products, realId), Double.parseDouble(fixtures.getProperty(vertical.getId() + "."
                + REPAIRABILITY)), REPAIRABILITY);
        assertDerived(product(products, DURABILITY_ONLY), 7.0, DURABILITY);
        assertDerived(product(products, REPAIRABILITY_ONLY), 7.0, REPAIRABILITY);
        assertDerived(product(products, BOTH), 7.0, DURABILITY);
        assertDerived(product(products, UNPARSEABLE_DURABILITY), 6.0, REPAIRABILITY);

        // 7/10 gives the same score whatever the source
        assertThat(product(products, DURABILITY_ONLY).getScores().get(CRITERION).getRelativ().getValue())
                .isEqualTo(product(products, REPAIRABILITY_ONLY).getScores().get(CRITERION).getRelativ().getValue());

        // Neither index: the criterion is absent, never imputed, and its weight is redistributed
        Product none = product(products, NONE);
        Product real = product(products, realId);
        Map<String, Double> matrix = vertical.getImpactScoreConfig().getV2().getCriteriasPonderation();
        double share = matrix.get(CRITERION) / matrix.values().stream().mapToDouble(Double::doubleValue).sum();
        assertThat(none.getScores()).doesNotContainKey(CRITERION);
        assertThat(none.getImpactScoreMeta().getRealCriteria()).doesNotContain(CRITERION);
        assertThat(real.getImpactScoreMeta().getRealCriteria()).contains(CRITERION);
        assertThat(none.getImpactScoreMeta().getRealWeightCoverage())
                .isCloseTo(real.getImpactScoreMeta().getRealWeightCoverage() - share, within(1e-9));
        assertThat(none.getImpactScoreMeta().getStatus()).isEqualTo(ImpactScoreMeta.Status.RATED.name());

        Map<String, Double> weights = vertical.getImpactScoreConfig().getV2().getCriteriasPonderation();
        for (Product product : products) {
            assertThat(product.getImpactScoreMeta().getMethodologyVersion()).isEqualTo(2);
            assertThat(product.getImpactScoreMeta().getStatus()).as("status of %s", product.getId())
                    .isEqualTo(ImpactScoreMeta.Status.RATED.name());
            double realWeight = 0;
            double weightedSum = 0;
            for (String criterion : product.getImpactScoreMeta().getRealCriteria()) {
                realWeight += weights.get(criterion);
                weightedSum += weights.get(criterion) * product.getScores().get(criterion).getRelativ().getValue();
            }
            assertThat(product.ecoscore().getValue()).as("ImpactScore of %s", product.getId())
                    .isCloseTo(4.0 * weightedSum / realWeight, within(1e-9));
            assertThat(product.getImpactScoreMeta().getRealCriteria()).as("criteria of %s", product.getId())
                    .doesNotContain(REPAIRABILITY, DURABILITY);
        }

        // The composite follows the V2 criterion, not the replaced index
        Score composite = real.getScores().get(COMPOSITE);
        assertThat(composite).isNotNull();
        assertThat(composite.getAggregates()).containsKey(CRITERION).doesNotContainKey(REPAIRABILITY);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("verticals")
    void v1IsUnchanged(VerticalConfig vertical) throws Exception {
        Properties fixtures = fixtures();
        long realId = Long.parseLong(fixtures.getProperty(vertical.getId() + ".gtin"));
        List<Product> products = cohort(vertical, fixtures, realId);
        assertThat(vertical.getImpactScoreConfig().effectiveMethodologyVersion()).isEqualTo(1);

        score(vertical, products);

        assertThat(products).allSatisfy(product -> {
            assertThat(product.getScores()).as("scores of %s", product.getId()).doesNotContainKey(CRITERION);
            assertThat(product.ecoscore()).as("ImpactScore of %s", product.getId()).isNotNull();
        });
        Product real = product(products, realId);
        assertThat(real.getImpactScoreMeta().getRealCriteria()).contains(REPAIRABILITY).doesNotContain(CRITERION);
        assertThat(real.getScores().get(COMPOSITE).getAggregates()).containsKey(REPAIRABILITY)
                .doesNotContainKey(CRITERION);
    }

    private static void assertDerived(Product product, double raw, String source) {
        Score score = product.getScores().get(CRITERION);
        assertThat(score).as("criterion of product %s", product.getId()).isNotNull();
        assertThat(score.getVirtual()).as("virtual criterion of %s", product.getId()).isNotEqualTo(Boolean.TRUE);
        assertThat(score.getAbsolute().getValue()).as("raw value of %s", product.getId()).isEqualTo(raw);
        assertThat(score.getRelativ().getValue()).as("0-5 value of %s", product.getId())
                .isCloseTo(raw / 2.0, within(1e-9));
        assertThat(score.getMetadatas()).containsEntry(Attribute2ScoreAggregationService.SCORE_SOURCE_METADATA, source);
    }

    /** The real product, then copies of it with designed repairability and durability indices. */
    private static List<Product> cohort(VerticalConfig vertical, Properties fixtures, long realId) {
        String prefix = vertical.getId() + ".";
        List<Product> products = new ArrayList<>();
        products.add(realProduct(fixtures, prefix, realId));
        products.add(withIndices(realProduct(fixtures, prefix, DURABILITY_ONLY), "7", null));
        products.add(withIndices(realProduct(fixtures, prefix, REPAIRABILITY_ONLY), null, "7"));
        products.add(withIndices(realProduct(fixtures, prefix, BOTH), "7", "3"));
        products.add(withIndices(realProduct(fixtures, prefix, NONE), null, null));
        products.add(withIndices(realProduct(fixtures, prefix, UNPARSEABLE_DURABILITY), "non communiqué", "6"));
        return products;
    }

    private static Product realProduct(Properties fixtures, String prefix, long id) {
        Product product = new Product(id);
        fixtures.stringPropertyNames().stream()
                .filter(name -> name.startsWith(prefix) && !name.equals(prefix + "gtin"))
                .forEach(name -> {
                    String key = name.substring(prefix.length());
                    product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, fixtures.getProperty(name)));
                });
        return product;
    }

    private static Product withIndices(Product product, String durability, String repairability) {
        Map<String, IndexedAttribute> indexed = product.getAttributes().getIndexed();
        indexed.remove(DURABILITY);
        indexed.remove(REPAIRABILITY);
        if (durability != null) {
            indexed.put(DURABILITY, new IndexedAttribute(DURABILITY, durability));
        }
        if (repairability != null) {
            indexed.put(REPAIRABILITY, new IndexedAttribute(REPAIRABILITY, repairability));
        }
        return product;
    }

    private static void score(VerticalConfig vertical, List<Product> products) {
        Logger logger = LoggerFactory.getLogger(RepairabilityOrDurabilityCriterionTest.class);
        List<AbstractAggregationService> services = new ArrayList<>();
        services.add(new CleanScoreAggregationService(logger));
        services.add(new Attribute2ScoreAggregationService(logger));
        services.add(new BrandScoresAggregationService(logger, mock(BrandService.class)));
        services.add(new DataCompletion2ScoreAggregationService(logger));
        services.add(new EcoScoreAggregationService(logger));
        services.add(new ParticipatingScoresAggregationService(logger));
        new ScoringBatchedAggregator(services).score(products, vertical);
    }

    private static void withV2(VerticalConfig vertical, Runnable body) {
        ImpactScoreConfig config = vertical.getImpactScoreConfig();
        try {
            config.setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
            body.run();
        } finally {
            config.setMethodologyVersionOverride(null);
        }
    }

    private static Product product(List<Product> products, long id) {
        return products.stream().filter(product -> product.getId() == id).findFirst().orElseThrow();
    }

    private static Properties fixtures() throws Exception {
        Properties properties = new Properties();
        try (InputStream input = RepairabilityOrDurabilityCriterionTest.class.getClassLoader()
                .getResourceAsStream(FIXTURES)) {
            assertThat(input).as(FIXTURES).isNotNull();
            properties.load(new InputStreamReader(input, StandardCharsets.UTF_8));
        }
        return properties;
    }
}
