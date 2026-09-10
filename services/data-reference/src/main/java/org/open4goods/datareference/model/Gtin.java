package org.open4goods.datareference.model;

import java.util.Set;

/**
 * GTIN product identity, preserving leading zeroes.
 *
 * @param value GTIN-8, GTIN-12, GTIN-13, or GTIN-14 digits
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record Gtin(@JsonValue String value) {

    private static final Set<Integer> VALID_LENGTHS = Set.of(8, 12, 13, 14);

    /**
     * Validates the syntactic GTIN contract. Check-digit verification remains the
     * responsibility of the GTIN service at ingestion boundaries.
     */
    public Gtin {
        if (value == null || !value.chars().allMatch(Character::isDigit)
                || !VALID_LENGTHS.contains(value.length())) {
            throw new IllegalArgumentException("GTIN must contain 8, 12, 13, or 14 digits");
        }
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized value
     * @return validated identifier
     */
    @JsonCreator
    public static Gtin fromJson(String value) {
        return new Gtin(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
