package org.open4goods.api.services.migration.legacybackup;

/**
 * Explicit operator-triggered operations of the legacy backup importer.
 *
 * <p>No operation ever runs on application boot; each is invoked explicitly through the
 * admin controller.
 */
public enum LegacyBackupOperation {
    /** Validates the pinned manifest and per-file digests/line counts; writes nothing. */
    INVENTORY,
    /** Like {@link #INVENTORY}, plus a dry-run classification of a bounded sample of lines. */
    SAMPLE,
    /** Resumable write of source, legacy baseline and price stores. */
    APPLY,
    /** Reports checkpoint progress for a dataset without touching its files. */
    STATUS,
    /** Requests a running {@link #APPLY} to stop cleanly after its current batch. */
    CANCEL
}
