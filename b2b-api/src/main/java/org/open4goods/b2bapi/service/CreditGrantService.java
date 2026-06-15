package org.open4goods.b2bapi.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.open4goods.b2bapi.config.B2bApiProperties;
import org.open4goods.b2bapi.config.BillingCatalogProperties;
import org.open4goods.b2bapi.exception.RedisUnavailableException;
import org.open4goods.b2bapi.exception.ResourceNotFoundException;
import org.open4goods.b2bapi.model.AdminAuditEvent;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.open4goods.b2bapi.model.Organization;
import org.open4goods.b2bapi.model.User;
import org.open4goods.b2bapi.repository.AdminAuditEventRepository;
import org.open4goods.b2bapi.repository.CreditBucketRepository;
import org.open4goods.b2bapi.repository.CreditTransactionRepository;
import org.open4goods.b2bapi.repository.OrganizationRepository;
import org.open4goods.b2bapi.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Applies durable credit grants, rollover expiration, and cancellation expiry.
 */
@Service
public class CreditGrantService {

    private static final Duration CANCELLATION_GRACE = Duration.ofDays(30);
    private static final String FREE_GRANT_SOURCE_REF = "first-login-free-grant";
    private static final String MANUAL_GRANT_ACTION = "CREDIT_MANUAL_GRANT";

    private final B2bApiProperties properties;
    private final BillingCatalogProperties catalogProperties;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final CreditBucketRepository creditBucketRepository;
    private final CreditTransactionRepository creditTransactionRepository;
    private final AdminAuditEventRepository adminAuditEventRepository;
    private final RedisMeteringService redisMeteringService;
    private final Clock clock;

    @Autowired
    public CreditGrantService(
            final B2bApiProperties properties,
            final BillingCatalogProperties catalogProperties,
            final ObjectProvider<OrganizationRepository> organizationRepository,
            final ObjectProvider<UserRepository> userRepository,
            final ObjectProvider<CreditBucketRepository> creditBucketRepository,
            final ObjectProvider<CreditTransactionRepository> creditTransactionRepository,
            final ObjectProvider<AdminAuditEventRepository> adminAuditEventRepository,
            final ObjectProvider<RedisMeteringService> redisMeteringService) {
        this(
                properties,
                catalogProperties,
                organizationRepository.getIfAvailable(),
                userRepository.getIfAvailable(),
                creditBucketRepository.getIfAvailable(),
                creditTransactionRepository.getIfAvailable(),
                adminAuditEventRepository.getIfAvailable(),
                redisMeteringService.getIfAvailable(),
                Clock.systemUTC());
    }

    CreditGrantService(
            final B2bApiProperties properties,
            final BillingCatalogProperties catalogProperties,
            final OrganizationRepository organizationRepository,
            final UserRepository userRepository,
            final CreditBucketRepository creditBucketRepository,
            final CreditTransactionRepository creditTransactionRepository,
            final AdminAuditEventRepository adminAuditEventRepository,
            final RedisMeteringService redisMeteringService,
            final Clock clock) {
        this.properties = properties;
        this.catalogProperties = catalogProperties;
        this.organizationRepository = organizationRepository;
        this.userRepository = userRepository;
        this.creditBucketRepository = creditBucketRepository;
        this.creditTransactionRepository = creditTransactionRepository;
        this.adminAuditEventRepository = adminAuditEventRepository;
        this.redisMeteringService = redisMeteringService;
        this.clock = clock;
    }

