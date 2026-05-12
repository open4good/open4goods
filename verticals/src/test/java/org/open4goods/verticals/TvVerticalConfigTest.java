package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;


import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.core.io.ClassPathResource;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.dataformat.yaml.YAMLMapper;

class TvVerticalConfigTest {

    @Test
    void testTvYmlContainsElectronicDisplays() throws Exception {
        ObjectMapper mapper = YAMLMapper.builder()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();

        ClassPathResource resource = new ClassPathResource("verticals/tv.yml");
        assertThat(resource.exists()).describedAs("tv.yml must exist").isTrue();

        try (InputStream inputStream = resource.getInputStream()) {
            VerticalConfig config = mapper.readValue(inputStream, VerticalConfig.class);

            assertThat(config.getId()).isEqualTo("tv");
        }
    }
}
