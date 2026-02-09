package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class UsageCostAggregationServiceTest
{

    private static final Logger LOGGER = LoggerFactory.getLogger(UsageCostAggregationServiceTest.class);

    @Test
    void onProductComputesUsageCostPerYear() throws Exception
    {
        UsageCostAggregationService service = new UsageCostAggregationService(LOGGER);
        VerticalConfig verticalConfig = new VerticalConfig();
        verticalConfig.setAverageHoursPerDay(4.0);
        verticalConfig.setAverageKwhCost(0.1952);

        Product product = new Product(123L);

        service.onProduct(product, verticalConfig);

        assertThat(product.getAttributes().getIndexed()).containsKey("USAGE_COST_YEAR");
        assertThat(product.getAttributes().getIndexed().get("USAGE_COST_YEAR").getValue()).isEqualTo("284.99");
        assertThat(product.getAttributes().getIndexed().get("USAGE_COST_YEAR").getNumericValue()).isEqualTo(284.99);
    }

    @Test
    void onProductSkipsWhenConfigurationMissing() throws Exception
    {
        UsageCostAggregationService service = new UsageCostAggregationService(LOGGER);
        VerticalConfig verticalConfig = new VerticalConfig();

        Product product = new Product(456L);

        service.onProduct(product, verticalConfig);

        assertThat(product.getAttributes().getIndexed()).doesNotContainKey("USAGE_COST_YEAR");
    }
}
