package org.open4goods.b2bapi.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.ApiKey;
import org.open4goods.b2bapi.model.ApiKeyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for hashed API key metadata.
 */
@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByOrganizationIdAndStatus(UUID organizationId, ApiKeyStatus status);

    List<ApiKey> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<ApiKey> findByOrganizationIdAndCreatedByIdOrderByCreatedAtDesc(UUID organizationId, UUID createdById);

    Optional<ApiKey> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Modifying
    @Query("""
            update ApiKey apiKey
            set apiKey.lastUsedAt = :lastUsedAt
            where apiKey.id = :id
              and (apiKey.lastUsedAt is null or apiKey.lastUsedAt < :lastUsedAt)
            """)
    int updateLastUsedAtIfNewer(@Param("id") UUID id, @Param("lastUsedAt") Instant lastUsedAt);
}
