package org.open4goods.datareference.model.value;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

import org.open4goods.datareference.model.UcumCode;

/**
 * A canonical quantity: a decimal amount and the UCUM unit it is expressed in.
 *
 * <p>The amount is {@link BigDecimal} so that a stored value survives a
 * round-trip unchanged. Binary floating point would make {@code 0.55 m} and the
 * value read back from the index differ in the last digits, and a comparison
 * between two sources would then depend on how each was parsed.
 *
 * @param amount decimal quantity
 * @param dimension canonical physical dimension, such as {@code LENGTH}
 * @param unit validated UCUM unit the amount is expressed in
 */
public record QuantityValue(BigDecimal amount, String dimension, UcumCode unit) implements CanonicalValue {

    /**
     * Validates the quantity.
     */
    public QuantityValue {
        Objects.requireNonNull(amount, "amount must not be null");
        Objects.requireNonNull(unit, "unit must not be null");
        if (dimension == null || dimension.isBlank()) {
            throw new IllegalArgumentException("quantity dimension must not be blank");
        }
        dimension = dimension.trim().toUpperCase(Locale.ROOT);
    }

    /**
     * Builds a quantity from a raw UCUM code.
     *
     * @param amount decimal quantity
     * @param dimension canonical physical dimension
     * @param ucumCode UCUM unit code
     * @return validated quantity
     */
    public static QuantityValue of(BigDecimal amount, String dimension, String ucumCode) {
        return new QuantityValue(amount, dimension, new UcumCode(ucumCode));
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.QUANTITY;
    }
}
