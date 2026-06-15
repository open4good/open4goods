package org.open4goods.b2bapi.repository;

import java.util.Optional;
import java.util.UUID;
import org.open4goods.b2bapi.model.OrganizationMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for organization memberships.
 */
@Repository
public interface OrganizationMemberRepository extends JpaRepository<OrganizationMember, UUID> {

    Optional<OrganizationMember> findByOrganizationIdAndUserId(UUID organizationId, UUID userId);

    Optional<OrganizationMember> findFirstByUserIdOrderByCreatedAtAsc(UUID userId);
}
