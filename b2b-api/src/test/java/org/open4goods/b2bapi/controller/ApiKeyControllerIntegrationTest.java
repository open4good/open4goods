package org.open4goods.b2bapi.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationMember;
import org.open4goods.b2bapi.model.OrganizationRole;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationMemberRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.open4goods.b2bapi.service.JwtTokenService;
import org.open4goods.b2bapi.service.JwtTokenType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.open4goods.services.productrepository.services.ProductRepository;

/**
 * Full REST integration tests for API-key lifecycle endpoints.
 * Verifies the HTTP/JWT-auth/RBAC seam; service-level logic is covered by ApiKeyServiceTest.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate"
})
class ApiKeyControllerIntegrationTest {

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
    private OrganizationRepository organizationRepository;

    @Autowired
    private OrganizationMemberRepository organizationMemberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private JwtTokenService jwtTokenService;

    @MockitoBean
    private ProductRepository productRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private MockMvc mockMvc;
    private Organization organization;
    private String ownerToken;
    private String billingOnlyToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        creditTransactionRepository.deleteAll();
        creditBucketRepository.deleteAll();
        apiKeyRepository.deleteAll();
        organizationMemberRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();

        organization = organizationRepository.save(buildActiveOrg("Test Org", "test-org"));

        final User owner = userRepository.save(new User("owner@example.com", OidcProvider.GITHUB, "owner"));
        organizationMemberRepository.save(new OrganizationMember(organization, owner, OrganizationRole.OWNER));
        ownerToken = "Bearer " + jwtTokenService.issueTokenPair(owner, organization).accessToken();

        final User billingUser = userRepository.save(new User("billing@example.com", OidcProvider.GITHUB, "billing"));
        organizationMemberRepository.save(new OrganizationMember(organization, billingUser, OrganizationRole.BILLING));
        billingOnlyToken = "Bearer " + jwtTokenService.issueTokenPair(billingUser, organization).accessToken();
    }

    @Test
    void unauthenticatedListReturns401() throws Exception {
        mockMvc.perform(get("/api/v1/customer/api-keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void billingRoleIsBlockedByRbac() throws Exception {
        mockMvc.perform(get("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, billingOnlyToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void ownerCanListEmptyKeys() throws Exception {
        mockMvc.perform(get("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void ownerCanCreateKey() throws Exception {
        final MvcResult result = mockMvc.perform(post("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "My Test Key"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearKey").isString())
                .andExpect(jsonPath("$.key.keyPrefix").isString())
                .andReturn();

        final String secret = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("clearKey").asText();
        assertThat(secret).startsWith("pdapi_");

        // Secret is returned once; the stored hash is not re-exposed in subsequent list
        mockMvc.perform(get("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void rotateReplacesSecretAndKeepsOldKeyRevoked() throws Exception {
        // Create
        final MvcResult createResult = mockMvc.perform(post("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Rotate Me"))))
                .andExpect(status().isOk())
                .andReturn();

        final String keyId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("key").get("id").asText();
        final String firstSecret = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("clearKey").asText();

        // Rotate
        final MvcResult rotateResult = mockMvc.perform(post("/api/v1/customer/api-keys/" + keyId + "/rotate")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clearKey").isString())
                .andReturn();

        final String newSecret = objectMapper.readTree(rotateResult.getResponse().getContentAsString())
                .get("clearKey").asText();

        assertThat(newSecret).startsWith("pdapi_").isNotEqualTo(firstSecret);
    }

    @Test
    void revokeFlipsStatusToRevoked() throws Exception {
        // Create
        final MvcResult createResult = mockMvc.perform(post("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "To Revoke"))))
                .andExpect(status().isOk())
                .andReturn();

        final String keyId = objectMapper.readTree(createResult.getResponse().getContentAsString())
                .get("key").get("id").asText();

        // Revoke
        mockMvc.perform(post("/api/v1/customer/api-keys/" + keyId + "/revoke")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REVOKED"));

        // Appears as revoked in list
        mockMvc.perform(get("/api/v1/customer/api-keys")
                        .header(HttpHeaders.AUTHORIZATION, ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("REVOKED"));
    }

    private Organization buildActiveOrg(final String name, final String slug) {
        final Organization org = new Organization(name, slug);
        org.setStatus(OrganizationStatus.ACTIVE);
        return org;
    }
}
