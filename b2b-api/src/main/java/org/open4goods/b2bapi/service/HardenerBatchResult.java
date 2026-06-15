package org.open4goods.b2bapi.service;

/**
 * Counts produced by one hardener batch run.
 *
 * @param usageEventsDrained Redis usage events persisted to Postgres
 * @param bucketsExpired expired credit buckets zeroed
 * @param balancesReconciled organization balances reconciled into Redis
 * @param lastUsedFlushed debounced API key last-used values flushed
 */
public record HardenerBatchResult(
        int usageEventsDrained,
        int bucketsExpired,
        int balancesReconciled,
        int lastUsedFlushed) {
}
