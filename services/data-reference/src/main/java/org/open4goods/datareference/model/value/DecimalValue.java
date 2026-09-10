package org.open4goods.datareference.model.value;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Arbitrary-precision canonical decimal.
 *
 * @param value decimal value
 */
public record DecimalValue(BigDecimal value) implements CanonicalValue {

    /** Validates the decimal value. */
    public DecimalValue {
        Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.DECIMAL;
    }
}
