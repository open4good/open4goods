package org.open4goods.model.vertical;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

class VerticalConfigTest
{

        @Test
        void shouldNormaliseAndDeduplicateEprelGroupNames()
        {
                VerticalConfig config = new VerticalConfig();

                config.setEprelGroupNames(List.of("  TV  ", "MONITOR", "TV"));

                assertThat(config.getEprelGroupNames()).containsExactly("TV", "MONITOR");
        }

        @Test
        void shouldSupportLegacyScalarEprelGroupName()
        {
                VerticalConfig config = new VerticalConfig();

                config.setEprelGroupName("Legacy");

                assertThat(config.getEprelGroupNames()).containsExactly("Legacy");
                assertThat(config.getEprelGroupName()).isEqualTo("Legacy");
        }
}
