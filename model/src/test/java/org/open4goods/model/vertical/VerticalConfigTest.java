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

        @Test
        void shouldStoreConfiguredSubCategories()
        {
                VerticalConfig config = new VerticalConfig();
                VerticalSubCategory subCategory = new VerticalSubCategory();
                subCategory.setId("under-sink");
                subCategory.getSlug().put("fr", "lave-vaisselle-sous-lavabo");

                config.setSubCategories(List.of(subCategory));

                assertThat(config.getSubCategories()).containsExactly(subCategory);
                assertThat(config.getSubCategories().get(0).getSlug().i18n("fr"))
                        .isEqualTo("lave-vaisselle-sous-lavabo");
        }
}
