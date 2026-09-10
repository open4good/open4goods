package org.open4goods.datareference.model;

import java.util.Locale;

/**
 * Digest of a retrieved provider payload.
 *
 * @param algorithm digest algorithm such as {@code SHA-256}
 * @param hexadecimalValue lower-case hexadecimal digest
 */
public record PayloadHash(String algorithm, String hexadecimalValue) {

    /**
     * Validates and canonicalizes the digest description.
     */
    public PayloadHash {
        if (algorithm == null || algorithm.isBlank()) {
            throw new IllegalArgumentException("hash algorithm must not be blank");
        }
        if (hexadecimalValue == null || !hexadecimalValue.matches("[0-9a-fA-F]+")
                || hexadecimalValue.length() % 2 != 0) {
            throw new IllegalArgumentException("hash value must be an even-length hexadecimal string");
        }
        algorithm = algorithm.trim().toUpperCase(Locale.ROOT);
        hexadecimalValue = hexadecimalValue.toLowerCase(Locale.ROOT);
    }
}
