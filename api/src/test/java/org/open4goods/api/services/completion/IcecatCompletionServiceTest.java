package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.Duration;

import ch.qos.logback.classic.Level;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.config.yml.IcecatCompletionConfig;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.commons.services.DataSourceConfigService;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

@ExtendWith(MockitoExtension.class)
class IcecatCompletionServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private VerticalsConfigService verticalConfigService;

    @Mock
    private ApiProperties apiProperties;

    @Mock
    private DataSourceConfigService dataSourceConfigService;

    @Mock
    private AggregationFacadeService aggregationFacadeService;

    @Mock
    private StandardAggregator standardAggregator;

    @Mock
    private VerticalConfig verticalConfig;

    private IcecatCompletionService service;

    @BeforeEach
    void setUp() {
        when(apiProperties.logsFolder()).thenReturn("/tmp");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);
        when(apiProperties.getIcecatCompletionConfig()).thenReturn(new IcecatCompletionConfig());
        when(aggregationFacadeService.getStandardAggregator(anyString())).thenReturn(standardAggregator);

        service = new IcecatCompletionService(productRepository, verticalConfigService, apiProperties, dataSourceConfigService, aggregationFacadeService);
    }

    @Test
    void shouldProcessWhenNeverProcessed() {
        Product product = new Product();

        assertThat(service.shouldProcess(verticalConfig, product)).isTrue();
    }

    @Test
    void shouldSkipWhenRecentlyProcessed() {
        Product product = new Product();
        long recentTimestamp = System.currentTimeMillis() - Duration.ofDays(1).toMillis();
        product.getDatasourceCodes().put(service.getDatasourceName(), recentTimestamp);

        assertThat(service.shouldProcess(verticalConfig, product)).isFalse();
    }

    @Test
    void shouldProcessWhenStale() {
        Product product = new Product();
        long staleTimestamp = System.currentTimeMillis() - Duration.ofDays(REFRESH_AGE_IN_DAYS()).toMillis() - Duration.ofHours(1).toMillis();
        product.getDatasourceCodes().put(service.getDatasourceName(), staleTimestamp);

        assertThat(service.shouldProcess(verticalConfig, product)).isTrue();
    }

    private static long REFRESH_AGE_IN_DAYS() {
        return 30;
    }
}
