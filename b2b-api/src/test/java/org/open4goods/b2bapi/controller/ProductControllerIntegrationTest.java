package org.open4goods.b2bapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.open4goods.b2bapi.service.ApiKeySecretGenerator;
import org.open4goods.b2bapi.service.RedisMeteringService;
import org.open4goods.model.price.AggregatedPrice;
import org.open4goods.model.price.AggregatedPrices;
import org.open4goods.model.product.Product;
import org.open4goods.model.product.ProductCondition;
import org.open4goods.services.productrepository.services.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
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
 * Full REST controller integration tests for the B2B Product Price endpoint.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ProductControllerIntegrationTest {

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

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private ApiKeySecretGenerator secretGenerator;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private RedisMeteringService redisMeteringService;

    @MockitoBean
    private ProductRepository productRepository;

    private MockMvc mockMvc;
    private Organization organization;
    private String apiKeyHeader;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        creditTransactionRepository.deleteAll();
        creditBucketRepository.deleteAll();
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        organization = new Organization("Integration Org", "integration-org");
        organization.setStatus(OrganizationStatus.ACTIVE);
        organization = organizationRepository.save(organization);

        final User user = userRepository.save(new User("tester@example.com", OidcProvider.GITHUB, "tester"));

        final String rawKey = "pdapi_testkey1234567890";
        final ApiKey apiKey = new ApiKey(
                organization,
                user,
                "Test key",
                "pdapi_testke",
                secretGenerator.sha256Hex(rawKey));
        apiKey.setStatus(ApiKeyStatus.ACTIVE);
        apiKeyRepository.save(apiKey);

        apiKeyHeader = "Bearer " + rawKey;
    }

    @Test
    void unauthenticatedReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/products/885909950805/price"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void revokedKeyReturns401() throws Exception {
        final ApiKey revokedKey = apiKeyRepository.findAll().get(0);
        revokedKey.setStatus(ApiKeyStatus.REVOKED);
        apiKeyRepository.save(revokedKey);

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void invalidGtinReturns400AndConsumesZeroCredits() throws Exception {
        mockMvc.perform(get("/api/v1/products/1234567/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isBadRequest())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(jsonPath("$.type").value("https://product-data-api.com/problems/invalid-gtin"))
                .andExpect(jsonPath("$.title").value("Invalid GTIN"));

        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(StreamOffset.fromStart("b2b:usage"));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("httpStatus").toString()).isEqualTo("400");
        assertThat(records.get(0).getValue().get("creditsConsumed").toString()).isEqualTo("0");
        assertThat(records.get(0).getValue().get("noPayReason").toString()).isEqualTo("invalid-gtin");
    }

    @Test
    void missingProductReturns404AndRefundsReservedCredits() throws Exception {
        saveBucket(organization, 10);

        when(productRepository.getByIdWithoutEmbedding(885909950805L))
                .thenThrow(new org.open4goods.model.exceptions.ResourceNotFoundException("Product not found."));

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isNotFound())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(header().string("X-Credits-Remaining", "10"))
                .andExpect(jsonPath("$.type").value("https://product-data-api.com/problems/product-not-found"))
                .andExpect(jsonPath("$.title").value("Product not found"));

        final String balanceKey = redisMeteringService.balanceKey(organization.getId());
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("10");

        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(StreamOffset.fromStart("b2b:usage"));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("httpStatus").toString()).isEqualTo("404");
        assertThat(records.get(0).getValue().get("creditsConsumed").toString()).isEqualTo("0");
        assertThat(records.get(0).getValue().get("noPayReason").toString()).isEqualTo("not-found");
    }

    @Test
    void noFreshOffersReturns200WithZeroCreditsConsumed() throws Exception {
        saveBucket(organization, 10);

        final Product product = new Product(885909950805L);
        final AggregatedPrices prices = new AggregatedPrices();
        final AggregatedPrice offer = new AggregatedPrice();
        offer.setPrice(19.99);
        offer.setCurrency(org.open4goods.model.price.Currency.EUR);
        offer.setProductState(ProductCondition.NEW);
        offer.setTimeStamp(Instant.now().minusSeconds(40 * 86400L).toEpochMilli());
        prices.setOffers(Set.of(offer));
        product.setPrice(prices);

        when(productRepository.getByIdWithoutEmbedding(885909950805L)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(header().string("X-Credits-Remaining", "10"))
                .andExpect(jsonPath("$.data.offersCount").value(1))
                .andExpect(jsonPath("$.data.freshOffersCount").value(0));

        final String balanceKey = redisMeteringService.balanceKey(organization.getId());
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("10");

        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(StreamOffset.fromStart("b2b:usage"));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("httpStatus").toString()).isEqualTo("200");
        assertThat(records.get(0).getValue().get("creditsConsumed").toString()).isEqualTo("0");
        assertThat(records.get(0).getValue().get("noPayReason").toString()).isEqualTo("no-fresh-offer");
    }

    @Test
    void freshOffersReturns200AndDebitsFiveCredits() throws Exception {
        saveBucket(organization, 10);

        final Product product = new Product(885909950805L);
        final AggregatedPrices prices = new AggregatedPrices();
        final AggregatedPrice offer = new AggregatedPrice();
        offer.setPrice(19.99);
        offer.setCurrency(org.open4goods.model.price.Currency.EUR);
        offer.setProductState(ProductCondition.NEW);
        offer.setTimeStamp(Instant.now().toEpochMilli());
        prices.setOffers(Set.of(offer));
        product.setPrice(prices);

        when(productRepository.getByIdWithoutEmbedding(885909950805L)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "5"))
                .andExpect(header().string("X-Credits-Remaining", "5"))
                .andExpect(jsonPath("$.data.offersCount").value(1))
                .andExpect(jsonPath("$.data.freshOffersCount").value(1));

        final String balanceKey = redisMeteringService.balanceKey(organization.getId());
        assertThat(redisTemplate.opsForValue().get(balanceKey)).isEqualTo("5");
        assertThat(creditBucketRepository.sumLiveCredits(organization.getId())).isEqualTo(5);
        assertThat(creditTransactionRepository.findByOrganizationId(organization.getId(), 10)).hasSize(1);

        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(StreamOffset.fromStart("b2b:usage"));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("httpStatus").toString()).isEqualTo("200");
        assertThat(records.get(0).getValue().get("creditsConsumed").toString()).isEqualTo("5");
        assertThat(records.get(0).getValue().get("billable").toString()).isEqualTo("true");
    }

    @Test
    void insufficientCreditsReturns402PaymentRequired() throws Exception {
        saveBucket(organization, 3);

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isPaymentRequired())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(header().string("X-Credits-Remaining", "3"))
                .andExpect(jsonPath("$.type").value("https://product-data-api.com/problems/insufficient-credits"))
                .andExpect(jsonPath("$.title").value("Insufficient credits"));

        final List<MapRecord<String, Object, Object>> records = redisTemplate.opsForStream().read(StreamOffset.fromStart("b2b:usage"));
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getValue().get("httpStatus").toString()).isEqualTo("402");
        assertThat(records.get(0).getValue().get("creditsConsumed").toString()).isEqualTo("0");
    }

    @Test
    void duplicateRequestIdDebitsOnceIdempotently() throws Exception {
        saveBucket(organization, 10);

        final Product product = new Product(885909950805L);
        final AggregatedPrices prices = new AggregatedPrices();
        final AggregatedPrice offer = new AggregatedPrice();
        offer.setPrice(19.99);
        offer.setCurrency(org.open4goods.model.price.Currency.EUR);
        offer.setProductState(ProductCondition.NEW);
        offer.setTimeStamp(Instant.now().toEpochMilli());
        prices.setOffers(Set.of(offer));
        product.setPrice(prices);

        when(productRepository.getByIdWithoutEmbedding(885909950805L)).thenReturn(product);

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader)
                        .header("X-Request-Id", "pdreq_idempotenttest123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "5"))
                .andExpect(header().string("X-Credits-Remaining", "5"));

        mockMvc.perform(get("/api/v1/products/885909950805/price")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader)
                        .header("X-Request-Id", "pdreq_idempotenttest123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(header().string("X-Credits-Remaining", "5"));

        assertThat(creditBucketRepository.sumLiveCredits(organization.getId())).isEqualTo(5);
    }

    private void saveBucket(final Organization org, final long credits) {
        final CreditBucket bucket = new CreditBucket(org, CreditBucketKind.PACK, credits, credits);
        bucket.setCreatedAt(Instant.now());
        creditBucketRepository.save(bucket);
    }
}
