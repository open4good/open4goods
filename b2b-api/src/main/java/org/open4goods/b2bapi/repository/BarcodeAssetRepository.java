package org.open4goods.b2bapi.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.BarcodeAsset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for cached barcode assets.
 */
@Repository
public interface BarcodeAssetRepository extends JpaRepository<BarcodeAsset, UUID> {

    Optional<BarcodeAsset> findByToken(String token);

    @Modifying
    @Query("DELETE FROM BarcodeAsset b WHERE b.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