    /**
     * Applies the one-time free grant if the organization has not received it.
     *
     * @param organizationId target organization id
     * @param actor user receiving the grant
     * @return grant result
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditGrantResult grantFreeIfNeeded(final UUID organizationId, final User actor) {
        ensurePersistence();
        final Organization organization = lockedOrganization(organizationId);
        if (organization.isFreeGrantApplied()) {
            return new CreditGrantResult(
                    null,
                    0,
                    0,
                    creditBucketRepository.sumLiveCredits(organizationId),
                    true);
        }

        organization.setFreeGrantApplied(true);
        organization.setUpdatedAt(clock.instant());
        final CreditGrantResult result = createGrantBucket(
                organization,
                CreditBucketKind.FREE_GRANT,
                properties.getCredits().getFreeGrantCredits(),
                null,
                null,
                FREE_GRANT_SOURCE_REF,
                actor,
                "One-time free grant");
        reconcileHotBalance(organizationId, result.durableBalance());
        return result;
    }

    /**
     * Grants credits from a prepaid pack purchase.
     *
     * @param organizationId target organization id
     * @param catalogId pack catalog id
     * @param sourceRef Stripe session or invoice id
     * @return grant result
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditGrantResult grantPack(final UUID organizationId, final String catalogId, final String sourceRef) {
        ensurePersistence();
        final BillingCatalogProperties.Pack pack = catalogProperties.getBilling().getPacks().get(catalogId);
        if (pack == null) {
            throw new IllegalArgumentException("Unknown pack catalog id: " + catalogId);
        }
        final Organization organization = lockedOrganization(organizationId);
        final CreditGrantResult result = createGrantBucket(
                organization,
                CreditBucketKind.PACK,
                pack.getCredits(),
                null,
                catalogId,
                sourceRef,
                null,
                "Pack purchase: " + catalogId);
        reconcileHotBalance(organizationId, result.durableBalance());
        return result;
    }

    /**
     * Grants subscription credits and expires oldest retained credits above the rollover cap.
     *
     * @param organizationId target organization id
     * @param catalogId subscription catalog id
     * @param stripeSubscriptionId Stripe subscription id
     * @param expiresAt bucket expiry already including any billing grace
     * @return grant result
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditGrantResult grantSubscription(
            final UUID organizationId,
            final String catalogId,
            final String stripeSubscriptionId,
            final Instant expiresAt) {
        ensurePersistence();
        if (!StringUtils.hasText(stripeSubscriptionId)) {
            throw new IllegalArgumentException("stripeSubscriptionId is required.");
        }
        final BillingCatalogProperties.Subscription subscription = catalogProperties.getBilling()
                .getSubscriptions()
                .get(catalogId);
        if (subscription == null) {
            throw new IllegalArgumentException("Unknown subscription catalog id: " + catalogId);
        }

        final Organization organization = lockedOrganization(organizationId);
        final CreditGrantResult grant = createGrantBucket(
                organization,
                CreditBucketKind.SUBSCRIPTION,
                subscription.getMonthlyCredits(),
                expiresAt,
                catalogId,
                stripeSubscriptionId,
                null,
                "Subscription grant: " + catalogId);
        final long expired = enforceRolloverCap(organization, catalogId, subscription);
        final long balance = creditBucketRepository.sumLiveCredits(organizationId);
        reconcileHotBalance(organizationId, balance);
        return new CreditGrantResult(grant.bucketId(), grant.creditsGranted(), expired, balance, false);
    }

    /**
     * Grants manual admin credits and writes an audit event.
     *
     * @param organizationId target organization id
     * @param actorUserId platform-admin user id
     * @param credits credits to grant
     * @param expiresAt optional expiry
     * @param note admin reason
     * @return grant result
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public CreditGrantResult grantManual(
            final UUID organizationId,
            final UUID actorUserId,
            final long credits,
            final Instant expiresAt,
            final String note) {
        ensurePersistence();
        if (userRepository == null || adminAuditEventRepository == null) {
            throw new IllegalStateException("Admin grant persistence is unavailable.");
        }
        final User actor = userRepository.findById(actorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Actor user not found."));
        final Organization organization = lockedOrganization(organizationId);
        final CreditGrantResult result = createGrantBucket(
                organization,
                CreditBucketKind.MANUAL,
                credits,
                expiresAt,
                null,
                "manual:" + UUID.randomUUID(),
                actor,
                note);

        final AdminAuditEvent event = new AdminAuditEvent(actor, MANUAL_GRANT_ACTION);
        event.setTargetOrganization(organization);
        event.setTargetRef(result.bucketId().toString());
        event.setDetail(manualGrantDetail(credits, expiresAt, note));
        event.setCreatedAt(clock.instant());
        adminAuditEventRepository.insert(event);
        reconcileHotBalance(organizationId, result.durableBalance());
        return result;
    }

    /**
     * Sets live subscription credits to expire 30 days after cancellation.
     *
     * @param organizationId target organization id
     * @param stripeSubscriptionId Stripe subscription id
     * @param cancelTime cancellation time
     * @return number of buckets updated
     */
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public int applySubscriptionCancellationExpiry(
            final UUID organizationId,
            final String stripeSubscriptionId,
            final Instant cancelTime) {
        ensurePersistence();
        if (!StringUtils.hasText(stripeSubscriptionId)) {
            throw new IllegalArgumentException("stripeSubscriptionId is required.");
        }
        lockedOrganization(organizationId);
        final Instant expiresAt = cancelTime.plus(CANCELLATION_GRACE);
        final List<CreditBucket> buckets = creditBucketRepository.findLiveSubscriptionBucketsForSourceRef(
                organizationId,
                CreditBucketKind.SUBSCRIPTION,
                stripeSubscriptionId);
        buckets.forEach(bucket -> bucket.setExpiresAt(expiresAt));
        reconcileHotBalance(organizationId, creditBucketRepository.sumLiveCredits(organizationId));
        return buckets.size();
    }

