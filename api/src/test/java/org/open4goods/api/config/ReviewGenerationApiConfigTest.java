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
        assertThat(properties.getProperty("review.generation.max-search"))
                .isEqualTo("${REVIEW_GENERATION_MAX_SEARCH:7}");
        assertThat(properties.getProperty("review.generation.min-global-tokens"))
                .isEqualTo("${REVIEW_GENERATION_MIN_GLOBAL_TOKENS:6000}");
        assertThat(properties.getProperty("review.generation.min-url-count"))
                .isEqualTo("${REVIEW_GENERATION_MIN_URL_COUNT:3}");
        assertThat(properties.getProperty("review.generation.preferred-domains[0]"))
                .isEqualTo("leclaireur.fnac.com");
        assertThat(properties.getProperty("review.generation.preferred-domains[3]"))
                .isEqualTo("quel-lave-linge.fr");
        assertThat(properties.getProperty("review.generation.preferred-domains[8]"))
                .isEqualTo("lesnumeriques.com");
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> name.contains("official-domains-by-brand")
                        || name.contains("source-url-templates-by-brand"));
        assertThat(properties.stringPropertyNames())
                .noneMatch(name -> {
                    String value = properties.getProperty(name);
                    return value != null && value.toLowerCase().contains("haier");
                });
        assertThat(properties.getProperty("urlfetcher.domains.lesnumeriques.com.strategy"))
                .isEqualTo("PLAYWRIGHT");
        assertThat(properties.getProperty("urlfetcher.domains.www.quechoisir.org.timeout"))
                .isEqualTo("15000");
    }
}
