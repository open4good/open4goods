package org.open4goods.b2bapi.repository;

import java.util.UUID;
import org.open4goods.b2bapi.model.UsageEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for persisted usage analytics events.
 */
@Repository
public interface UsageEventRepository extends JpaRepository<UsageEvent, UUID> {
}
