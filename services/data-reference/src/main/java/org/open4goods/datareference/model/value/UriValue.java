package org.open4goods.datareference.model.value;

import java.net.URI;
import java.util.Objects;

/**
 * Canonical URI value.
 *
 * @param value URI value
 */
public record UriValue(URI value) implements CanonicalValue {

    /** Validates the URI. */
    public UriValue {
        Objects.requireNonNull(value, "value must not be null");
    }

    @Override
    public CanonicalValueType type() {
        return CanonicalValueType.URI;
    }
}
