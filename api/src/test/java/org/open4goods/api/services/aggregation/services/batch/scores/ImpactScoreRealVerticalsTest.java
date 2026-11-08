package org.open4goods.api.services.aggregation.services.batch.scores;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
