package org.open4goods.api.services.metriks;

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
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Shows that the {@code GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH} OS environment variable, rendered into
 * {@code /etc/open4goods/api.env} by {@code scripts/deploy/render-service-secrets.sh} and exported by
 * {@code publish-jars.sh} (GOU-933), reaches the {@code @Value("${google-indexation.service-account-path:}")}
 * of {@link GoogleSearchConsoleService} through Spring Boot's relaxed binding, and beats the value of the
 * configuration file ({@code application-active.yml} in production).
 */
class GoogleSearchConsoleServiceBindingTest {

    private static final String RENDERED_PATH = "/etc/open4goods/secrets/google-indexation.json";
    private static final String CONFIG_FILE_PATH = "/opt/open4goods/config/google-api.json";

    @TempDir
    Path tempDir;

    @Test
    void osEnvironmentVariableBindsToServiceAccountPath() throws IOException {
        try (ConfigurableApplicationContext context = start(
                Map.of("GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH", RENDERED_PATH), "metriks:\n  gsc:\n    window-days: 28\n")) {
            assertThat(context.getBean(GoogleSearchConsoleService.class))
                    .extracting("serviceAccountPath")
                    .isEqualTo(RENDERED_PATH);
        }
    }

    @Test
    void osEnvironmentVariableBeatsConfigurationFile() throws IOException {
        try (ConfigurableApplicationContext context = start(
                Map.of("GOOGLE_INDEXATION_SERVICE_ACCOUNT_PATH", RENDERED_PATH),
                "google-indexation:\n  service-account-path: " + CONFIG_FILE_PATH + "\n")) {
            assertThat(context.getBean(GoogleSearchConsoleService.class))
                    .extracting("serviceAccountPath")
                    .isEqualTo(RENDERED_PATH);
        }
    }

    @Test
    void configurationFileApplies_whenTheVariableIsAbsent() throws IOException {
        try (ConfigurableApplicationContext context = start(Map.of(),
                "google-indexation:\n  service-account-path: " + CONFIG_FILE_PATH + "\n")) {
            assertThat(context.getBean(GoogleSearchConsoleService.class))
                    .extracting("serviceAccountPath")
                    .isEqualTo(CONFIG_FILE_PATH);
        }
    }

    /**
     * Starts a non-web Spring Boot application holding only {@link GoogleSearchConsoleService}, with the given
     * map as OS environment and the given YAML as its only configuration file.
     */
    private ConfigurableApplicationContext start(Map<String, Object> osEnvironment, String yaml) throws IOException {
        Path config = Files.writeString(tempDir.resolve("application-active.yml"), yaml);
        StandardEnvironment environment = new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addLast(new SystemEnvironmentPropertySource(
                        SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
            }
        };
        SpringApplication application = new SpringApplication(MetriksConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setEnvironment(environment);
        return application.run("--spring.config.location=file:" + config);
    }

    @Configuration(proxyBeanMethods = false)
    @Import(GoogleSearchConsoleService.class)
    static class MetriksConfig {
    }
}
