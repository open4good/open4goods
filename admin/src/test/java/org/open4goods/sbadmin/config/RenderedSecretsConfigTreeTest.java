package org.open4goods.sbadmin.config;

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
import org.springframework.beans.BeanUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.mail.autoconfigure.MailProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties;
import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientPropertiesMapper;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;

import de.codecentric.boot.admin.server.config.AdminServerProperties;

/**
 * Shows that the config tree {@code scripts/deploy/render-service-secrets.sh} renders for admin (GOU-1041), which runs
 * as {@code sbadmin}: {@code /etc/open4goods/secrets/sbadmin/<property>}, imported through the
 * {@code SPRING_CONFIG_IMPORT} line of {@code /etc/open4goods/sbadmin.env}, beats {@code application-active.yml}
 * (open4goods-config), the only file {@code publish-jars.sh} loads for sbadmin ({@code spring.config.location}). Each
 * property is read back through the class that consumes it, so a misspelt file name fails here, and every {@code tree}
 * row of the script's table for admin must be covered.
 */
class RenderedSecretsConfigTreeTest {

    private static final String SERVICE = "admin";

    /** Each rendered property and how admin reads it. */
    private static final Map<String, Function<Binder, String>> CONSUMERS = Map.ofEntries(
            entry("spring.boot.admin.instance-auth.default-password",
                    binder -> bind(binder, "spring.boot.admin", AdminServerProperties.class).getInstanceAuth()
                            .getDefaultPassword()),
            entry("o4g.sba.registration.password",
                    binder -> bind(binder, "o4g.sba.registration", SbaRegistrationCredentials.class).getPassword()),
            entry("spring.mail.password", binder -> bind(binder, "spring.mail", MailProperties.class).getPassword()),
            // The site's Google SSO client (GOOGLE_SSO_*, GOU-1091), read as the oauth2Login registration.
            entry("spring.security.oauth2.client.registration.google.client-id",
                    binder -> googleRegistration(binder).getClientId()),
            entry("spring.security.oauth2.client.registration.google.client-secret",
                    binder -> googleRegistration(binder).getClientSecret()));

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
                Map.of("SPRING_CONFIG_IMPORT", configTreeImport(tempDir.resolve("secrets").resolve("sbadmin"))))) {
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
        Path tree = Files.createDirectories(tempDir.resolve("secrets").resolve("sbadmin"));
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

    /** The {@code google} client registration, mapped as Spring Boot builds the admin's ClientRegistrationRepository. */
    private static ClientRegistration googleRegistration(Binder binder) {
        OAuth2ClientProperties properties = bind(binder, "spring.security.oauth2.client", OAuth2ClientProperties.class);
        return new OAuth2ClientPropertiesMapper(properties).asClientRegistrations().get("google");
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
     * Starts a non-web Spring Boot application without beans, with the given map as OS environment and only an
     * {@code application-active.yml} setting every property, located as {@code publish-jars.sh} does for sbadmin
     * (no {@code application.yml}).
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
        return application.run("--spring.config.location=file:" + config,
                "--spring.main.web-application-type=none", "--spring.docker.compose.enabled=false");
    }

    @Configuration(proxyBeanMethods = false)
    static class EmptyConfig {
    }
}
