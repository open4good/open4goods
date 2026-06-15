package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.UsageEvent;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.ApiKeyRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UsageEventRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Integration tests for the Redis/Postgres hardener batch.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "b2b.hardener.cron=-"
})
class HardenerBatchTest {

    private static final String POSTGRES_IMAGE = "postgres:16-alpine";
    private static final DockerImageName REDIS_IMAGE = DockerImageName.parse("redis:7-alpine");
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGRES_IMAGE);
    private static final GenericContainer<?> REDIS = new GenericContainer<>(REDIS_IMAGE).withExposedPorts(6379);

    static {
        POSTGRES.start();
        REDIS.start();
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
    }

    @DynamicPropertySource
    static void properties(final DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @Autowired
    private HardenerBatch hardenerBatch;

    @Autowired
    private UsageStreamService usageStreamService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ApiKeyRepository apiKeyRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private UsageEventRepository usageEventRepository;

    @BeforeEach
    void flushRedis() {
        redisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @Test
    void runBatchDrainsUsageExpiresBucketsReconcilesBalanceAndFlushesLastUsed() {
        final Organization organization = organizationRepository.save(uniqueOrganization());
        final User user = userRepository.save(uniqueUser());
        final ApiKey apiKey = apiKeyRepository.save(new ApiKey(
                organization,
                user,
                "Test key",
                "pdapi_test",
                "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"));

        final CreditBucket expired = new CreditBucket(organization, CreditBucketKind.PACK, 7, 7);
        expired.setExpiresAt(Instant.now().minusSeconds(60));
        creditBucketRepository.save(expired);
        creditBucketRepository.save(new CreditBucket(organization, CreditBucketKind.PACK, 5, 5));

        usageStreamService.emit(new UsageStreamEvent(
                organization.getId(),
                apiKey.getId(),
                "product.price",
                "1234567890123",
                "pdreq_hardener",
                200,
                true,
                5,
                null,
                42,
                Instant.parse("2026-06-15T12:00:00Z")));
        final Instant lastUsedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
        redisTemplate.opsForValue().set("b2b:lastused:" + apiKey.getId(), lastUsedAt.toString());

        final HardenerBatchResult result = hardenerBatch.runBatch();

        assertThat(result.usageEventsDrained()).isEqualTo(1);
        assertThat(result.bucketsExpired()).isEqualTo(1);
        assertThat(result.balancesReconciled()).isEqualTo(1);
        assertThat(result.lastUsedFlushed()).isEqualTo(1);

        final List<UsageEvent> usageEvents = usageEventRepository.findAll();
        assertThat(usageEvents).singleElement().satisfies(event -> {
            assertThat(event.getOrganization().getId()).isEqualTo(organization.getId());
            assertThat(event.getApiKey().getId()).isEqualTo(apiKey.getId());
            assertThat(event.getRequestId()).isEqualTo("pdreq_hardener");
            assertThat(event.getCreditsConsumed()).isEqualTo(5);
        });

        assertThat(creditBucketRepository.findById(expired.getId()).orElseThrow().getCreditsRemaining()).isZero();
        assertThat(creditBucketRepository.sumLiveCredits(organization.getId())).isEqualTo(5);
        assertThat(redisTemplate.opsForValue().get("b2b:org:" + organization.getId() + ":balance")).isEqualTo("5");
        assertThat(redisTemplate.hasKey("b2b:lastused:" + apiKey.getId())).isFalse();
        assertThat(apiKeyRepository.findById(apiKey.getId()).orElseThrow().getLastUsedAt()).isEqualTo(lastUsedAt);

        final List<CreditTransaction> transactions = creditTransactionRepository.findByOrganizationId(
                organization.getId(),
                10);
        assertThat(transactions)
                .filteredOn(transaction -> transaction.getType() == CreditTransactionType.EXPIRE)
                .singleElement()
                .satisfies(transaction -> assertThat(transaction.getCredits()).isEqualTo(-7));
    }

    private Organization uniqueOrganization() {
        final String suffix = UUID.randomUUID().toString();
        return new Organization("Hardener " + suffix, "hardener-" + suffix);
    }

    private User uniqueUser() {
        final String suffix = UUID.randomUUID().toString();
        return new User("hardener-" + suffix + "@example.com", OidcProvider.GOOGLE, "hardener-" + suffix);
    }
}
