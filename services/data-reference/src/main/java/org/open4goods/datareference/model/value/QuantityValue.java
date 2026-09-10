package org.open4goods.datareference.model.value;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Objects;

/**
 * Canonical quantity represented without binary floating-point loss.
 *
 * @param value decimal quantity
 * @param dimension canonical physical dimension
 * @param ucumCode canonical UCUM code selected by the attribute contract
 */
public record QuantityValue(BigDecimal value, String dimension, String ucumCode) implements CanonicalValue {

    /**
     * Validates the quantity and preserves the case-sensitive UCUM code.
     */
    public QuantityValue {
        Objects.requireNonNull(value, "value must not be null");
        if (dimension == null || dimension.isBlank() || ucumCode == null || ucumCode.isBlank()) {
            throw new IllegalArgumentException("quantity dimension and UCUM code must not be blank");
        }
        dimension = dimension.trim().toUpperCase(Locale.ROOT);
        ucumCode = ucumCode.trim();
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.QUANTITY;
    }
}
