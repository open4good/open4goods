package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.io.InputStream;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import java.util.stream.Stream;

import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
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
import org.open4goods.model.product.ImpactScoreMeta;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.Score;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.evaluation.service.EvaluationService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.services.serialisation.service.SerialisationService;
import org.open4goods.verticals.GoogleTaxonomyService;
import org.open4goods.verticals.VerticalsConfigService;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
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
        ApiProperties apiProperties = new ApiProperties();
        apiProperties.setRootFolder(logs + "/");

        AutowireCapableBeanFactory beanFactory = mock(AutowireCapableBeanFactory.class);
        doAnswer(invocation -> null).when(beanFactory).autowireBean(any());

        return new AggregationFacadeService(
                mock(EvaluationService.class),
                mock(org.open4goods.model.StandardiserService.class),
                beanFactory,
                mock(ProductRepository.class),
                apiProperties,
                mock(Gs1PrefixService.class),
                mock(DataSourceConfigService.class),
                mock(VerticalsConfigService.class),
                mock(BarcodeValidationService.class),
                mock(BrandService.class),
                mock(BlablaService.class),
                mock(IcecatService.class),
                mock(IcecatFeatureResolver.class),
                mock(SerialisationService.class),
                mock(BarcodeForensicsService.class));
    }
}
