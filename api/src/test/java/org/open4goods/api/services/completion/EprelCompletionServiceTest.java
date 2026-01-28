package org.open4goods.api.services.completion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.api.services.AggregationFacadeService;
import org.open4goods.api.services.aggregation.aggregator.StandardAggregator;
import org.open4goods.model.attribute.ReferentielKey;
import org.open4goods.model.eprel.EprelProduct;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.services.eprelservice.service.EprelSearchService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.open4goods.verticals.VerticalsConfigService;

import ch.qos.logback.classic.Level;

class EprelCompletionServiceTest {

    private EprelCompletionService service;
    private EprelSearchService eprelSearchService;
    private VerticalConfig vertical;
    private Product product;

    @BeforeEach
    void setUp() {
        VerticalsConfigService verticalConfigService = Mockito.mock(VerticalsConfigService.class);
        ProductRepository productRepository = Mockito.mock(ProductRepository.class);
        ApiProperties apiProperties = Mockito.mock(ApiProperties.class);
        eprelSearchService = Mockito.mock(EprelSearchService.class);
        AggregationFacadeService aggregationFacadeService = Mockito.mock(AggregationFacadeService.class);
        StandardAggregator aggregator = Mockito.mock(StandardAggregator.class);

        when(apiProperties.logsFolder()).thenReturn("/tmp/logs");
        when(apiProperties.aggLogLevel()).thenReturn(Level.INFO);
        when(aggregationFacadeService.getStandardAggregator("eprel-aggregation")).thenReturn(aggregator);

        service = new EprelCompletionService(verticalConfigService, productRepository, apiProperties,
                eprelSearchService, aggregationFacadeService);
        vertical = new VerticalConfig();
        vertical.setEprelGroupNames(List.of("TELEVISION"));
        product = new Product(123L);
        product.getAttributes().addReferentielAttribute(ReferentielKey.MODEL, "MODEL-A");
    }

    @Test
    void processProductUsesLatestEprelVersionWhenAvailable() {
        EprelProduct current = new EprelProduct();
        current.setEprelRegistrationNumber("old");
        current.setLastVersion(false);
        current.setProductModelCoreId(10L);
        current.setVersionId(1L);

        EprelProduct latest = new EprelProduct();
        latest.setEprelRegistrationNumber("new");
        latest.setLastVersion(true);
        latest.setProductModelCoreId(10L);
        latest.setVersionId(2L);

        when(eprelSearchService.search(anyString(), anyList(), anyCollection()))
                .thenReturn(List.of(current));
        when(eprelSearchService.searchByProductModelCoreId(Mockito.eq(10L), anyCollection()))
                .thenReturn(List.of(current, latest));

        service.processProduct(vertical, product);

        assertThat(product.getEprelDatas()).isEqualTo(latest);
        assertThat(product.getExternalIds().getEprel()).isEqualTo("new");
    }
}
