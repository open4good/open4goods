package org.open4goods.nudgerfrontapi.service.css;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.open4goods.nudgerfrontapi.config.properties.CssProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

/** The client wiring depends only on the configuration: default start needs no credential. */
class CssSyncConfigTest {

    @Configuration
    @EnableConfigurationProperties(CssProperties.class)
    static class PropertiesOnly {
    }

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesOnly.class, CssSyncConfig.class);

    @Test
    void defaultConfigurationCreatesNoClientAndNeedsNoCredential() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).doesNotHaveBean(CssProductsClient.class);
            assertThat(context).doesNotHaveBean(GoogleCssProductsClient.class);
        });
    }

    @Test
    void syncDisabledCreatesNoGoogleClientEvenWithCredentialsConfigured() {
        runner.withPropertyValues("front.css.enabled=true", "front.css.sync.enabled=false",
                "front.css.sync.account-id=123", "front.css.sync.service-account-json={}").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).doesNotHaveBean(CssProductsClient.class);
                });
    }

    @Test
    void simulatedModeCreatesTheSimulatedClientOnly() {
        runner.withPropertyValues("front.css.enabled=true", "front.css.sync.enabled=true",
                "front.css.sync.simulated=true").run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(SimulatedCssProductsClient.class);
                    assertThat(context).doesNotHaveBean(GoogleCssProductsClient.class);
                });
    }

    @Test
    void enabledWithoutCredentialFailsAtStartup() {
        runner.withPropertyValues("front.css.enabled=true", "front.css.sync.enabled=true",
                "front.css.sync.account-id=123").run(context -> assertThat(context).hasFailed());
    }
}
