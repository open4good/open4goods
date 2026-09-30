package org.open4goods.datareference.port;

/**
 * Ordering guaranteed by a batch scan.
 *
 * <p>Stated rather than assumed: a replay that must not miss a record needs an
 * order that a concurrent write cannot disturb, and that is not the same order
 * an operator wants when reading a report.
 */
public enum ScanOrder {
    /**
     * By record key. Stable under concurrent writes, so a resumed scan visits
     * every record that existed at the start exactly once.
     */
    RECORD_KEY,
    /**
     * By observation instant, oldest first. Not stable under concurrent writes:
     * a record re-observed mid-scan may be visited twice or not at all.
     */
    OBSERVED_AT
}
