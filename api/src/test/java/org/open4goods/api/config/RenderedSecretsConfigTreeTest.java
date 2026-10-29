package org.open4goods.api.config;

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
import org.open4goods.api.config.yml.ApiProperties;
import org.open4goods.services.eprelservice.config.EprelServiceProperties;
import org.open4goods.services.googlesearch.config.GoogleSearchConfig;
import org.open4goods.services.prompt.config.PromptServiceConfig;
import org.open4goods.services.prompt.config.VertexBatchConfig;
import org.springframework.ai.model.openai.autoconfigure.OpenAiCommonProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.boot.Banner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.elasticsearch.autoconfigure.ElasticsearchProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.env.SystemEnvironmentPropertySource;

import de.codecentric.boot.admin.client.config.ClientProperties;

/**
 * Shows that the config tree {@code scripts/deploy/render-service-secrets.sh} renders for api (GOU-1041),
 * {@code /etc/open4goods/secrets/api/<property>} imported through the {@code SPRING_CONFIG_IMPORT} line of
 * {@code /etc/open4goods/api.env}, beats {@code application-active.yml} (open4goods-config) and
 * {@code application.yml}, loaded as {@code publish-jars.sh} does ({@code spring.config.location}). Each property is
 * read back through the class that consumes it, so a misspelt file name fails here, and every {@code tree} row of the
 * script's table for api must be covered.
 */
class RenderedSecretsConfigTreeTest {

    private static final String SERVICE = "api";

    /** A service account key: quotes, newlines and braces, which no env line can carry. */
    private static final String SERVICE_ACCOUNT_JSON = """
            {
              "type": "service_account",
              "private_key": "-----BEGIN PRIVATE KEY-----\\nfake\\n-----END PRIVATE KEY-----\\n",
              "client_email": "fake@example.iam.gserviceaccount.com"
            }""";

    /** Each rendered property and how api reads it. */
    private static final Map<String, Function<Binder, String>> CONSUMERS = Map.ofEntries(
            entry("gen-ai-config.google-api-json",
                    binder -> bind(binder, "gen-ai-config", PromptServiceConfig.class).getGoogleApiJson()),
            entry("vertex.batch.credentials-json",
                    binder -> bind(binder, "vertex.batch", VertexBatchConfig.class).getCredentialsJson()),
            entry("googlesearch.api-key",
                    binder -> bind(binder, "googlesearch", GoogleSearchConfig.class).getApiKey()),
            entry("spring.ai.openai.api-key",
                    binder -> bind(binder, "spring.ai.openai", OpenAiCommonProperties.class).getApiKey()),
            entry("affiliation-config.awin-access-token",
                    binder -> api(binder).getAffiliationConfig().getAwinAccessToken()),
            entry("affiliation-config.kwanko-token", binder -> api(binder).getAffiliationConfig().getKwankoToken()),
            entry("affiliation-config.tradetracker-api-key",
                    binder -> api(binder).getAffiliationConfig().getTradetrackerApiKey()),
            entry("eprel.api-key", binder -> bind(binder, "eprel", EprelServiceProperties.class).getApiKey()),
            entry("icecat-feature-config.password", binder -> api(binder).getIcecatFeatureConfig().getPassword()),
            entry("spring.boot.admin.client.password",
                    binder -> bind(binder, "spring.boot.admin.client", ClientProperties.class).getPassword()),
            entry("admin-key", binder -> api(binder).getAdminKey()),
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
        return property.endsWith("-json") ? SERVICE_ACCOUNT_JSON : "tree \"" + property + "\" #1 $HOME: ok";
    }

    private static String yamlValue(String property) {
        return "yaml-" + property.replace('.', '-');
    }

    private static String configTreeImport(Path tree) {
        return "optional:configtree:" + tree + "/";
    }

    private static ApiProperties api(Binder binder) {
        return bind(binder, "", ApiProperties.class);
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
