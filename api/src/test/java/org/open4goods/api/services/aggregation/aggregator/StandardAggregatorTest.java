package org.open4goods.api.services.aggregation.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.model.datafragment.DataFragment;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

/**
 * Regression tests for the vertical configuration handed to each aggregation
 * service. Classification runs inside the same chain, so a service that changes
 * {@link Product#getVertical()} must not leave later services parsing, indexing
 * and scoring against the configuration the product had on entry.
 */
@ExtendWith(MockitoExtension.class)
class StandardAggregatorTest {

    private static final String INITIAL_VERTICAL = "tv";
    private static final String CLASSIFIED_VERTICAL = "refrigerateurs";

    @Mock
    private VerticalsConfigService verticalsConfigService;

    private VerticalConfig initialConfig;
    private VerticalConfig classifiedConfig;
    private VerticalConfig defaultConfig;

    @BeforeEach
    void setUp() {
        initialConfig = new VerticalConfig();
        initialConfig.setId(INITIAL_VERTICAL);
        classifiedConfig = new VerticalConfig();
        classifiedConfig.setId(CLASSIFIED_VERTICAL);
        defaultConfig = new VerticalConfig();
        defaultConfig.setId("_default");
    }

    /**
     * Aggregation service that reclassifies the product into another vertical,
     * standing in for the taxonomy service.
     */
    private static final class ReclassifyingService extends AbstractAggregationService {

        private final String vertical;

        private ReclassifyingService(String vertical) {
            super(LoggerFactory.getLogger(StandardAggregatorTest.class));
            this.vertical = vertical;
        }

        @Override
        public void onProduct(Product data, VerticalConfig vConf) {
            data.setVertical(vertical);
        }

        @Override
        public void onDataFragment(DataFragment input, Product output, VerticalConfig vConf) {
            output.setVertical(vertical);
        }
    }

    /**
     * Aggregation service that records the identifier of the configuration it was
     * handed, so the test can assert what later services actually see.
     */
    private static final class RecordingService extends AbstractAggregationService {

        private final List<String> seenVerticalIds = new ArrayList<>();

        private RecordingService() {
            super(LoggerFactory.getLogger(StandardAggregatorTest.class));
        }

        @Override
        public void onProduct(Product data, VerticalConfig vConf) {
            seenVerticalIds.add(vConf == null ? null : vConf.getId());
        }

        @Override
        public void onDataFragment(DataFragment input, Product output, VerticalConfig vConf) {
            seenVerticalIds.add(vConf == null ? null : vConf.getId());
        }
    }

    @Test
    void onProduct_shouldReResolveVerticalConfig_whenClassificationChangesTheVertical() throws Exception {
        when(verticalsConfigService.getConfigByIdOrDefault(INITIAL_VERTICAL)).thenReturn(initialConfig);
        when(verticalsConfigService.getConfigByIdOrDefault(CLASSIFIED_VERTICAL)).thenReturn(classifiedConfig);

        RecordingService recorder = new RecordingService();
        StandardAggregator aggregator = new StandardAggregator(
                List.of(new ReclassifyingService(CLASSIFIED_VERTICAL), recorder), verticalsConfigService);

        Product product = new Product();
        product.setVertical(INITIAL_VERTICAL);

        aggregator.onProduct(product);

        assertThat(recorder.seenVerticalIds).containsExactly(CLASSIFIED_VERTICAL);
    }

    @Test
    void onDatafragment_shouldReResolveVerticalConfig_whenClassificationChangesTheVertical() throws Exception {
        when(verticalsConfigService.getConfigByIdOrDefault(INITIAL_VERTICAL)).thenReturn(initialConfig);
        when(verticalsConfigService.getConfigByIdOrDefault(CLASSIFIED_VERTICAL)).thenReturn(classifiedConfig);

        RecordingService recorder = new RecordingService();
        StandardAggregator aggregator = new StandardAggregator(
                List.of(new ReclassifyingService(CLASSIFIED_VERTICAL), recorder), verticalsConfigService);

        Product product = new Product();
        product.setVertical(INITIAL_VERTICAL);

        aggregator.onDatafragment(new DataFragment(), product);

        assertThat(recorder.seenVerticalIds).containsExactly(CLASSIFIED_VERTICAL);
    }

    @Test
    void onProduct_shouldReResolveTheDefaultConfig_whenClassificationClearsTheVertical() throws Exception {
        when(verticalsConfigService.getConfigByIdOrDefault(INITIAL_VERTICAL)).thenReturn(initialConfig);
        when(verticalsConfigService.getConfigByIdOrDefault(null)).thenReturn(defaultConfig);

        RecordingService recorder = new RecordingService();
        StandardAggregator aggregator = new StandardAggregator(
                List.of(new ReclassifyingService(null), recorder), verticalsConfigService);

        Product product = new Product();
        product.setVertical(INITIAL_VERTICAL);

        aggregator.onProduct(product);

        assertThat(recorder.seenVerticalIds).containsExactly("_default");
    }

    @Test
    void onProduct_shouldResolveTheConfigOnce_whenClassificationLeavesTheVerticalUnchanged() throws Exception {
        when(verticalsConfigService.getConfigByIdOrDefault(INITIAL_VERTICAL)).thenReturn(initialConfig);

        RecordingService firstRecorder = new RecordingService();
        RecordingService secondRecorder = new RecordingService();
        StandardAggregator aggregator = new StandardAggregator(
                List.of(firstRecorder, new ReclassifyingService(INITIAL_VERTICAL), secondRecorder),
                verticalsConfigService);

        Product product = new Product();
        product.setVertical(INITIAL_VERTICAL);

        aggregator.onProduct(product);

        assertThat(firstRecorder.seenVerticalIds).containsExactly(INITIAL_VERTICAL);
        assertThat(secondRecorder.seenVerticalIds).containsExactly(INITIAL_VERTICAL);
        org.mockito.Mockito.verify(verticalsConfigService, org.mockito.Mockito.times(1))
                .getConfigByIdOrDefault(INITIAL_VERTICAL);
    }
}
