package org.open4goods.services.productalert.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productalert.ProductAlertApplication;
import org.open4goods.services.productalert.model.ProductAlertSubscription;
import org.springframework.boot.autoconfigure.elasticsearch.RestClientBuilderCustomizer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.elasticsearch.client.RestClientBuilder;
import org.apache.http.impl.nio.client.HttpAsyncClientBuilder;

/**
 * Integration tests for {@link ProductAlertSubscriptionRepository}.
 */
@SpringBootTest(
        classes = {
                ProductAlertApplication.class,
                ProductAlertSubscriptionRepositoryTest.ElasticsearchTestConfiguration.class
        },
        properties = {
                "embedding.enabled=false"
        },
        webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@TestInstance(Lifecycle.PER_CLASS)
class ProductAlertSubscriptionRepositoryTest
{
    private static final String ELASTICSEARCH_PASSWORD = "test-password";

    @Container
    static final ElasticsearchContainer ELASTICSEARCH = new ElasticsearchContainer(
            "docker.elastic.co/elasticsearch/elasticsearch:8.15.3")
                    .withPassword(ELASTICSEARCH_PASSWORD);

    @Autowired
    private ProductAlertSubscriptionRepository repository;

    @DynamicPropertySource
    static void elasticsearchProperties(DynamicPropertyRegistry registry)
    {
        if (!ELASTICSEARCH.isRunning())
        {
            ELASTICSEARCH.start();
        }
        registry.add("spring.elasticsearch.uris", () -> "https://" + ELASTICSEARCH.getHttpHostAddress());
        registry.add("spring.elasticsearch.username", () -> "elastic");
        registry.add("spring.elasticsearch.password", () -> ELASTICSEARCH_PASSWORD);
    }

    @BeforeEach
    void setUp()
    {
        repository.deleteAll();
    }

    @Test
    void findByEnabledTrueAndGtinAndConditionReturnsMatchingSubscriptions()
    {
        ProductAlertSubscription subscription = new ProductAlertSubscription();
        subscription.setId("alice@example.org#1234567890128#NEW");
        subscription.setEmail("alice@example.org");
        subscription.setGtin(1234567890128L);
        subscription.setCondition(ProductCondition.NEW);
        subscription.setEnabled(true);
        subscription.setAlertOnDecrease(true);
        subscription.setCreatedAt(Instant.parse("2026-03-13T10:15:30Z"));
        subscription.setUpdatedAt(Instant.parse("2026-03-13T10:15:30Z"));
        repository.save(subscription);

        List<ProductAlertSubscription> matches = repository.findByEnabledTrueAndGtinAndCondition(1234567890128L, ProductCondition.NEW);

        assertThat(matches).hasSize(1);
        assertThat(matches.getFirst().getEmail()).isEqualTo("alice@example.org");
    }

    /**
     * Test-only Elasticsearch client customization for the container CA.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class ElasticsearchTestConfiguration
    {
        /**
         * Configures the test client to trust the Elasticsearch container CA.
         *
         * @return Spring Boot Elasticsearch customizer
         */
        @Bean
        RestClientBuilderCustomizer elasticsearchRestClientBuilderCustomizer()
        {
            return new RestClientBuilderCustomizer()
            {
                @Override
                public void customize(RestClientBuilder builder)
                {
                    // No-op, SSL is configured on the underlying async client.
                }

                @Override
                public void customize(HttpAsyncClientBuilder builder)
                {
                    builder.setSSLContext(ELASTICSEARCH.createSslContextFromCa());
                }
            };
        }
    }
}
