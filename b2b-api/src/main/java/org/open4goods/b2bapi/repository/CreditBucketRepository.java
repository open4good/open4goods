package org.open4goods.b2bapi.repository;

import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.model.CreditBucket;
import org.open4goods.b2bapi.model.CreditBucketKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for authoritative credit buckets.
 */
@Repository
public interface CreditBucketRepository extends JpaRepository<CreditBucket, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bucket
            from CreditBucket bucket
            where bucket.organization.id = :organizationId
              and bucket.creditsRemaining > 0
              and (bucket.expiresAt is null or bucket.expiresAt > current_timestamp)
            order by bucket.expiresAt asc nulls last, bucket.createdAt asc
            """)
    List<CreditBucket> findLiveBucketsForDebit(@Param("organizationId") UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bucket
            from CreditBucket bucket
            where bucket.organization.id = :organizationId
              and bucket.kind = :kind
              and bucket.catalogId = :catalogId
              and bucket.creditsRemaining > 0
              and (bucket.expiresAt is null or bucket.expiresAt > current_timestamp)
            order by bucket.createdAt asc
            """)
    List<CreditBucket> findLiveSubscriptionBucketsForRollover(
            @Param("organizationId") UUID organizationId,
            @Param("kind") CreditBucketKind kind,
            @Param("catalogId") String catalogId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bucket
            from CreditBucket bucket
            where bucket.organization.id = :organizationId
              and bucket.kind = :kind
              and bucket.sourceRef = :sourceRef
              and bucket.creditsRemaining > 0
              and (bucket.expiresAt is null or bucket.expiresAt > current_timestamp)
            order by bucket.createdAt asc
            """)
    List<CreditBucket> findLiveSubscriptionBucketsForSourceRef(
            @Param("organizationId") UUID organizationId,
            @Param("kind") CreditBucketKind kind,
            @Param("sourceRef") String sourceRef);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select bucket
            from CreditBucket bucket
            where bucket.creditsRemaining > 0
              and bucket.expiresAt is not null
              and bucket.expiresAt <= :now
            order by bucket.expiresAt asc, bucket.createdAt asc
            """)
    List<CreditBucket> findExpiredBucketsForSweep(@Param("now") Instant now);

    @Query("""
            select distinct bucket.organization.id
            from CreditBucket bucket
            where bucket.creditsRemaining >= 0
            """)
    List<UUID> findOrganizationIdsWithBuckets();

    @Query("""
            select coalesce(sum(bucket.creditsRemaining), 0)
            from CreditBucket bucket
            where bucket.organization.id = :organizationId
              and bucket.creditsRemaining > 0
              and (bucket.expiresAt is null or bucket.expiresAt > current_timestamp)
            """)
    long sumLiveCredits(@Param("organizationId") UUID organizationId);
}
