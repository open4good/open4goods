package org.open4goods.api.services.migration.legacybackup;

/** Stable, sanitized reason codes for one legacy backup line that was not written as-is. */
public enum LegacyBackupDeadLetterReason {
    /** The line was not valid JSON. */
    MALFORMED_JSON_LINE,
    /** The record has no field carrying a syntactically and check-digit valid GTIN. */
    MISSING_OR_INVALID_GTIN,
    /** The record named an Amazon-labelled field; that field's value was never converted. */
    FORBIDDEN_SOURCE_CONTENT,
    /** Same GTIN observed twice in this dataset with a different payload; neither was written. */
    CONFLICTING_DUPLICATE_GTIN,
    /** A native-evidenced attribute field exists but its full registry mapping is deferred. */
    ATTRIBUTE_MAPPING_DEFERRED,
    /** The target store rejected or failed to apply an otherwise valid mutation. */
    BULK_APPLY_FAILURE
}
