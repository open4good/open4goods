package org.open4goods.api.services.aggregation.aggregator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.open4goods.api.services.aggregation.AbstractAggregationService;
import org.open4goods.commons.exceptions.AggregationSkipException;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.open4goods.verticals.VerticalsConfigService;
import org.slf4j.LoggerFactory;

class StandardAggregatorTest
{

    @Test
    void onProductsRunsLifecycleHooks() throws AggregationSkipException
    {
        CountingAggregationService service = new CountingAggregationService();
        VerticalsConfigService verticals = mock(VerticalsConfigService.class);
        VerticalConfig vertical = new VerticalConfig();
        vertical.setId("vertical-test");
        when(verticals.getConfigByIdOrDefault("vertical-test")).thenReturn(vertical);

        StandardAggregator aggregator = new StandardAggregator(List.of(service), verticals);

        Product first = new Product(1L);
        first.setVertical("vertical-test");
        Product second = new Product(2L);
        second.setVertical("vertical-test");

        aggregator.onProducts(List.of(first, second), vertical);

        assertThat(service.getInitCount()).isEqualTo(1);
        assertThat(service.getOnProductCount()).isEqualTo(2);
        assertThat(service.getDoneCount()).isEqualTo(1);
        assertThat(service.getLastVertical()).isSameAs(vertical);
    }

    private static class CountingAggregationService extends AbstractAggregationService
    {

        private int initCount;
        private int onProductCount;
        private int doneCount;
        private VerticalConfig lastVertical;

        CountingAggregationService()
        {
            super(LoggerFactory.getLogger(CountingAggregationService.class));
        }

        @Override
        public void init(java.util.Collection<Product> datas)
        {
            initCount += 1;
        }

        @Override
        public void onProduct(Product data, VerticalConfig vConf)
        {
            onProductCount += 1;
            lastVertical = vConf;
        }

        @Override
        public void done(java.util.Collection<Product> datas, VerticalConfig vConf)
        {
            doneCount += 1;
        }

        int getInitCount()
        {
            return initCount;
        }

        int getOnProductCount()
        {
            return onProductCount;
        }

        int getDoneCount()
        {
            return doneCount;
        }

        VerticalConfig getLastVertical()
        {
            return lastVertical;
        }
    }
}
