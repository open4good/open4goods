package org.open4goods.datareference.model;

/**
 * Lifecycle state of a provider record as O4G currently understands it.
 */
public enum SourceRecordState {
    /** The source serves this record and O4G may use it. */
    ACTIVE,
    /** The source withdrew the record. */
    DELETED,
    /** The source could not be reached or returned no usable payload. */
    UNAVAILABLE,
    /** O4G refused the record, for instance on policy or validation grounds. */
    REJECTED
}
