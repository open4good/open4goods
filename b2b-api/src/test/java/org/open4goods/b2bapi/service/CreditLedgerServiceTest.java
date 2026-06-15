package org.open4goods.b2bapi.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.open4goods.b2bapi.B2bApiApplication;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Integration tests for durable expiring-first credit settlement.
 */
@SpringBootTest(classes = B2bApiApplication.class, properties = {
        "spring.jpa.hibernate.ddl-auto=validate",
        "management.health.redis.enabled=false"
})
class CreditLedgerServiceTest {

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
    private CreditLedgerService creditLedgerService;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private CreditBucketRepository creditBucketRepository;

    @Autowired
    private CreditTransactionRepository creditTransactionRepository;

    @Test
    void settlesDebitAcrossExpiringFirstBuckets() {
        final Organization organization = organizationRepository.save(new Organization("Debit Org", "debit-org"));
        final CreditBucket first = saveBucket(organization, 3, Instant.now().plusSeconds(3600), 1);
        final CreditBucket second = saveBucket(organization, 4, Instant.now().plusSeconds(7200), 2);
        final CreditBucket last = saveBucket(organization, 10, null, 3);

        final CreditSettlementResult result = creditLedgerService.settleDebit(
                organization.getId(),
                "pdreq_debit",
                "product.price",
                "1234567890123",
                6);

        assertThat(result.creditsDebited()).isEqualTo(6);
        assertThat(result.durableBalance()).isEqualTo(11);
        assertThat(result.idempotentReplay()).isFalse();
        assertThat(creditBucketRepository.findById(first.getId()).orElseThrow().getCreditsRemaining()).isZero();
        assertThat(creditBucketRepository.findById(second.getId()).orElseThrow().getCreditsRemaining()).isEqualTo(1);
        assertThat(creditBucketRepository.findById(last.getId()).orElseThrow().getCreditsRemaining()).isEqualTo(10);

        final List<CreditTransaction> transactions = creditTransactionRepository.findByOrganizationId(
                organization.getId(), 10);
        assertThat(transactions).hasSize(2);
        assertThat(transactions).extracting(CreditTransaction::getCredits).containsExactlyInAnyOrder(-3L, -3L);
        assertThat(transactions).extracting(CreditTransaction::getRequestId).containsOnly("pdreq_debit");
    }

    @Test
    void duplicateRequestIdDoesNotDebitTwice() {
        final Organization organization = organizationRepository.save(new Organization("Replay Org", "replay-org"));
        saveBucket(organization, 10, null, 1);

        final CreditSettlementResult first = creditLedgerService.settleDebit(
                organization.getId(),
                "pdreq_replay",
                "product.price",
                "1234567890123",
                4);
        final CreditSettlementResult second = creditLedgerService.settleDebit(
                organization.getId(),
                "pdreq_replay",
                "product.price",
                "1234567890123",
                4);

        assertThat(first.creditsDebited()).isEqualTo(4);
        assertThat(second.creditsDebited()).isZero();
        assertThat(second.idempotentReplay()).isTrue();
        assertThat(creditBucketRepository.sumLiveCredits(organization.getId())).isEqualTo(6);
        assertThat(creditTransactionRepository.findByOrganizationId(organization.getId(), 10)).hasSize(1);
    }

    @Test
    void insufficientDurableCreditsRollbackBucketChangesAndTransactions() {
        final Organization organization = organizationRepository.save(new Organization("Poor Org", "poor-org"));
        final CreditBucket bucket = saveBucket(organization, 2, null, 1);

        assertThatThrownBy(() -> creditLedgerService.settleDebit(
                organization.getId(),
                "pdreq_insufficient",
                "product.price",
                "1234567890123",
                3))
                .isInstanceOf(InsufficientCreditsException.class);

        assertThat(creditBucketRepository.findById(bucket.getId()).orElseThrow().getCreditsRemaining()).isEqualTo(2);
        assertThat(creditTransactionRepository.findByOrganizationId(organization.getId(), 10)).isEmpty();
    }

    private CreditBucket saveBucket(
            final Organization organization,
            final long credits,
            final Instant expiresAt,
            final int createdOrder) {
        final CreditBucket bucket = new CreditBucket(organization, CreditBucketKind.PACK, credits, credits);
        bucket.setExpiresAt(expiresAt);
        bucket.setCreatedAt(Instant.now().plusSeconds(createdOrder));
        return creditBucketRepository.save(bucket);
    }
}
