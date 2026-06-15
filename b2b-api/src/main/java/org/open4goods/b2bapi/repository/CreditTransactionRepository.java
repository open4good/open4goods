package org.open4goods.b2bapi.repository;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.open4goods.b2bapi.model.CreditTransaction;
import org.open4goods.b2bapi.model.CreditTransactionType;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Insert-only repository for the append-only credit ledger.
 */
@Repository
public class CreditTransactionRepository {

    private final ObjectProvider<EntityManager> entityManager;

    public CreditTransactionRepository(final ObjectProvider<EntityManager> entityManager) {
        this.entityManager = entityManager;
    }

    @Transactional
    public CreditTransaction insert(final CreditTransaction transaction) {
        entityManager.getObject().persist(transaction);
        return transaction;
    }

    public boolean existsDebitByRequestId(final String requestId) {
        return entityManager.getObject().createQuery("""
                        select count(transaction.id)
                        from CreditTransaction transaction
                        where transaction.type = :type
                          and transaction.requestId = :requestId
                        """, Long.class)
                .setParameter("type", CreditTransactionType.DEBIT)
                .setParameter("requestId", requestId)
                .getSingleResult() > 0;
    }

    public List<CreditTransaction> findByOrganizationId(final UUID organizationId, final int limit) {
        return entityManager.getObject().createQuery("""
                        select transaction
                        from CreditTransaction transaction
                        where transaction.organization.id = :organizationId
                        order by transaction.createdAt desc
                        """, CreditTransaction.class)
                .setParameter("organizationId", organizationId)
                .setMaxResults(limit)
                .getResultList();
    }
}
