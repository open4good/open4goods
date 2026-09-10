package org.open4goods.datareference.model.projection;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.value.CanonicalValue;

/**
 * Fields another domain contributes to a product projection.
 *
 * <p>Offer summaries, evaluation outputs and search fields are not reference
 * facts and are not resolved from sources, but they live in the same document
 * so that a read serves a page in one call. They arrive already computed: this
 * module composes them, and never derives them.
 *
 * @param name slice name, unique within one projection
 * @param fields slice fields, keyed by a name meaningful to that domain
 */
public record DomainSlice(String name, Map<String, CanonicalValue> fields) {

    /**
     * Copies the slice content.
     */
    public DomainSlice {
        Objects.requireNonNull(name, "name must not be null");
        name = name.trim();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("slice name must not be blank");
        }
        // Insertion order is kept for the same reason as on the projection: an
        // immutable map's order is unspecified, and a frozen document cannot pin
        // a shape that varies between runs.
        fields = Collections.unmodifiableMap(
                new LinkedHashMap<>(Objects.requireNonNull(fields, "fields must not be null")));
    }
}
