package org.open4goods.api.services.metriks.affiliation;

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
import org.open4goods.services.contribution.service.AffiliationClickStatsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

/**
 * Shows that the {@code METRIKS_AFFILIATION_*} OS environment variables, rendered into
 * {@code /etc/open4goods/api.env} by {@code scripts/deploy/render-service-secrets.sh} and exported by
 * {@code publish-jars.sh} (GOU-957), reach the {@code metriks.affiliation.*} properties of the Metriks affiliation
 * clients through Spring Boot's relaxed binding, and that the properties default to empty (missing credential).
 */
class AffiliationCredentialsBindingTest {

    private static final Map<String, Object> RENDERED = Map.of(
            "METRIKS_AFFILIATION_AWIN_PUBLISHER_ID", "100000",
            "METRIKS_AFFILIATION_AWIN_API_TOKEN", "awin-token",
            "METRIKS_AFFILIATION_EFFILIATION_API_KEY", "effi-key");

    @TempDir
    Path tempDir;

    @Test
    void osEnvironmentVariablesBindToTheAffiliationProperties() throws IOException {
        try (ConfigurableApplicationContext context = start(RENDERED)) {
            AwinReportClient awin = context.getBean(AwinReportClient.class);
            assertThat(awin).extracting("publisherId", "apiToken").containsExactly("100000", "awin-token");
            assertThat(awin.missingCredentials()).isEmpty();

            EffiliationReportClient effiliation = context.getBean(EffiliationReportClient.class);
            assertThat(effiliation).extracting("apiKey").isEqualTo("effi-key");
            assertThat(effiliation.missingCredentials()).isEmpty();

            assertThat(context.getBean(AffiliationMetriksService.class)).extracting("clients")
                    .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST).hasSize(2);
        }
    }

    @Test
    void propertiesDefaultToEmpty_soEveryCredentialIsReportedMissing() throws IOException {
        try (ConfigurableApplicationContext context = start(Map.of())) {
            assertThat(context.getBean(AwinReportClient.class).missingCredentials()).containsExactly(
                    "metriks.affiliation.awin.publisher-id", "metriks.affiliation.awin.api-token");
            assertThat(context.getBean(EffiliationReportClient.class).missingCredentials())
                    .containsExactly("metriks.affiliation.effiliation.api-key");
        }
    }

    /**
     * Starts a non-web Spring Boot application holding only the Metriks affiliation beans, with the given map as OS
     * environment and an empty configuration file.
     */
    private ConfigurableApplicationContext start(Map<String, Object> osEnvironment) throws IOException {
        Path config = Files.writeString(tempDir.resolve("application-active.yml"), "metriks:\n  gsc:\n    window-days: 28\n");
        StandardEnvironment environment = new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addLast(new SystemEnvironmentPropertySource(
                        SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
            }
        };
        SpringApplication application = new SpringApplication(AffiliationConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setEnvironment(environment);
        return application.run("--spring.config.location=file:" + config);
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ AwinReportClient.class, EffiliationReportClient.class, AffiliationMetriksService.class })
    static class AffiliationConfig {

        /** Click reader without Elasticsearch: only the credential binding is under test. */
        @Bean
        AffiliationClickStatsService affiliationClickStatsService() {
            return AffiliationTestSupport.noClicks();
        }
    }
}
