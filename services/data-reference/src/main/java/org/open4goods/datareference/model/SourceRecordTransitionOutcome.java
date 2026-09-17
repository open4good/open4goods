package org.open4goods.datareference.model;

/** Outcome of one source-record mutation attempt. */
public enum SourceRecordTransitionOutcome {
    /** A new head was accepted and must be delivered to the journal. */
    ACCEPTED,
    /** The same content already occupies the head. */
    DUPLICATE,
    /** The candidate describes an older provider observation. */
    OUT_OF_ORDER,
    /** Equal ordering timestamps carried distinct content and need operator review. */
    COLLISION,
    /** A deletion replaced the previous head. */
    TOMBSTONED,
    /** An unavailable/rejected attempt was recorded without replacing a valid head. */
    REJECTED
}
