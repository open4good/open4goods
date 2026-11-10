package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.api.services.aggregation.aggregator.ScoringBatchedAggregator;
import org.open4goods.brand.model.Brand;
import org.open4goods.brand.model.Company;
import org.open4goods.brand.model.CompanyScore;
import org.open4goods.brand.model.ScoreScale;
import org.open4goods.brand.service.BrandService;
import org.open4goods.commons.services.BarcodeForensicsService;
import org.open4goods.commons.services.BarcodeValidationService;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.commons.services.Gs1PrefixService;
import org.open4goods.commons.services.textgen.BlablaService;
import org.open4goods.icecat.services.IcecatFeatureResolver;
import org.open4goods.icecat.services.IcecatService;
import org.open4goods.model.attribute.AttributeType;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.ImpactScoreConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

/**
 * Runs the real scoring chain on every real vertical configuration, in production service order.
 *
 * <p>Service order and vertical YAML are what make these invariants fail in practice: a composite
 * declared under the name {@code ECOSCORE} replaced the weighted 0-20 score of air conditioners with
 * a 0-5 sub-score, while every unit test built on synthetic configurations stayed green.
 */
class ImpactScoreRealVerticalsTest {

    private static final int PRODUCT_COUNT = 5;

    private static final String DATA_QUALITY = "DATA_QUALITY";

    /** Coverage threshold of a V2 test while the vertical has none yet (design, section 7). */
    private static final double DEFAULT_V2_THRESHOLD = 0.5;

    /** Ids of the designed V2 products: two tied products, then the one just below and just above the threshold. */
    private static final long TIED = 1000L;
    private static final long JUST_BELOW = 2003L;
    private static final long JUST_ABOVE = 2004L;

    /**
     * ImpactScore, rank and position of {@link #products(VerticalConfig)} on every real vertical, recorded with the
     * code before GOU-1882. Record again with {@code -Dimpactscore.snapshot.record=true} only for an intended V1
     * change.
     */
    private static final String V1_SNAPSHOT = "impactscore/v1-snapshot.properties";

