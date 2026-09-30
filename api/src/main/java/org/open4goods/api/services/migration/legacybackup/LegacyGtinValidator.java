package org.open4goods.api.services.migration.legacybackup;

import java.util.Optional;
import java.util.Set;

import org.open4goods.datareference.model.Gtin;

/**
 * GTIN-8/12/13/14 check-digit validation, ported from {@code valid_gtin()} in
 * {@code scripts/migration/pin_product_backup.py}.
 *
 * <p>{@link Gtin} itself only validates syntax (digit count), deferring check-digit
 * verification to ingestion boundaries such as this importer.
 */
public final class LegacyGtinValidator {

    private static final Set<Integer> VALID_LENGTHS = Set.of(8, 12, 13, 14);

    private LegacyGtinValidator() {
    }

    /**
     * Validates a candidate GTIN string's syntax and check digit.
     *
     * @param candidate raw candidate value, or {@code null}
     * @return the validated {@link Gtin}, or empty when the candidate is not a valid GTIN
     */
    public static Optional<Gtin> validate(String candidate) {
        if (candidate == null) {
            return Optional.empty();
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty() || !trimmed.chars().allMatch(Character::isDigit) || !VALID_LENGTHS.contains(trimmed.length())) {
            return Optional.empty();
        }
        if (!hasValidCheckDigit(trimmed)) {
            return Optional.empty();
        }
        return Optional.of(new Gtin(trimmed));
    }

    private static boolean hasValidCheckDigit(String digits) {
        int length = digits.length();
        int total = 0;
        for (int index = 0; index < length - 1; index++) {
            int digit = digits.charAt(index) - '0';
            int weight = index % 2 == length % 2 ? 3 : 1;
            total += digit * weight;
        }
        int expected = (10 - total % 10) % 10;
        int actual = digits.charAt(length - 1) - '0';
        return expected == actual;
    }
}