    private long enforceRolloverCap(
            final Organization organization,
            final String catalogId,
            final BillingCatalogProperties.Subscription subscription) {
        final long cap = (long) subscription.getMonthlyCredits() * subscription.getRolloverCapMonths();
        final List<CreditBucket> buckets = creditBucketRepository.findLiveSubscriptionBucketsForRollover(
                organization.getId(),
                CreditBucketKind.SUBSCRIPTION,
                catalogId);
        long liveCredits = buckets.stream().mapToLong(CreditBucket::getCreditsRemaining).sum();
        long creditsExpired = 0;
        for (final CreditBucket bucket : buckets) {
            if (liveCredits <= cap) {
                break;
            }
            final long expired = Math.min(bucket.getCreditsRemaining(), liveCredits - cap);
            bucket.setCreditsRemaining(bucket.getCreditsRemaining() - expired);
            liveCredits -= expired;
            creditsExpired += expired;

            final CreditTransaction transaction = new CreditTransaction(
                    organization,
                    CreditTransactionType.EXPIRE,
                    -expired);
            transaction.setBucket(bucket);
            transaction.setNote("Subscription rollover cap: " + catalogId);
            transaction.setCreatedAt(clock.instant());
            creditTransactionRepository.insert(transaction);
        }
        return creditsExpired;
    }

    private CreditGrantResult createGrantBucket(
            final Organization organization,
            final CreditBucketKind kind,
            final long credits,
            final Instant expiresAt,
            final String catalogId,
            final String sourceRef,
            final User actor,
            final String note) {
        if (credits <= 0) {
            throw new IllegalArgumentException("credits must be positive.");
        }

        final Instant now = clock.instant();
        final CreditBucket bucket = new CreditBucket(organization, kind, credits, credits);
        bucket.setExpiresAt(expiresAt);
        bucket.setCatalogId(catalogId);
        bucket.setSourceRef(sourceRef);
        bucket.setCreatedAt(now);
        final CreditBucket savedBucket = creditBucketRepository.save(bucket);

        final CreditTransaction transaction = new CreditTransaction(organization, CreditTransactionType.GRANT, credits);
        transaction.setBucket(savedBucket);
        transaction.setActorUser(actor);
        transaction.setNote(note);
        transaction.setCreatedAt(now);
        creditTransactionRepository.insert(transaction);

        return new CreditGrantResult(
                savedBucket.getId(),
                credits,
                0,
                creditBucketRepository.sumLiveCredits(organization.getId()),
                false);
    }

    private Organization lockedOrganization(final UUID organizationId) {
        return organizationRepository.findLockedById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found."));
    }

    private Map<String, Object> manualGrantDetail(final long credits, final Instant expiresAt, final String note) {
        final Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("credits", credits);
        detail.put("expiresAt", expiresAt == null ? null : expiresAt.toString());
        detail.put("note", note);
        return detail;
    }

    private void reconcileHotBalance(final UUID organizationId, final long durableBalance) {
        if (redisMeteringService == null) {
            return;
        }
        try {
            redisMeteringService.reconcileBalance(organizationId, durableBalance);
        } catch (final RedisUnavailableException ignored) {
            // Durable Postgres is authoritative; the hardener reconciles Redis when it is available.
        }
    }

    private void ensurePersistence() {
        if (organizationRepository == null || creditBucketRepository == null || creditTransactionRepository == null) {
            throw new IllegalStateException("Credit grant persistence is unavailable.");
        }
    }
}
