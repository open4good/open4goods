package org.open4goods.b2bapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeMetadata;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeOptions;
import org.open4goods.b2bapi.dto.barcode.B2bBarcodeRenderRequest;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.BarcodeAsset;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.BarcodeAssetRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.open4goods.b2bapi.service.ApiKeySecretGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Controller integration tests for Barcode API endpoints.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class BarcodeControllerIntegrationTest {

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
    private StringRedisTemplate redisTemplate;

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
    private BarcodeAssetRepository barcodeAssetRepository;

    @Autowired
    private ApiKeySecretGenerator secretGenerator;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private Organization organization;
    private String apiKeyHeader;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();

        barcodeAssetRepository.deleteAll();
        creditTransactionRepository.deleteAll();
        creditBucketRepository.deleteAll();
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

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
        final B2bBarcodeRenderRequest req = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"), null
        );

        mockMvc.perform(post("/api/v1/barcodes/render")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rendersBarcodeSuccessfully() throws Exception {
        saveBucket(organization, 10);

        final B2bBarcodeRenderRequest req = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true,
                new B2bBarcodeOptions(300, 0.33, 15.0, 8.0, "print-safe"),
                new B2bBarcodeMetadata("Antigravity Copy", "Antigravity", "Desc")
        );

        final MvcResult result = mockMvc.perform(post("/api/v1/barcodes/render")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "1"))
                .andExpect(header().string("X-Credits-Remaining", "9"))
                .andExpect(jsonPath("$.assetUrl").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andReturn();

        // Extract token from asset URL and retrieve the asset publicly
        final String jsonResponse = result.getResponse().getContentAsString();
        final String assetUrl = objectMapper.readTree(jsonResponse).get("assetUrl").asText();
        final String token = assetUrl.substring(assetUrl.lastIndexOf('/') + 1);

        mockMvc.perform(get("/api/v1/barcodes/assets/" + token))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "public, max-age=2592000"))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.IMAGE_PNG_VALUE));
    }

    @Test
    void rendersBarcodeZipSuccessfully() throws Exception {
        saveBucket(organization, 10);

        final B2bBarcodeRenderRequest req1 = new B2bBarcodeRenderRequest(
                "ean13", "4006381333931", "png", 200, 100, "#000000", "#ffffff", 0, true, true, null, null
        );
        final B2bBarcodeRenderRequest req2 = new B2bBarcodeRenderRequest(
                "qr", "Hello world", "svg", 200, 200, "#000000", "#ffffff", 0, true, true, null, null
        );

        final List<B2bBarcodeRenderRequest> reqs = List.of(req1, req2);

        mockMvc.perform(post("/api/v1/barcodes/render-zip")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqs)))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "2"))
                .andExpect(header().string("X-Credits-Remaining", "8"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"barcodes.zip\""));
    }

    @Test
    void publicAssetEndpointFailsIfExpiredOrNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/barcodes/assets/non_existent_token"))
                .andExpect(status().isNotFound());

        // expired token setup
        final BarcodeAsset expiredAsset = new BarcodeAsset("expired_tok", new byte[]{1, 2, 3}, "image/png", Instant.now().minusSeconds(10));
        barcodeAssetRepository.save(expiredAsset);

        mockMvc.perform(get("/api/v1/barcodes/assets/expired_tok"))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicBarcodeCheckRequiresNoAuth() throws Exception {
        mockMvc.perform(get("/api/v1/barcodes/check")
                        .param("barcode", "3017620422003"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.barcode").value("3017620422003"))
                .andExpect(jsonPath("$.forensics.valid").value(true))
                .andExpect(jsonPath("$.forensics.type").value("GTIN_13"))
                .andExpect(jsonPath("$.forensics.gs1Prefix").value("301"))
                .andExpect(jsonPath("$.forensics.normalizedGtin13").value("3017620422003"))
                .andExpect(jsonPath("$.forensics.normalizedGtin14").value("03017620422003"))
                .andExpect(jsonPath("$.forensics.checkDigit").value(3));
    }

    @Test
    void publicBarcodeCheckInvalidChecksumReturnsValidFalse() throws Exception {
        mockMvc.perform(get("/api/v1/barcodes/check")
                        .param("barcode", "3017620422004"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forensics.valid").value(false));
    }

    @Test
    void authenticatedBarcodeCheckSetsZeroCreditHeaders() throws Exception {
        saveBucket(organization, 5);

        mockMvc.perform(get("/api/v1/barcodes/3017620422003/check")
                        .header(HttpHeaders.AUTHORIZATION, apiKeyHeader))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Credits-Consumed", "0"))
                .andExpect(jsonPath("$.forensics.valid").value(true))
                .andExpect(jsonPath("$.forensics.type").value("GTIN_13"));
    }

    @Test
    void authenticatedBarcodeCheckRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/v1/barcodes/3017620422003/check"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void publicBarcodeCheckIsbn13IsClassifiedCorrectly() throws Exception {
        mockMvc.perform(get("/api/v1/barcodes/check")
                        .param("barcode", "9781845924539"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.forensics.valid").value(true))
                .andExpect(jsonPath("$.forensics.type").value("ISBN_13"))
                .andExpect(jsonPath("$.forensics.gs1Class").value("ISBN_BOOKLAND"))
                .andExpect(jsonPath("$.forensics.isbnRegistrationGroup").value("1"));
    }

    private void saveBucket(final Organization org, final long credits) {
        final CreditBucket bucket = new CreditBucket(org, CreditBucketKind.PACK, credits, credits);
        bucket.setCreatedAt(Instant.now());
        creditBucketRepository.save(bucket);
    }
}
