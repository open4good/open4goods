package org.open4goods.api.services.migration.legacybackup;

/**
 * Reviewed conversion disposition for one observed legacy field name.
 *
 * <p>Mirrors {@code field_disposition()} in {@code scripts/migration/pin_product_backup.py}
 * exactly, so the pinned manifest's {@code conversionRules.fieldDispositions} and this
 * importer's own classification never disagree on the same field name.
 */
public enum LegacyFieldDisposition {
    /** Preserved only as a legacy correlation key, never as product identity beyond the GTIN itself. */
    UNATTRIBUTED_IDENTITY,
    /** No approved conversion; the field's value is never converted. */
    QUARANTINE,
    /** Retained only as an unattributed historical minimum price. */
    LEGACY_MINIMUM_PRICE,
    /** Derived from eligible imported evidence; never carried over from the legacy value. */
    RECOMPUTED,
    /** Resolved through the reviewed registry attribute disposition manifest. */
    NATIVE_EVIDENCED_CONVERSION
}
