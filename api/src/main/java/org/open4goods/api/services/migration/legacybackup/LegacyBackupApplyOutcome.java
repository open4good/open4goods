package org.open4goods.api.services.migration.legacybackup;

/** Result of attempting to apply one converted legacy backup line to the target stores. */
public enum LegacyBackupApplyOutcome {
    /** No prior head existed for this GTIN; the record was written. */
    APPLIED,
    /** A prior head with the identical payload already existed; nothing was rewritten. */
    DUPLICATE_IDENTICAL_SKIPPED,
    /** A prior head with a different payload exists for the same GTIN; neither is written. */
    CONFLICT_QUARANTINED,
    /** The line converted to no mutation (malformed, missing GTIN, ...); only dead letters were recorded. */
    UNCONVERTIBLE
}
