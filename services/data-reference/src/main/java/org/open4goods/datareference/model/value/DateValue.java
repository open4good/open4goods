package org.open4goods.datareference.model.value;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Canonical calendar date.
 *
 * @param value date without a time zone
 */
public record DateValue(LocalDate value) implements CanonicalValue {

    /** Validates the date. */
    public DateValue {
        Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.DATE;
    }
}
