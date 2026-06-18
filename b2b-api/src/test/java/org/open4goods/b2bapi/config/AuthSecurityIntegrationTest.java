package org.open4goods.b2bapi.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.OrganizationStatus;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.open4goods.b2bapi.service.ApiKeySecretGenerator;
import org.open4goods.b2bapi.service.B2bProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Servlet-security integration tests for dashboard JWT and Product API-key 401 behavior.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
class AuthSecurityIntegrationTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);

    static {
        POSTGRES.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void postgresProperties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
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
    private ApiKeySecretGenerator secretGenerator;

    @MockitoBean
    private StringRedisTemplate redisTemplate;

    @MockitoBean
    private B2bProductService b2bProductService;

    private ValueOperations<String, String> valueOperations;
    private MockMvc mockMvc;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        valueOperations = org.mockito.Mockito.mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        apiKeyRepository.deleteAll();
        userRepository.deleteAll();
        organizationRepository.deleteAll();
    }

    @Test
    void productEndpoint401MatrixRejectsMissingMalformedUnknownRevokedAndDisabledKeys() throws Exception {
        saveApiKey("pdapi_revoked", ApiKeyStatus.REVOKED, OrganizationStatus.ACTIVE);
        saveApiKey("pdapi_disabled", ApiKeyStatus.ACTIVE, OrganizationStatus.SUSPENDED);

        mockMvc.perform(get("/api/v1/products/1234567890123/price"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products/1234567890123/price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer clear"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products/1234567890123/price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer pdapi_unknown"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products/1234567890123/price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer pdapi_revoked"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/products/1234567890123/price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer pdapi_disabled"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void productEndpointAllowsValidApiKeyThroughSecurityLayer() throws Exception {
        saveApiKey("pdapi_active", ApiKeyStatus.ACTIVE, OrganizationStatus.ACTIVE);
        when(b2bProductService.getProductPrice(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new org.open4goods.b2bapi.dto.product.B2bResponse<>(null, null));

        mockMvc.perform(get("/api/v1/products/1234567890123/price")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer pdapi_active"))
                .andExpect(status().isOk());
    }

    @Test
    void dashboardEndpointRejectsMissingJwtBeforeController() throws Exception {
        mockMvc.perform(get("/api/v1/customer/api-keys"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void actuatorHealthAndInfoArePublicForSpringBootAdmin() throws Exception {
        final int healthStatus = mockMvc.perform(get("/actuator/health"))
                .andReturn()
                .getResponse()
                .getStatus();
        final int infoStatus = mockMvc.perform(get("/actuator/info"))
                .andReturn()
                .getResponse()
                .getStatus();

        assertThat(healthStatus).isNotIn(401, 403);
        assertThat(infoStatus).isNotIn(401, 403);
    }

    private void saveApiKey(
            final String clearKey,
            final ApiKeyStatus apiKeyStatus,
            final OrganizationStatus organizationStatus) {
        final Organization organization = new Organization("Test workspace " + clearKey, "test-" + clearKey);
        organization.setStatus(organizationStatus);
        final Organization savedOrganization = organizationRepository.save(organization);
        final User user = userRepository.save(new User(clearKey + "@example.com", OidcProvider.GITHUB, clearKey));
        final ApiKey apiKey = new ApiKey(
                savedOrganization,
                user,
                "Test key",
                clearKey.substring(0, Math.min(12, clearKey.length())),
                secretGenerator.sha256Hex(clearKey));
        apiKey.setStatus(apiKeyStatus);
        apiKeyRepository.save(apiKey);
    }
}
