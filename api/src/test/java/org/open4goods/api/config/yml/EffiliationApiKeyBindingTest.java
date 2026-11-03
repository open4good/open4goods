package org.open4goods.api.config.yml;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Shows that the {@code AFFILIATIONCONFIG_EFFILIATIONAPIKEY} OS environment variable, rendered into
 * {@code /etc/open4goods/api.env} from the {@code API_EFFILIATION_API_KEY} GitHub Environment secret (GOU-1014) and
 * exported by {@code publish-jars.sh}, binds to {@code affiliation-config.effiliation-api-key} through Spring Boot's
 * relaxed binding and beats the value of {@code application-active.yml} (open4goods-config). That property is the key
 * {@code ApiConfig#effiliationFeedService} hands to the Effiliation product feeds.
 */
class EffiliationApiKeyBindingTest {

    private static final String YAML = """
            admin-key: test-admin-key
            affiliation-config:
              effiliation-api-key: yaml-key
            """;

    @TempDir
    Path tempDir;

    @Test
    void osEnvironmentVariableBeatsTheYamlValue() throws IOException {
        try (ConfigurableApplicationContext context = start(Map.of("AFFILIATIONCONFIG_EFFILIATIONAPIKEY", "env-key"))) {
            assertThat(context.getBean(ApiProperties.class).getAffiliationConfig().getEffiliationApiKey())
                    .isEqualTo("env-key");
        }
    }

    @Test
    void yamlValueIsKeptWithoutTheOsEnvironmentVariable() throws IOException {
        try (ConfigurableApplicationContext context = start(Map.of())) {
            assertThat(context.getBean(ApiProperties.class).getAffiliationConfig().getEffiliationApiKey())
                    .isEqualTo("yaml-key");
        }
    }

    /**
     * Starts a non-web Spring Boot application holding only {@link ApiProperties}, with the given map as OS
     * environment and {@link #YAML} as configuration file.
     */
    private ConfigurableApplicationContext start(Map<String, Object> osEnvironment) throws IOException {
        Path config = Files.writeString(tempDir.resolve("application-active.yml"), YAML);
        StandardEnvironment environment = new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addLast(new SystemEnvironmentPropertySource(
                        SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
            }
        };
        SpringApplication application = new SpringApplication(BindingConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setEnvironment(environment);
        return application.run("--spring.config.location=file:" + config);
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties
    @Import(ApiProperties.class)
    static class BindingConfig {
    }
}
