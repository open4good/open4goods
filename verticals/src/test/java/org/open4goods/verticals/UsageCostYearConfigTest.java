package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;

/**
 * USAGE_COST_YEAR can only be computed for verticals declaring power attributes:
 * only tv opts in through moneyFilters.
 */
class UsageCostYearConfigTest {

    private static final String KEY = "USAGE_COST_YEAR";

    @Test
    void usageCostYearMoneyFilterIsDeclaredOnlyForTv() throws Exception {
        ObjectMapper mapper = YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        Resource[] resources = new PathMatchingResourcePatternResolver().getResources("classpath:verticals/*.yml");
        assertThat(resources).isNotEmpty();

        boolean tvSeen = false;
        for (Resource resource : resources) {
            VerticalConfig config;
            try (InputStream in = resource.getInputStream()) {
                config = mapper.readValue(in, VerticalConfig.class);
            }
            String file = resource.getFilename();
            boolean hasFilter = config.getMoneyFilters() != null && config.getMoneyFilters().contains(KEY);
            if ("tv.yml".equals(file)) {
                tvSeen = true;
                assertThat(hasFilter).describedAs("tv moneyFilters").isTrue();
                assertThat(config.getUsagePowerAttribute()).isEqualTo("POWER_CONSUMPTION_SDR");
                assertThat(config.getStandbyPowerAttribute()).isEqualTo("POWER_CONSUMPTION_STANDBY");
                assertThat(config.getAverageHoursPerDay()).isEqualTo(3.19);
            } else {
                assertThat(hasFilter).describedAs("%s moneyFilters", file).isFalse();
            }
        }
        assertThat(tvSeen).isTrue();
    }
}
