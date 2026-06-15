package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.model.AdminAuditEvent;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.OidcProvider;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.AdminAuditEventRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration tests for durable credit grants and grant-related ledger updates.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
class CreditGrantServiceTest {

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
    private CreditGrantService creditGrantService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Autowired
    private AdminAuditEventRepository adminAuditEventRepository;

    @Test
    void freeGrantIsAppliedOnceAndWritesGrantLedgerRow() {
        final Organization organization = organizationRepository.save(uniqueOrganization("Free Grant"));
        final User actor = userRepository.save(uniqueUser("free"));

        final CreditGrantResult first = creditGrantService.grantFreeIfNeeded(organization.getId(), actor);
        final CreditGrantResult second = creditGrantService.grantFreeIfNeeded(organization.getId(), actor);

        assertThat(first.creditsGranted()).isEqualTo(2_500);
        assertThat(first.durableBalance()).isEqualTo(2_500);
        assertThat(second.idempotentReplay()).isTrue();
        assertThat(second.creditsGranted()).isZero();
        assertThat(organizationRepository.findById(organization.getId()).orElseThrow().isFreeGrantApplied()).isTrue();
        assertThat(creditBucketRepository.findAll())
                .filteredOn(bucket -> bucket.getOrganization().getId().equals(organization.getId()))
                .singleElement()
                .satisfies(bucket -> {
                    assertThat(bucket.getKind()).isEqualTo(CreditBucketKind.FREE_GRANT);
                    assertThat(bucket.getSourceRef()).isEqualTo("first-login-free-grant");
                });
        assertThat(creditTransactionRepository.findByOrganizationId(organization.getId(), 10))
                .singleElement()
                .satisfies(transaction -> {
                    assertThat(transaction.getType()).isEqualTo(CreditTransactionType.GRANT);
                    assertThat(transaction.getCredits()).isEqualTo(2_500);
                    assertThat(transaction.getActorUser().getId()).isEqualTo(actor.getId());
                });
    }

    @Test
    void packGrantUsesCatalogCreditsAndDoesNotExpire() {
        final Organization organization = organizationRepository.save(uniqueOrganization("Pack Grant"));

        final CreditGrantResult result = creditGrantService.grantPack(
                organization.getId(),
                "starter",
                "cs_test_pack");

        assertThat(result.creditsGranted()).isEqualTo(10_000);
        final CreditBucket bucket = creditBucketRepository.findById(result.bucketId()).orElseThrow();
        assertThat(bucket.getKind()).isEqualTo(CreditBucketKind.PACK);
        assertThat(bucket.getCatalogId()).isEqualTo("starter");
        assertThat(bucket.getSourceRef()).isEqualTo("cs_test_pack");
        assertThat(bucket.getExpiresAt()).isNull();
        assertThat(bucket.getCreditsRemaining()).isEqualTo(10_000);
    }

    @Test
    void subscriptionGrantExpiresOldestCreditsAboveRolloverCap() {
        final Organization organization = organizationRepository.save(uniqueOrganization("Subscription Cap"));
        final Instant expiresAt = Instant.now().plusSeconds(86_400);

        creditGrantService.grantSubscription(organization.getId(), "starter", "sub_1", expiresAt);
        creditGrantService.grantSubscription(organization.getId(), "starter", "sub_2", expiresAt);
        creditGrantService.grantSubscription(organization.getId(), "starter", "sub_3", expiresAt);
        final CreditGrantResult fourth = creditGrantService.grantSubscription(
                organization.getId(),
                "starter",
                "sub_4",
                expiresAt);

        assertThat(fourth.creditsGranted()).isEqualTo(12_000);
        assertThat(fourth.creditsExpired()).isEqualTo(12_000);
        assertThat(fourth.durableBalance()).isEqualTo(36_000);
        assertThat(creditBucketRepository.sumLiveCredits(organization.getId())).isEqualTo(36_000);

        final List<CreditTransaction> transactions = creditTransactionRepository.findByOrganizationId(
                organization.getId(),
                10);
        assertThat(transactions).filteredOn(transaction -> transaction.getType() == CreditTransactionType.GRANT)
                .hasSize(4);
        assertThat(transactions).filteredOn(transaction -> transaction.getType() == CreditTransactionType.EXPIRE)
                .singleElement()
                .satisfies(transaction -> assertThat(transaction.getCredits()).isEqualTo(-12_000));
    }

    @Test
    void cancellationExpiryUpdatesOnlyTargetSubscriptionBuckets() {
        final Organization organization = organizationRepository.save(uniqueOrganization("Cancel Expiry"));
        final Instant originalExpiry = Instant.now().plusSeconds(30 * 86_400).truncatedTo(ChronoUnit.MICROS);
        final Instant cancelTime = Instant.parse("2026-06-15T12:00:00Z");

        final UUID targetBucketId = creditGrantService.grantSubscription(
                organization.getId(),
                "starter",
                "sub_cancel",
                originalExpiry).bucketId();
        final UUID otherBucketId = creditGrantService.grantSubscription(
                organization.getId(),
                "starter",
                "sub_other",
                originalExpiry).bucketId();

        final int updated = creditGrantService.applySubscriptionCancellationExpiry(
                organization.getId(),
                "sub_cancel",
                cancelTime);

        assertThat(updated).isEqualTo(1);
        assertThat(creditBucketRepository.findById(targetBucketId).orElseThrow().getExpiresAt())
                .isEqualTo(Instant.parse("2026-07-15T12:00:00Z"));
        assertThat(creditBucketRepository.findById(otherBucketId).orElseThrow().getExpiresAt())
                .isEqualTo(originalExpiry);
    }

    @Test
    void manualGrantWritesAuditEvent() {
        final Organization organization = organizationRepository.save(uniqueOrganization("Manual Grant"));
        final User actor = userRepository.save(uniqueUser("manual"));
        final Instant expiresAt = Instant.now().plusSeconds(7 * 86_400).truncatedTo(ChronoUnit.MICROS);

        final CreditGrantResult result = creditGrantService.grantManual(
                organization.getId(),
                actor.getId(),
                123,
                expiresAt,
                "support adjustment");

        assertThat(result.creditsGranted()).isEqualTo(123);
        final CreditBucket bucket = creditBucketRepository.findById(result.bucketId()).orElseThrow();
        assertThat(bucket.getKind()).isEqualTo(CreditBucketKind.MANUAL);
        assertThat(bucket.getExpiresAt()).isEqualTo(expiresAt);

        assertThat(adminAuditEventRepository.findByTargetOrganizationId(organization.getId(), 10))
                .singleElement()
                .satisfies(event -> assertManualGrantAudit(event, actor, result.bucketId()));
    }

    private void assertManualGrantAudit(
            final AdminAuditEvent event,
            final User actor,
            final UUID bucketId) {
        assertThat(event.getActorUser().getId()).isEqualTo(actor.getId());
        assertThat(event.getAction()).isEqualTo("CREDIT_MANUAL_GRANT");
        assertThat(event.getTargetRef()).isEqualTo(bucketId.toString());
        assertThat(event.getDetail()).containsEntry("credits", 123);
        assertThat(event.getDetail()).containsEntry("note", "support adjustment");
    }

    private Organization uniqueOrganization(final String name) {
        final String suffix = UUID.randomUUID().toString();
        return new Organization(name + " " + suffix, "org-" + suffix);
    }

    private User uniqueUser(final String prefix) {
        final String suffix = UUID.randomUUID().toString();
        return new User(prefix + "-" + suffix + "@example.com", OidcProvider.GOOGLE, "sub-" + suffix);
    }
}
