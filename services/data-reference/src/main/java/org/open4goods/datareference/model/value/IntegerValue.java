package org.open4goods.datareference.model.value;

import java.math.BigInteger;
import java.util.Objects;

/**
 * Arbitrary-precision canonical integer.
 *
 * @param value integer value
 */
public record IntegerValue(BigInteger value) implements CanonicalValue {

    /** Validates the integer value. */
    public IntegerValue {
        Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.INTEGER;
    }
}
