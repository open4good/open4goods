package org.open4goods.api.services.migration.legacybackup;

/** Terminal status of one {@code APPLY} invocation. */
public enum LegacyBackupApplyStatus {
    /** Every manifest-listed file was fully reconciled. */
    COMPLETED,
    /** A pending cancellation request stopped the run cleanly after a batch boundary. */
    CANCELLED,
    /** An unreconciled parse, bulk-apply or digest error stopped the run. */
    FAILED
}
