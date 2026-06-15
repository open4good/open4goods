package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.exception.InsufficientCreditsException;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Settles durable credit ledger debits against expiring-first buckets.
 */
@Service
public class CreditLedgerService {

    private final CreditBucketRepository creditBucketRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final OrganizationRepository organizationRepository;
    private final Clock clock;

    @Autowired
    public CreditLedgerService(
            final ObjectProvider<CreditBucketRepository> creditBucketRepository,
            final ObjectProvider<CreditTransactionRepository> creditTransactionRepository,
            final ObjectProvider<OrganizationRepository> organizationRepository) {
        this(
                creditBucketRepository.getIfAvailable(),
                creditTransactionRepository.getIfAvailable(),
                organizationRepository.getIfAvailable(),
                Clock.systemUTC());
    }

    CreditLedgerService(
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final OrganizationRepository organizationRepository,
            final Clock clock) {
        this.creditBucketRepository = creditBucketRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.organizationRepository = organizationRepository;
        this.clock = clock;
    }

    /**
     * Debits the requested cost from live buckets, ordered by earliest expiry.
     *
     * @param organizationId organization id
     * @param requestId idempotency request id
     * @param facetId facet id being billed
     * @param gtin requested GTIN
     * @param cost credits to debit
     * @return durable settlement result
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditSettlementResult settleDebit(
            final UUID organizationId,
            final String requestId,
            final String facetId,
            final String gtin,
            final long cost) {
        ensurePersistence();
        if (cost <= 0) {
            return new CreditSettlementResult(creditBucketRepository.sumLiveCredits(organizationId), 0, false);
        }
        if (!StringUtils.hasText(requestId)) {
            throw new IllegalArgumentException("requestId is required for debit idempotency.");
        }
        if (creditTransactionRepository.existsDebitByRequestId(requestId)) {
            return new CreditSettlementResult(creditBucketRepository.sumLiveCredits(organizationId), 0, true);
        }

        final Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
        final List<CreditBucket> buckets = creditBucketRepository.findLiveBucketsForDebit(organizationId);
        if (creditTransactionRepository.existsDebitByRequestId(requestId)) {
            return new CreditSettlementResult(creditBucketRepository.sumLiveCredits(organizationId), 0, true);
        }

        long remaining = cost;
        for (final CreditBucket bucket : buckets) {
            if (remaining == 0) {
                break;
            }
            final long debit = Math.min(bucket.getCreditsRemaining(), remaining);
            bucket.setCreditsRemaining(bucket.getCreditsRemaining() - debit);
            remaining -= debit;

            final CreditTransaction transaction = new CreditTransaction(
                    organization,
                    CreditTransactionType.DEBIT,
                    -debit);
            transaction.setBucket(bucket);
            transaction.setFacetId(facetId);
            transaction.setGtin(gtin);
            transaction.setRequestId(requestId);
            transaction.setCreatedAt(clock.instant());
            creditTransactionRepository.insert(transaction);
        }

        if (remaining > 0) {
            throw new InsufficientCreditsException("Insufficient durable credits.");
        }
        return new CreditSettlementResult(creditBucketRepository.sumLiveCredits(organizationId), cost, false);
    }

    private void ensurePersistence() {
        if (creditBucketRepository == null || creditTransactionRepository == null || organizationRepository == null) {
            throw new IllegalStateException("Credit ledger persistence is unavailable.");
        }
    }
}
