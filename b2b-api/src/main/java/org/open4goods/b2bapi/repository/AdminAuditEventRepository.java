package org.open4goods.b2bapi.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.model.AdminAuditEvent;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insert-only repository for platform-admin audit events.
 */
@Repository
public class AdminAuditEventRepository {

    private final ObjectProvider<EntityManager> entityManager;

    public AdminAuditEventRepository(final ObjectProvider<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public AdminAuditEvent insert(final AdminAuditEvent event) {
        entityManager.getObject().persist(event);
        return event;
    }

    public List<AdminAuditEvent> findByTargetOrganizationId(final UUID organizationId, final int limit) {
        return entityManager.getObject().createQuery("""
                        select event
                        from AdminAuditEvent event
                        where event.targetOrganization.id = :organizationId
                        order by event.createdAt desc
                        """, AdminAuditEvent.class)
                .setParameter("organizationId", organizationId)
                .setMaxResults(limit)
                .getResultList();
     }

     public List<AdminAuditEvent> findAllRecent(final int limit) {
         return entityManager.getObject().createQuery("""
                         select event
                         from AdminAuditEvent event
                         order by event.createdAt desc
                         """, AdminAuditEvent.class)
                 .setMaxResults(limit)
                 .getResultList();
     }
}
