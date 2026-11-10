package org.open4goods.nudgerfrontapi.service.css;

import java.util.Map;

/**
 * Result of one synchronisation run (GOU-2081).
 *
 * @param examined   products evaluated
 * @param inserted   inserts sent for a new or modified product
 * @param renewed    inserts sent again before the expiration of an unchanged product
 * @param deleted    deletes sent
 * @param unchanged  eligible products left alone (unchanged and far from expiration)
 * @param expired    known inputs whose expiration had passed at the start of the run
 * @param rejected   failed calls by reason
 * @param capped     true when the run stopped on {@code max-products-per-run}
 * @param aborted    true when the run stopped on too many consecutive failures or a read error
 */
public record CssSyncReport(long examined, long inserted, long renewed, long deleted, long unchanged, long expired,
        Map<String, Long> rejected, boolean capped, boolean aborted) {

    /** @return the total number of failed calls */
    public long rejectedCount() {
        return rejected.values().stream().mapToLong(Long::longValue).sum();
    }
}
