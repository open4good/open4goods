package org.open4goods.verticals;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;


import org.junit.jupiter.api.Test;
import org.open4goods.model.vertical.VerticalConfig;
import org.springframework.core.io.ClassPathResource;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

class TvVerticalConfigTest {

    @Test
    void testTvYmlContainsElectronicDisplays() throws Exception {
        // Simple manual parsing to avoid complex dependency setup
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.findAndRegisterModules(); // Handle standard modules if needed

        ClassPathResource resource = new ClassPathResource("verticals/tv.yml");
        assertThat(resource.exists()).describedAs("tv.yml must exist").isTrue();

        try (InputStream inputStream = resource.getInputStream()) {
            VerticalConfig config = mapper.readValue(inputStream, VerticalConfig.class);
            
            assertThat(config.getId()).isEqualTo("tv");
            assertThat(config.getEprelGroupNames())
                .describedAs("tv.yml must contain 'electronicdisplays' in eprelGroupNames")
                .contains("electronicdisplays", "televisions");
        }
    }
}
