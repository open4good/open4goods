package org.open4goods.api.services.aggregation.services.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.open4goods.model.attribute.IndexedAttribute;
import org.open4goods.model.product.Product;
import org.open4goods.model.vertical.VerticalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

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
        verticalConfig.setUsagePowerAttribute("power_consumption");

        Product product = new Product(123L);
        product.getAttributes().getIndexed().put("power_consumption", new org.open4goods.model.attribute.IndexedAttribute("power_consumption", "1000.0"));

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

    /**
     * Uses the real tv.yml (power attributes, hours per day) and the shared
     * average kWh cost of _default.yml: 70 W in use, 0.5 W on standby.
     * (0.07 kW * 3.19 h + 0.0005 kW * 20.81 h) * 365 d * 0.194 EUR/kWh = 16.55 EUR.
     */
    @Test
    void onProductComputesUsageCostForTvWithRealConfig() throws Exception
    {
        ObjectMapper mapper = YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        VerticalConfig tv;
        VerticalConfig defaults;
        try (InputStream in = new ClassPathResource("verticals/tv.yml").getInputStream()) {
            tv = mapper.readValue(in, VerticalConfig.class);
        }
        try (InputStream in = new ClassPathResource("verticals/_default.yml").getInputStream()) {
            defaults = mapper.readValue(in, VerticalConfig.class);
        }
        // tv.yml inherits averageKwhCost from _default.yml
        tv.setAverageKwhCost(defaults.getAverageKwhCost());

        assertThat(tv.getUsagePowerAttribute()).isEqualTo("POWER_CONSUMPTION_SDR");
        assertThat(tv.getStandbyPowerAttribute()).isEqualTo("POWER_CONSUMPTION_STANDBY");

        Product product = new Product(789L);
        product.getAttributes().getIndexed().put("POWER_CONSUMPTION_SDR",
                new IndexedAttribute("POWER_CONSUMPTION_SDR", "70"));
        product.getAttributes().getIndexed().put("POWER_CONSUMPTION_STANDBY",
                new IndexedAttribute("POWER_CONSUMPTION_STANDBY", "0.5"));

        new UsageCostAggregationService(LOGGER).onProduct(product, tv);

        assertThat(product.getAttributes().getIndexed().get("USAGE_COST_YEAR").getNumericValue()).isEqualTo(16.55);
    }
}
