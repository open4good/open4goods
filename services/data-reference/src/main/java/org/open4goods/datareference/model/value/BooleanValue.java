package org.open4goods.datareference.model.value;

/**
 * Canonical boolean value.
 *
 * @param value boolean fact
 */
public record BooleanValue(boolean value) implements CanonicalValue {

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.BOOLEAN;
    }
}
