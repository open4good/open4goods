package org.open4goods.api.services.migration.legacybackup;

import java.util.Locale;
import java.util.Set;

/**
 * Classifies one observed legacy field name into its reviewed conversion disposition.
 *
 * <p>Deliberately kept byte-for-byte equivalent to {@code field_disposition()} in
 * {@code scripts/migration/pin_product_backup.py}: the pinned manifest already carries this
 * classification per observed field, and this importer recomputes it independently so a
 * manifest mismatch is detected rather than silently trusted.
 */
public final class LegacyBackupFieldDispositionRegistry {

    private static final Set<String> GTIN_FIELDS = Set.of("gtin", "ean", "ean13", "barcode", "id", "uuid", "productid");
    private static final Set<String> NATIVE_SCALAR_FIELDS = Set.of(
            "name", "title", "description", "brand", "model", "manufacturer", "vertical", "category");
    private static final Set<String> RECOMPUTED_TOKENS = Set.of("score", "rank", "availability", "available", "stock");
    private static final Set<String> ATTRIBUTE_TOKENS = Set.of("attribute", "feature", "specification", "characteristic");
    private static final Set<String> PRICE_FIELDS = Set.of("price", "minprice", "prices", "offers", "offer");

    private LegacyBackupFieldDispositionRegistry() {
    }

    /**
     * Classifies one observed field name.
     *
     * @param fieldName raw legacy field name as observed in the JSONL payload
     * @return reviewed disposition for that field name
     */
    public static LegacyFieldDisposition classify(String fieldName) {
        String normalized = fieldName.toLowerCase(Locale.ROOT);
        if (GTIN_FIELDS.contains(normalized)) {
            return LegacyFieldDisposition.UNATTRIBUTED_IDENTITY;
        }
        if (normalized.contains("amazon")) {
            return LegacyFieldDisposition.QUARANTINE;
        }
        if (normalized.contains("price") || PRICE_FIELDS.contains(normalized)) {
            return LegacyFieldDisposition.LEGACY_MINIMUM_PRICE;
        }
        if (containsAny(normalized, RECOMPUTED_TOKENS)) {
            return LegacyFieldDisposition.RECOMPUTED;
        }
        if (containsAny(normalized, ATTRIBUTE_TOKENS)) {
            return LegacyFieldDisposition.NATIVE_EVIDENCED_CONVERSION;
        }
        if (NATIVE_SCALAR_FIELDS.contains(normalized)) {
            return LegacyFieldDisposition.NATIVE_EVIDENCED_CONVERSION;
        }
        return LegacyFieldDisposition.QUARANTINE;
    }

    /**
     * Reports whether a native-scalar field can be converted directly by this importer without
     * the deferred 94-entry attribute registry.
     *
     * @param fieldName raw legacy field name
     * @return {@code true} for the small, reviewed subset of directly convertible identity/text fields
     */
    public static boolean isDirectlyConvertibleNativeField(String fieldName) {
        return NATIVE_SCALAR_FIELDS.contains(fieldName.toLowerCase(Locale.ROOT));
    }

    private static boolean containsAny(String normalized, Set<String> tokens) {
        return tokens.stream().anyMatch(normalized::contains);
    }
}
