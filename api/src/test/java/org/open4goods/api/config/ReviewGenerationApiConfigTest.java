package org.open4goods.api.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Verifies that API-owned YAML carries review-generation source preferences.
 */
class ReviewGenerationApiConfigTest {

    @Test
    void applicationYaml_ConfiguresPreferredDomainsAndPlaywrightFetching() {
        YamlPropertiesFactoryBean yamlFactory = new YamlPropertiesFactoryBean();
        yamlFactory.setResources(new ClassPathResource("application.yml"));

        Properties properties = yamlFactory.getObject();

        assertThat(properties).isNotNull();
        assertThat(properties.getProperty("review.generation.min-markdown-chars"))
                .isEqualTo("${REVIEW_GENERATION_MIN_MARKDOWN_CHARS:500}");
        assertThat(properties.getProperty("review.generation.preferred-domains[0]"))
                .isEqualTo("leclaireur.fnac.com");
        assertThat(properties.getProperty("review.generation.preferred-domains[4]"))
                .isEqualTo("lesnumeriques.com");
        assertThat(properties.getProperty("urlfetcher.domains.lesnumeriques.com.strategy"))
                .isEqualTo("PLAYWRIGHT");
        assertThat(properties.getProperty("urlfetcher.domains.www.quechoisir.org.timeout"))
                .isEqualTo("15000");
    }
}
