package org.open4goods.b2bapi.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.open4goods.b2bapi.dto.billing.B2bBalanceResponseDto;
import org.open4goods.b2bapi.dto.billing.B2bInvoiceDto;
import org.open4goods.b2bapi.dto.billing.B2bSubscriptionDto;
import org.open4goods.b2bapi.dto.billing.B2bTransactionDto;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.Invoice;
import org.open4goods.b2bapi.model.StripeSubscription;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.InvoiceRepository;
import org.open4goods.b2bapi.repository.StripeSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service to handle customer billing & subscription queries.
 */
@Service
@Transactional(readOnly = true)
public class CustomerBillingService {

    private final CreditBucketRepository creditBucketRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final StripeSubscriptionRepository stripeSubscriptionRepository;

    public CustomerBillingService(
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final InvoiceRepository invoiceRepository,
            final StripeSubscriptionRepository stripeSubscriptionRepository) {
        this.creditBucketRepository = creditBucketRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.invoiceRepository = invoiceRepository;
        this.stripeSubscriptionRepository = stripeSubscriptionRepository;
    }

    /**
     * Get credit balance and active bucket breakdown for an organization.
     */
    public B2bBalanceResponseDto getBalance(final UUID organizationId) {
        final long totalRemaining = creditBucketRepository.sumLiveCredits(organizationId);
        final List<CreditBucket> liveBuckets = creditBucketRepository.findLiveBuckets(organizationId);

        final List<B2bBalanceResponseDto.B2bBucketDetailDto> bucketDetails = liveBuckets.stream()
                .map(b -> new B2bBalanceResponseDto.B2bBucketDetailDto(
                        b.getId() != null ? b.getId().toString() : null,
                        b.getKind() != null ? b.getKind().name() : null,
                        b.getCreditsTotal(),
                        b.getCreditsRemaining(),
                        b.getExpiresAt(),
                        b.getCatalogId()
                ))
                .collect(Collectors.toList());

        return new B2bBalanceResponseDto(totalRemaining, bucketDetails);
    }

    /**
     * Get transactions ledger for an organization.
     */
    public List<B2bTransactionDto> getTransactions(final UUID organizationId, final int limit) {
        final List<CreditTransaction> txs = creditTransactionRepository.findByOrganizationId(organizationId, limit);
        return txs.stream()
                .map(t -> new B2bTransactionDto(
                        t.getId(),
                        t.getType() != null ? t.getType().name() : null,
                        t.getCredits(),
                        t.getFacetId(),
                        t.getGtin(),
                        t.getRequestId(),
                        t.getNote(),
                        t.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get invoices for an organization.
     */
    public List<B2bInvoiceDto> getInvoices(final UUID organizationId) {
        final List<Invoice> invoices = invoiceRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        return invoices.stream()
                .map(i -> new B2bInvoiceDto(
                        i.getId(),
                        i.getStripeInvoiceId(),
                        i.getAmountCents(),
                        i.getCurrency(),
                        i.getStatus(),
                        i.getHostedInvoiceUrl(),
                        i.getCreditsGranted(),
                        i.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /**
     * Get subscriptions for an organization.
     */
    public List<B2bSubscriptionDto> getSubscriptions(final UUID organizationId) {
        final List<StripeSubscription> subs = stripeSubscriptionRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
        return subs.stream()
                .map(s -> new B2bSubscriptionDto(
                        s.getId(),
                        s.getStripeSubscriptionId(),
                        s.getCatalogId(),
                        s.getStatus(),
                        s.getCurrentPeriodEnd(),
                        s.getCancelAt(),
                        s.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }
}
