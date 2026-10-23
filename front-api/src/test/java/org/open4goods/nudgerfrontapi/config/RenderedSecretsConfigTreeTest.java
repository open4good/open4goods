package org.open4goods.nudgerfrontapi.config;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.open4goods.icecat.config.yml.IcecatConfiguration;
import org.open4goods.nudgerfrontapi.config.properties.AffiliationPartnersProperties;
import org.open4goods.nudgerfrontapi.config.properties.SecurityProperties;
import org.open4goods.services.captcha.config.HcaptchaProperties;
import org.open4goods.services.feedback.config.FeedbackConfiguration;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchProperties;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import de.codecentric.boot.admin.client.config.ClientProperties;

/**
 * Shows that the config tree {@code scripts/deploy/render-service-secrets.sh} renders for front-api (GOU-1041),
 * {@code /etc/open4goods/secrets/front-api/<property>} imported through the {@code SPRING_CONFIG_IMPORT} line of
 * {@code /etc/open4goods/front-api.env}, beats {@code application-active.yml} (open4goods-config) and
 * {@code application.yml} (including its {@code ${FRONT_SECURITY_JWT_SECRET:...}} defaults), loaded as
 * {@code publish-jars.sh} does ({@code spring.config.location}). Each property is read back through the class that
 * consumes it, so a misspelt file name fails here, and every {@code tree} row of the script's table for front-api must
 * be covered.
 */
class RenderedSecretsConfigTreeTest {

    private static final String SERVICE = "front-api";

    /** Each rendered property and how front-api reads it. */
    private static final Map<String, Function<Binder, String>> CONSUMERS = Map.ofEntries(
            entry("spring.boot.admin.client.password",
                    binder -> bind(binder, "spring.boot.admin.client", ClientProperties.class).getPassword()),
            entry("o4g.actuator.monitor.password",
                    binder -> bind(binder, "o4g.actuator.monitor", ActuatorMonitorCredentials.class).getPassword()),
            entry("front.security.jwt-secret",
                    binder -> bind(binder, "front.security", SecurityProperties.class).getJwtSecret()),
            entry("front.security.shared-token",
                    binder -> bind(binder, "front.security", SecurityProperties.class).getSharedToken()),
            entry("front.affiliation-partners.api-key",
                    binder -> bind(binder, "front.affiliation-partners", AffiliationPartnersProperties.class)
                            .getApiKey()),
            entry("icecat-feature-config.password",
                    binder -> bind(binder, "icecat-feature-config", IcecatConfiguration.class).getPassword()),
            entry("spring.mail.password", binder -> bind(binder, "spring.mail", MailProperties.class).getPassword()),
            entry("captcha.secret-key", binder -> bind(binder, "captcha", HcaptchaProperties.class).getSecretKey()),
            entry("feedback.github.access-token",
                    binder -> bind(binder, "feedback", FeedbackConfiguration.class).getGithub().getAccessToken()),
            entry("spring.elasticsearch.password",
                    binder -> bind(binder, "spring.elasticsearch", ElasticsearchProperties.class).getPassword()));

    @TempDir
    Path tempDir;

    @Test
    void configTreeBeatsApplicationActiveYml() throws IOException {
        Path tree = writeTree();
        try (ConfigurableApplicationContext context = start(Map.of("SPRING_CONFIG_IMPORT", configTreeImport(tree)))) {
            Binder binder = Binder.get(context.getEnvironment());
            CONSUMERS.forEach((property, consumer) ->
                    assertThat(consumer.apply(binder)).as(property).isEqualTo(treeValue(property)));
        }
    }

    @Test
    void applicationActiveYmlIsKeptWithoutTheImport() throws IOException {
        writeTree();
        try (ConfigurableApplicationContext context = start(Map.of())) {
            assertYamlValues(Binder.get(context.getEnvironment()));
        }
    }

    @Test
    void importOfAMissingTreeIsANoOp() throws IOException {
        try (ConfigurableApplicationContext context = start(
                Map.of("SPRING_CONFIG_IMPORT", configTreeImport(tempDir.resolve("secrets").resolve(SERVICE))))) {
            assertYamlValues(Binder.get(context.getEnvironment()));
        }
    }

    @Test
    void everyTreeRowOfTheRenderScriptIsCovered() throws IOException {
        assertThat(treeProperties(SERVICE)).isEqualTo(new TreeSet<>(CONSUMERS.keySet()));
    }

    private void assertYamlValues(Binder binder) {
        CONSUMERS.forEach((property, consumer) ->
                assertThat(consumer.apply(binder)).as(property).isEqualTo(yamlValue(property)));
    }

    /** Writes one file per property, as push-remote-file.sh --exact does (no trailing newline). */
    private Path writeTree() throws IOException {
        Path tree = Files.createDirectories(tempDir.resolve("secrets").resolve(SERVICE));
        for (String property : CONSUMERS.keySet()) {
            Files.writeString(tree.resolve(property), treeValue(property));
        }
        return tree;
    }

    private static String treeValue(String property) {
        return "tree \"" + property + "\" #1 $HOME: ok";
    }

    private static String yamlValue(String property) {
        return "yaml-" + property.replace('.', '-');
    }

    private static String configTreeImport(Path tree) {
        return "optional:configtree:" + tree + "/";
    }

    /** Binds into a new instance, as {@code @ConfigurationProperties} beans are. */
    private static <T> T bind(Binder binder, String prefix, Class<T> type) {
        return binder.bind(prefix, Bindable.ofInstance(BeanUtils.instantiateClass(type))).get();
    }

    /** The {@code tree} and {@code tree-sa} targets of the service in render-service-secrets.sh. */
    static Set<String> treeProperties(String service) throws IOException {
        String script = Files.readString(Path.of("..", "scripts", "deploy", "render-service-secrets.sh"));
        Matcher row = Pattern.compile("(?m)^" + Pattern.quote(service) + " \\S+ tree(?:-sa)? (\\S+) ").matcher(script);
        Set<String> properties = new TreeSet<>();
        while (row.find()) {
            properties.add(row.group(1));
        }
        return properties;
    }

    /**
     * Starts a non-web Spring Boot application without beans, with the given map as OS environment and the module's
     * {@code application.yml} plus an {@code application-active.yml} setting every property, located as
     * {@code publish-jars.sh} does.
     */
    private ConfigurableApplicationContext start(Map<String, Object> osEnvironment) throws IOException {
        StringBuilder yaml = new StringBuilder();
        CONSUMERS.keySet().stream().sorted()
                .forEach(property -> yaml.append(property).append(": ").append(yamlValue(property)).append('\n'));
        Path config = Files.writeString(tempDir.resolve("application-active.yml"), yaml);
        StandardEnvironment environment = new StandardEnvironment() {
            @Override
            protected void customizePropertySources(MutablePropertySources propertySources) {
                propertySources.addLast(new SystemEnvironmentPropertySource(
                        SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, osEnvironment));
            }
        };
        SpringApplication application = new SpringApplication(EmptyConfig.class);
        application.setWebApplicationType(WebApplicationType.NONE);
        application.setBannerMode(Banner.Mode.OFF);
        application.setLogStartupInfo(false);
        application.setEnvironment(environment);
        return application.run("--spring.config.location=classpath:/application.yml,file:" + config,
                "--spring.main.web-application-type=none", "--spring.docker.compose.enabled=false");
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {
    }
}
