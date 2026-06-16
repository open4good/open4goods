package org.open4goods.b2bapi.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.exception.B2bApiException;
import org.open4goods.b2bapi.exception.ErrorCode;
import org.open4goods.b2bapi.service.StripeWebhookService;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for the Stripe webhook HTTP endpoint.
 * StripeWebhookService is mocked; these tests verify the HTTP/security seam:
 * the endpoint is publicly accessible, delegates to the service, and maps exceptions correctly.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class StripeWebhookControllerIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    private static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void dynamicProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private StripeWebhookService stripeWebhookService;

    @MockitoBean
    private ProductRepository productRepository;

    private MockMvc mockMvc;

    private static final String SAMPLE_PAYLOAD = """
            {"id":"evt_test_001","object":"event","type":"checkout.session.completed"}
            """;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void endpointIsAccessibleWithoutAuthentication() throws Exception {
        doNothing().when(stripeWebhookService).processWebhook(any(), any());

        mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=1234,v1=abc")
                        .content(SAMPLE_PAYLOAD))
                .andExpect(status().isOk());

        verify(stripeWebhookService).processWebhook(any(), any());
    }

    @Test
    void invalidSignatureReturns400() throws Exception {
        doThrow(new B2bApiException(ErrorCode.INVALID_PARAMETER, "Invalid webhook signature"))
                .when(stripeWebhookService).processWebhook(any(), any());

        mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "t=0,v1=bad")
                        .content(SAMPLE_PAYLOAD))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.type").value(
                        "https://product-data-api.com/problems/invalid-parameter"));
    }

    @Test
    void missingSignatureHeaderReturns400() throws Exception {
        mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(SAMPLE_PAYLOAD))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateEventIsIdempotentlyIgnored() throws Exception {
        doNothing().when(stripeWebhookService).processWebhook(any(), any());

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/billing/stripe/webhook")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", "t=1234,v1=abc")
                            .content(SAMPLE_PAYLOAD))
                    .andExpect(status().isOk());
        }
        // Service called twice — idempotency is handled inside the service, not the controller
        verify(stripeWebhookService, org.mockito.Mockito.times(2)).processWebhook(any(), any());
    }
}