    static Stream<VerticalConfig> realVerticals() throws Exception {
        VerticalsConfigService service = new VerticalsConfigService(new SerialisationService(),
                mock(GoogleTaxonomyService.class), new PathMatchingResourcePatternResolver());
        List<VerticalConfig> verticals = new ArrayList<>(service.getConfigsWithoutDefault());
        assertThat(verticals).isNotEmpty();
        return verticals.stream();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void impactScoreIsTheWeightedSumOnTheTwentyPointScale(VerticalConfig vertical, @TempDir Path logs)
            throws Exception {
        List<Product> products = products(vertical);

        aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

        for (Product product : products) {
            Score impactScore = product.ecoscore();
            assertThat(impactScore).as("ImpactScore of product %s", product.getId()).isNotNull();
            assertThat(impactScore.getVirtual()).isFalse();
            assertThat(impactScore.getAggregates()).as("ImpactScore must not be a composite").isEmpty();
            assertThat(impactScore.getValue()).isBetween(0.0, 20.0);
            assertThat(impactScore.getAbsolute().getValue()).isEqualTo(impactScore.getValue());
            assertThat(impactScore.getRelativ().getValue()).isCloseTo(impactScore.getValue() / 4.0, within(1e-9));
        }
    }

    /**
     * GOU-1882: in methodology version 1 too, the batch records how it assessed each product (design
     * {@code docs/impactscore/v2-design.md}, 1.1): version 1, revision of the matrix, date and coverage C of the V1
     * matrix, for information.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v1AssessmentMetadataIsRecorded(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        List<Product> products = products(vertical);
        long before = System.currentTimeMillis();

        aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

        Map<String, Double> weights = vertical.getImpactScoreConfig().activeCriteriasPonderation();
        long weighted = weights.values().stream().filter(weight -> weight != null && weight > 0).count();
        for (Product product : products) {
            ImpactScoreMeta meta = product.getImpactScoreMeta();
            assertThat(meta).as("impactScoreMeta of product %s", product.getId()).isNotNull();
            assertThat(meta.getMethodologyVersion()).isEqualTo(1);
            assertThat(meta.getMethodologyRevision()).isEqualTo(vertical.getImpactScoreConfig().getMethodologyRevision())
                    .matches("[0-9a-f]{8}");
            assertThat(meta.getScoredAt()).isBetween(before, System.currentTimeMillis());
            assertThat(meta.getStatus()).isEqualTo(ImpactScoreMeta.Status.RATED.name());
            assertThat(meta.getNotRatedReason()).isNull();
            assertThat(meta.getTotalCriteriaCount()).isEqualTo((int) weighted);
            assertThat(meta.getRealCriteriaCount()).isBetween(1, meta.getTotalCriteriaCount());

            double total = 0;
            double real = 0;
            for (Map.Entry<String, Double> weight : weights.entrySet()) {
                if (weight.getValue() == null || weight.getValue() <= 0) {
                    continue;
                }
                total += weight.getValue();
                Score score = product.getScores().get(weight.getKey());
                if (score != null && !Boolean.TRUE.equals(score.getVirtual()) && score.getRelativ() != null
                        && score.getRelativ().getValue() != null) {
                    real += weight.getValue();
                }
            }
            assertThat(meta.getRealWeightCoverage()).as("C of product %s", product.getId())
                    .isCloseTo(real / total, within(1e-9));
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void bestProductHasTheHighestRankAndTheFirstPosition(VerticalConfig vertical, @TempDir Path logs)
            throws Exception {
        List<Product> products = products(vertical);

        aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

        Product best = products.stream().max(Comparator.comparingDouble(p -> p.ecoscore().getValue())).orElseThrow();
        assertThat(best.ecoscore().getRanking()).isEqualTo(PRODUCT_COUNT - 1);
        assertThat(best.getRanking().getGlobalPosition()).isEqualTo(1);
    }

    /**
     * GOU-1882: in methodology version 1, the scores are those of the code before the V2 foundation.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v1ScoresMatchTheSnapshot(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        List<Product> products = products(vertical);

        aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

        Map<String, String> actual = new TreeMap<>();
        for (Product product : products) {
            Score impactScore = product.ecoscore();
            actual.put(vertical.getId() + "." + product.getId(), impactScore.getValue() + "|" + impactScore.getRanking()
                    + "|" + product.getRanking().getGlobalPosition() + "|" + product.getRanking().getGlobalCount());
        }

        if (Boolean.getBoolean("impactscore.snapshot.record")) {
            Path file = Path.of("src/test/resources", V1_SNAPSHOT);
            Properties recorded = new Properties();
            if (Files.exists(file)) {
                try (InputStream in = Files.newInputStream(file)) {
                    recorded.load(in);
                }
            }
            recorded.putAll(actual);
            Files.createDirectories(file.getParent());
            try (Writer out = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                new TreeMap<>(recorded).forEach((key, value) -> {
                    try {
                        out.write(key + "=" + value + "\n");
                    } catch (java.io.IOException e) {
                        throw new java.io.UncheckedIOException(e);
                    }
                });
            }
            return;
        }

        Properties expected = new Properties();
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(V1_SNAPSHOT)) {
            assertThat(in).as("snapshot %s", V1_SNAPSHOT).isNotNull();
            expected.load(in);
        }
        actual.forEach((key, value) -> assertThat(value).as("ImpactScore|rank|position|count of %s", key)
                .isEqualTo(expected.getProperty(key)));
    }

    /**
     * GOU-1897 point 5: products with the same ImpactScore must keep the same position whatever the order in which
     * the batch received them. Ties are broken by GTIN.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void tiedProductsKeepTheSamePositionWhateverTheBatchOrder(VerticalConfig vertical, @TempDir Path logs)
            throws Exception {
        List<Product> forward = tiedProducts(vertical);
        List<Product> backward = tiedProducts(vertical);
        java.util.Collections.reverse(backward);

        aggregationFacadeService(logs).getScoringAggregator().score(forward, vertical);
        aggregationFacadeService(logs).getScoringAggregator().score(backward, vertical);

        for (Product product : forward) {
            Product twin = backward.stream().filter(p -> p.getId().equals(product.getId())).findFirst().orElseThrow();
            assertThat(twin.getRanking().getGlobalPosition()).as("position of %s", product.getId())
                    .isEqualTo(product.getRanking().getGlobalPosition());
            assertThat(twin.ecoscore().getRanking()).isEqualTo(product.ecoscore().getRanking());
        }
        assertThat(forward.stream().map(p -> p.getRanking().getGlobalPosition()).distinct().count())
                .as("every tied product gets its own position").isEqualTo(PRODUCT_COUNT);
    }

    /**
     * GOU-1880: in V2, {@code DATA_QUALITY} is out of the ImpactScore (design {@code docs/impactscore/v2-design.md},
     * 2.3). Forcing every product's {@code DATA_QUALITY} to the worst value between its computation and the
     * ImpactScore leaves the V2 score unchanged, while it does change the V1 score (proof that the change bites).
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2DataQualityDoesNotChangeTheImpactScore(VerticalConfig vertical) {
        assertThat(vertical.getImpactScoreConfig().getV2()).as("v2 block of %s", vertical.getId()).isNotNull();
        assertThat(vertical.getImpactScoreConfig().getV2().getCriteriasPonderation()).doesNotContainKey(DATA_QUALITY);

        try {
            vertical.getImpactScoreConfig().setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
            assertThat(impactScores(vertical, false)).as("V2 ImpactScores of %s with DATA_QUALITY at worst", vertical.getId())
                    .isEqualTo(impactScores(vertical, true));
        } finally {
            vertical.getImpactScoreConfig().setMethodologyVersionOverride(null);
        }
        assertThat(impactScores(vertical, false)).as("V1 ImpactScores of %s with DATA_QUALITY at worst", vertical.getId())
                .isNotEqualTo(impactScores(vertical, true));
    }

    /**
     * GOU-1880: in V2, {@code DATA_QUALITY} counts only the real scores of the ImpactScore criteria
     * ({@code availableImpactScoreCriterias}, {@code asScore}), without the brand scores, itself or composites. V1
     * keeps counting every real score, brand scores included.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2DataQualityDoesNotCountBrandScores(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        BrandService brandService = brandServiceWithTwoScores();

        List<Product> v2Products = brandedProducts(vertical);
        try {
            vertical.getImpactScoreConfig().setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
            aggregationFacadeService(logs, brandService).getScoringAggregator().score(v2Products, vertical);
        } finally {
            vertical.getImpactScoreConfig().setMethodologyVersionOverride(null);
        }
        List<Product> v1Products = brandedProducts(vertical);
        aggregationFacadeService(logs, brandService).getScoringAggregator().score(v1Products, vertical);

        for (int i = 0; i < PRODUCT_COUNT; i++) {
            Product v2 = v2Products.get(i);
            long brandScores = v2.getScores().values().stream()
                    .filter(score -> !Boolean.TRUE.equals(score.getVirtual()))
                    .filter(score -> score.getName().startsWith("BRAND_"))
                    .count();
            // The two company scores, plus those built from the BRAND_* attributes of the test products
            assertThat(v2.getScores()).containsKeys("BRAND_CDP_SCORING", "BRAND_GOOD_ON_YOU_SCORING");
            assertThat(brandScores).as("real brand scores of product %s", v2.getId()).isGreaterThanOrEqualTo(2);

            long criteria = v2.getScores().values().stream()
                    .filter(score -> !Boolean.TRUE.equals(score.getVirtual()))
                    .filter(score -> isV2DataQualityCriterion(vertical, score.getName()))
                    .count();
            assertThat(v2.getScores().get(DATA_QUALITY).getAbsolute().getValue())
                    .as("V2 DATA_QUALITY count of product %s", v2.getId()).isEqualTo((double) criteria);

            Product v1 = v1Products.get(i);
            // ECOSCORE and the other composites are computed after DATA_QUALITY
            long everyRealScore = v1.getScores().values().stream()
                    .filter(score -> !Boolean.TRUE.equals(score.getVirtual()))
                    .filter(score -> !DATA_QUALITY.equals(score.getName()))
                    .filter(score -> !vertical.getCompositeScores().contains(score.getName()))
                    .count();
            assertThat(v1.getScores().get(DATA_QUALITY).getAbsolute().getValue())
                    .as("V1 DATA_QUALITY count of product %s", v1.getId()).isEqualTo((double) everyRealScore);
        }
    }

    /**
     * GOU-1879, invariants 1 to 3 of the design (section 7): in V2, no score is imputed, C is exact, the score is
     * {@code 4 x sum(w x rel) / sum(w)} over the real criteria and a product below the threshold has no ImpactScore,
     * with the right reason.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2ScoresTheRealCriteriaAboveTheCoverageThreshold(VerticalConfig vertical, @TempDir Path logs)
            throws Exception {
        withV2(vertical, threshold -> {
            Map<Long, Set<String>> designed = new TreeMap<>();
            List<Product> products = v2Products(vertical, threshold, designed);
            long before = System.currentTimeMillis();

            aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

            Map<String, Double> weights = v2Weights(vertical);
            double totalWeight = weights.values().stream().mapToDouble(Double::doubleValue).sum();
            for (Product product : products) {
                assertThat(product.getScores().values()).as("scores of product %s", product.getId())
                        .noneMatch(score -> Boolean.TRUE.equals(score.getVirtual()));

                Set<String> real = new TreeSet<>();
                double realWeight = 0;
                double weightedSum = 0;
                for (Map.Entry<String, Double> weight : weights.entrySet()) {
                    Score score = product.getScores().get(weight.getKey());
                    if (score != null && score.getRelativ() != null && score.getRelativ().getValue() != null) {
                        real.add(weight.getKey());
                        realWeight += weight.getValue();
                        weightedSum += weight.getValue() * score.getRelativ().getValue();
                    }
                }
                assertThat(real).as("real criteria of product %s", product.getId())
                        .isEqualTo(designed.get(product.getId()));

                ImpactScoreMeta meta = product.getImpactScoreMeta();
                assertThat(meta.getMethodologyVersion()).isEqualTo(2);
                assertThat(meta.getMethodologyRevision())
                        .isEqualTo(vertical.getImpactScoreConfig().getMethodologyRevision());
                assertThat(meta.getScoredAt()).isBetween(before, System.currentTimeMillis());
                assertThat(meta.getRealWeightCoverage()).as("C of product %s", product.getId())
                        .isCloseTo(realWeight / totalWeight, within(1e-9));
                assertThat(meta.getRealCriteriaCount()).isEqualTo(real.size());
                assertThat(meta.getRealCriteria()).as("real criteria of product %s, even not rated", product.getId())
                        .containsExactlyInAnyOrderElementsOf(real);
                assertThat(meta.getTotalCriteriaCount()).isEqualTo(weights.size());

                Score impactScore = product.ecoscore();
                if (real.isEmpty()) {
                    assertNotRated(product, ImpactScoreMeta.NotRatedReason.NO_REAL_CRITERIA);
                } else if (realWeight / totalWeight < threshold - EcoScoreAggregationService.COVERAGE_TOLERANCE) {
                    assertNotRated(product, ImpactScoreMeta.NotRatedReason.LOW_COVERAGE);
                } else {
                    assertThat(meta.getStatus()).isEqualTo(ImpactScoreMeta.Status.RATED.name());
                    assertThat(meta.getNotRatedReason()).isNull();
                    assertThat(impactScore).as("ImpactScore of product %s", product.getId()).isNotNull();
                    assertThat(impactScore.getValue()).as("ImpactScore of product %s", product.getId())
                            .isCloseTo(4.0 * weightedSum / realWeight, within(1e-9));
                    assertThat(impactScore.getValue()).isBetween(0.0, 20.0);
                    assertThat(impactScore.getAbsolute().getValue()).isEqualTo(impactScore.getValue());
                    assertThat(impactScore.getRelativ().getValue()).isCloseTo(impactScore.getValue() / 4.0,
                            within(1e-9));
                }
            }

            // The designed cohort reaches every outcome
            assertThat(products).extracting(product -> product.getImpactScoreMeta().getStatus() + "/"
                    + product.getImpactScoreMeta().getNotRatedReason())
                    .contains("RATED/null", "NOT_RATED/NO_REAL_CRITERIA", "NOT_RATED/LOW_COVERAGE");
            assertThat(status(products, JUST_BELOW)).isEqualTo("NOT_RATED");
            assertThat(status(products, JUST_ABOVE)).isEqualTo("RATED");
        });
    }

    /**
     * GOU-1879, invariant 3 of the design: the threshold is inclusive. With the threshold set to the coverage of a
     * product, that product is rated.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2CoverageThresholdIsInclusive(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        withV2(vertical, threshold -> {
            Map<Long, Set<String>> designed = new TreeMap<>();
            List<Product> products = v2Products(vertical, threshold, designed);
            Map<String, Double> weights = v2Weights(vertical);
            double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();
            double atThreshold = designed.get(JUST_ABOVE).stream().mapToDouble(weights::get).sum() / total;

            vertical.getImpactScoreConfig().getV2().setMinRealWeightCoverage(atThreshold);
            aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

            Product product = products.stream().filter(p -> p.getId() == JUST_ABOVE).findFirst().orElseThrow();
            assertThat(product.getImpactScoreMeta().getStatus()).as("C %s at threshold %s",
                    product.getImpactScoreMeta().getRealWeightCoverage(), atThreshold)
                    .isEqualTo(ImpactScoreMeta.Status.RATED.name());
            assertThat(product.ecoscore()).isNotNull();
        });
    }

    /**
     * GOU-1879, invariant 4 of the design: only rated products are ranked; {@code count} is their number, the best one
     * has {@code ranking = count - 1} and position 1, and tied products share a rank.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2RanksOnlyRatedProductsAndTiesShareARank(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        withV2(vertical, threshold -> {
            List<Product> products = v2Products(vertical, threshold, new TreeMap<>());

            aggregationFacadeService(logs).getScoringAggregator().score(products, vertical);

            List<Product> rated = products.stream().filter(product -> product.ecoscore() != null).toList();
            int count = rated.size();
            assertThat(count).isGreaterThan(2).isLessThan(products.size());
            for (Product product : rated) {
                double value = product.ecoscore().getValue();
                long worse = rated.stream().filter(other -> other.ecoscore().getValue() < value).count();
                long better = rated.stream().filter(other -> other.ecoscore().getValue() > value).count();
                assertThat(product.ecoscore().getRanking()).as("ranking of %s", product.getId()).isEqualTo((int) worse);
                assertThat(product.getRanking().getGlobalPosition()).as("position of %s", product.getId())
                        .isEqualTo(1 + better);
                assertThat(product.getRanking().getGlobalCount()).isEqualTo(count);
                assertThat(product.ecoscore().getAbsolute().getCount()).isEqualTo(count);
            }
            Product best = rated.stream().max(Comparator.comparingDouble(p -> p.ecoscore().getValue())).orElseThrow();
            // count - 1 when the best product is alone at the top; products tied with it share its rank
            long tiedWithBest = rated.stream()
                    .filter(other -> other.ecoscore().getValue().equals(best.ecoscore().getValue())).count();
            assertThat(best.ecoscore().getRanking()).isEqualTo((int) (count - tiedWithBest));
            assertThat(best.getRanking().getGlobalPosition()).isEqualTo(1);

            Product tie = products.stream().filter(p -> p.getId() == TIED).findFirst().orElseThrow();
            Product twin = products.stream().filter(p -> p.getId() == TIED + 1).findFirst().orElseThrow();
            assertThat(twin.ecoscore().getRanking()).isEqualTo(tie.ecoscore().getRanking());
            assertThat(twin.getRanking().getGlobalPosition()).isEqualTo(tie.getRanking().getGlobalPosition());

            products.stream().filter(product -> product.ecoscore() == null).forEach(product -> {
                assertThat(product.getRanking().getGlobalPosition()).as("position of %s", product.getId()).isZero();
                assertThat(product.getRanking().getGlobalCount()).isZero();
            });
        });
    }

    /**
     * GOU-1879: {@code POST /score/{vertical}} follows the batch rules in V2: a product without offers is not
     * recomputed, it keeps its stored score and is not indexed again.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("realVerticals")
    void v2ScoreEndpointScoresOnlyProductsWithOffers(VerticalConfig vertical, @TempDir Path logs) throws Exception {
        withV2(vertical, threshold -> {
            List<Product> products = products(vertical);
            products.forEach(product -> product.setOffersCount(1));
            Product withoutOffers = products.getFirst();
            withoutOffers.setOffersCount(0);
            withoutOffers.getScores().put("ECOSCORE", new Score("ECOSCORE", 12.0));

            ProductRepository repository = mock(ProductRepository.class);
            when(repository.exportVerticalWithValidDate(vertical, false)).thenReturn(products.stream());
            VerticalsConfigService verticalsConfigService = mock(VerticalsConfigService.class);
            when(verticalsConfigService.getConfigByIdOrDefault(vertical.getId())).thenReturn(vertical);

            aggregationFacadeService(logs, mock(BrandService.class), repository, verticalsConfigService).score(vertical);

            assertThat(withoutOffers.ecoscore().getValue()).isEqualTo(12.0);
            assertThat(withoutOffers.getImpactScoreMeta()).isNull();
            products.subList(1, products.size()).forEach(product -> {
                assertThat(product.getImpactScoreMeta().getMethodologyVersion()).isEqualTo(2);
                assertThat(product.ecoscore().getRanking()).isNotNull();
            });
            @SuppressWarnings("unchecked")
            ArgumentCaptor<Collection<Product>> indexed = ArgumentCaptor.forClass(Collection.class);
            verify(repository).addToFullindexationQueue(indexed.capture());
            assertThat(indexed.getValue()).hasSize(products.size() - 1).doesNotContain(withoutOffers);
        });
    }

    private static void assertNotRated(Product product, ImpactScoreMeta.NotRatedReason reason) {
        assertThat(product.ecoscore()).as("ImpactScore of not rated product %s", product.getId()).isNull();
        assertThat(product.getImpactScoreMeta().getStatus()).isEqualTo(ImpactScoreMeta.Status.NOT_RATED.name());
        assertThat(product.getImpactScoreMeta().getNotRatedReason()).as("reason of product %s", product.getId())
                .isEqualTo(reason.name());
        assertThat(product.getRanking().getGlobalPosition()).isZero();
        assertThat(product.getRanking().getGlobalCount()).isZero();
    }

    private static String status(List<Product> products, long id) {
        return products.stream().filter(p -> p.getId() == id).findFirst().orElseThrow().getImpactScoreMeta()
                .getStatus();
    }

    /** Body run with the methodology version forced to 2, given the coverage threshold. */
    @FunctionalInterface
    private interface V2Body {
        void run(double threshold) throws Exception;
    }

    /**
     * Runs {@code body} with the version forced to 2 by the override, and the configured threshold, or
     * {@link #DEFAULT_V2_THRESHOLD} while it is not set; both are restored afterwards.
     */
    private static void withV2(VerticalConfig vertical, V2Body body) throws Exception {
        ImpactScoreConfig config = vertical.getImpactScoreConfig();
        assertThat(config.getV2()).as("v2 block of %s", vertical.getId()).isNotNull();
        Double configured = config.getV2().getMinRealWeightCoverage();
        try {
            config.setMethodologyVersionOverride(ImpactScoreConfig.METHODOLOGY_V2);
            if (configured == null) {
                config.getV2().setMinRealWeightCoverage(DEFAULT_V2_THRESHOLD);
            }
            body.run(config.minRealWeightCoverage());
        } finally {
            config.getV2().setMinRealWeightCoverage(configured);
            config.setMethodologyVersionOverride(null);
        }
    }

    /** The V2 matrix of the vertical, criteria weighted above 0. */
    private static Map<String, Double> v2Weights(VerticalConfig vertical) {
        Map<String, Double> weights = new TreeMap<>();
        vertical.getImpactScoreConfig().getV2().getCriteriasPonderation().forEach((key, weight) -> {
            if (weight != null && weight > 0) {
                weights.put(key, weight);
            }
        });
        return weights;
    }

    /**
     * Products with a designed coverage of the V2 matrix: every criterion (two of them tied), none, just below and
     * just above the threshold, heavy criteria only, light criteria only.
     *
     * @param designed filled with the criteria given to each product, by id
     */
    private static List<Product> v2Products(VerticalConfig vertical, double threshold, Map<Long, Set<String>> designed) {
        Map<String, Double> weights = v2Weights(vertical);
        for (String criterion : weights.keySet()) {
            AttributeConfig attribute = vertical.getAttributesConfig().getAttributeConfigByKey(criterion);
            assertThat(attribute).as("attribute of V2 criterion %s", criterion).isNotNull();
            assertThat(attribute.isAsScore() && hasParseableValue(attribute)).as("V2 criterion %s", criterion).isTrue();
        }
        List<String> criteria = new ArrayList<>(weights.keySet());
        double total = weights.values().stream().mapToDouble(Double::doubleValue).sum();

        // Designed subsets: the largest coverage below the threshold, the smallest at or above it
        Set<String> justBelow = Set.of();
        Set<String> justAbove = Set.copyOf(criteria);
        double below = -1;
        double above = 2;
        for (int mask = 1; mask < (1 << criteria.size()); mask++) {
            Set<String> subset = new TreeSet<>();
            for (int bit = 0; bit < criteria.size(); bit++) {
                if ((mask & (1 << bit)) != 0) {
                    subset.add(criteria.get(bit));
                }
            }
            double coverage = subset.stream().mapToDouble(weights::get).sum() / total;
            if (coverage < threshold - 1e-6 && coverage > below) {
                below = coverage;
                justBelow = subset;
            } else if (coverage >= threshold && coverage < above) {
                above = coverage;
                justAbove = subset;
            }
        }
        List<String> byWeight = criteria.stream().sorted(Comparator.comparing(weights::get).reversed()).toList();
        int half = (byWeight.size() + 1) / 2;

        Map<Long, Set<String>> cohort = new java.util.LinkedHashMap<>();
        cohort.put(TIED, Set.copyOf(criteria));
        cohort.put(TIED + 1, Set.copyOf(criteria));
        cohort.put(2000L, Set.copyOf(criteria));
        cohort.put(2001L, Set.copyOf(criteria));
        cohort.put(2002L, Set.of());
        cohort.put(JUST_BELOW, justBelow);
        cohort.put(JUST_ABOVE, justAbove);
        cohort.put(2005L, Set.copyOf(byWeight.subList(0, half)));
        cohort.put(2006L, Set.copyOf(byWeight.subList(half, byWeight.size())));

        List<Product> products = new ArrayList<>();
        int index = 0;
        for (Map.Entry<Long, Set<String>> entry : cohort.entrySet()) {
            // The two tied products carry the same values
            int valueIndex = entry.getKey() == TIED + 1 ? 0 : index;
            Product product = new Product(entry.getKey());
            for (String criterion : entry.getValue()) {
                AttributeConfig attribute = vertical.getAttributesConfig().getAttributeConfigByKey(criterion);
                // A derived criterion is read from its sources only (GOU-2041): fill in the first one
                String key = attribute.isDerivedScore() ? attribute.getScoreSources().getFirst() : criterion;
                AttributeConfig valued = vertical.getAttributesConfig().getAttributeConfigByKey(key);
                product.getAttributes().getIndexed().put(key, new IndexedAttribute(key, value(valued, valueIndex)));
            }
            products.add(product);
            designed.put(entry.getKey(), new TreeSet<>(entry.getValue()));
            index++;
        }
        return products;
    }

    /** A criterion counted by the V2 {@code DATA_QUALITY}, read from the configuration (design 2.3). */
    private static boolean isV2DataQualityCriterion(VerticalConfig vertical, String name) {
        if (DATA_QUALITY.equals(name) || name.startsWith("BRAND_") || vertical.getCompositeScores().contains(name)
                || !vertical.getAvailableImpactScoreCriterias().contains(name)) {
            return false;
        }
        AttributeConfig attribute = vertical.getAttributesConfig().getAttributeConfigByKey(name);
        return attribute != null && attribute.isAsScore();
    }

    /**
     * ImpactScore of each product of {@link #products(VerticalConfig)}, scored in production order. With
     * {@code worstDataQuality}, every {@code DATA_QUALITY} is set to 0 once computed, before the ImpactScore.
     */
    private static Map<Long, Double> impactScores(VerticalConfig vertical, boolean worstDataQuality) {
        Logger logger = LoggerFactory.getLogger(ImpactScoreRealVerticalsTest.class);
        List<AbstractAggregationService> services = new ArrayList<>();
        services.add(new CleanScoreAggregationService(logger));
        services.add(new Attribute2ScoreAggregationService(logger));
        services.add(new BrandScoresAggregationService(logger, mock(BrandService.class)));
        services.add(new DataCompletion2ScoreAggregationService(logger));
        if (worstDataQuality) {
            services.add(new AbstractAggregationService(logger) {
                @Override
                public void onProduct(Product data, VerticalConfig vConf) {
                    Score dataQuality = data.getScores().get(DATA_QUALITY);
                    assertThat(dataQuality).as("DATA_QUALITY of product %s", data.getId()).isNotNull();
                    dataQuality.getRelativ().setValue(0.0);
                }
            });
        }
        services.add(new EcoScoreAggregationService(logger));
        services.add(new ParticipatingScoresAggregationService(logger));

        List<Product> products = products(vertical);
        new ScoringBatchedAggregator(services).score(products, vertical);

        Map<Long, Double> scores = new TreeMap<>();
        products.forEach(product -> scores.put(product.getId(), product.ecoscore().getValue()));
        return scores;
    }

    /** {@link #products(VerticalConfig)} with a brand, resolved to a company carrying two scores (CDP, Good On You). */
    private static List<Product> brandedProducts(VerticalConfig vertical) {
        List<Product> products = products(vertical);
        products.forEach(product -> product.getAttributes().getReferentielAttributes().put(ReferentielKey.BRAND, "ACME"));
        return products;
    }

    private static BrandService brandServiceWithTwoScores() {
        Company company = new Company("acme", "Acme");
        for (String provider : List.of("cdp", "good-on-you")) {
            CompanyScore score = new CompanyScore();
            score.setValue(60.0);
            score.setScale(new ScoreScale(0.0, 100.0, true));
            company.getScores().put(provider, score);
        }
        Brand brand = new Brand("ACME");
        brand.setCompanyName("Acme");
        brand.setCompany(company);

        BrandService brandService = mock(BrandService.class);
        when(brandService.resolve(anyString())).thenReturn(brand);
        return brandService;
    }

    /** Builds products that all carry the very same attribute values, hence the same ImpactScore. */
    private static List<Product> tiedProducts(VerticalConfig vertical) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < PRODUCT_COUNT; i++) {
            Product product = new Product(1000L + i);
            for (AttributeConfig attribute : vertical.getAttributesConfig().getConfigs()) {
                if (attribute.isAsScore() && hasParseableValue(attribute)) {
                    product.getAttributes().getIndexed().put(attribute.getKey(),
                            new IndexedAttribute(attribute.getKey(), value(attribute, 0)));
                }
            }
            products.add(product);
        }
        return products;
    }

    /** Builds products carrying a distinct, parseable value for every attribute scored by the vertical. */
    private static List<Product> products(VerticalConfig vertical) {
        List<Product> products = new ArrayList<>();
        for (int i = 0; i < PRODUCT_COUNT; i++) {
            Product product = new Product(1000L + i);
            for (AttributeConfig attribute : vertical.getAttributesConfig().getConfigs()) {
                if (attribute.isAsScore() && hasParseableValue(attribute)) {
                    product.getAttributes().getIndexed().put(attribute.getKey(),
                            new IndexedAttribute(attribute.getKey(), value(attribute, i)));
                }
            }
            products.add(product);
        }
        return products;
    }

    private static boolean hasParseableValue(AttributeConfig attribute) {
        return AttributeType.NUMERIC.equals(attribute.getFilteringType()) || !attribute.getNumericMapping().isEmpty();
    }

    private static String value(AttributeConfig attribute, int index) {
        if (AttributeType.NUMERIC.equals(attribute.getFilteringType())) {
            return String.valueOf(1.0 + index * 2.5);
        }
        List<String> labels = attribute.getNumericMapping().keySet().stream().sorted().toList();
        return labels.get(index % labels.size());
    }

    private static AggregationFacadeService aggregationFacadeService(Path logs) {
        return aggregationFacadeService(logs, mock(BrandService.class));
    }

    private static AggregationFacadeService aggregationFacadeService(Path logs, BrandService brandService) {
        return aggregationFacadeService(logs, brandService, mock(ProductRepository.class),
                mock(VerticalsConfigService.class));
    }

    private static AggregationFacadeService aggregationFacadeService(Path logs, BrandService brandService,
            ProductRepository repository, VerticalsConfigService verticalsConfigService) {
        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setRootFolder(logs + "/");

        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        doAnswer(invocation -> null).when(beanFactory).autowireBean(any());

        return new AggregationFacadeService(
                mock(EvaluationService.class),
                mock(org.open4goods.model.StandardiserService.class),
                beanFactory,
                repository,
                apiProperties,
                mock(Gs1PrefixService.class),
                mock(DataSourceConfigService.class),
                verticalsConfigService,
                mock(BarcodeValidationService.class),
                brandService,
                mock(BlablaService.class),
                mock(IcecatService.class),
                mock(IcecatFeatureResolver.class),
                mock(SerialisationService.class),
                mock(BarcodeForensicsService.class));
    }
}
