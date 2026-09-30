package org.open4goods.datareference.model;

import java.util.Objects;

/**
 * Stable identity of one assertion inside its provider record.
 *
 * <p>Projections store the winning assertion id per canonical field, so the id
 * must survive a head replacement that re-states the same coordinate. It is
 * therefore derived from the record key, the provider field and the ordinal
 * rather than allocated at write time.
 *
 * @param value stable assertion identifier
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record AssertionId(@JsonValue String value) {

    /**
     * Validates the identifier.
     */
    public AssertionId {
        Objects.requireNonNull(value, "assertion id must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("assertion id must not be blank");
        }
    }

    /**
     * Derives the stable identifier of one assertion coordinate.
     *
     * @param recordKey record the assertion belongs to
     * @param field versioned provider field
     * @param ordinal zero-based position among values of that field
     * @return derived assertion identifier
     */
    public static AssertionId of(SourceRecordKey recordKey, SourceFieldId field, int ordinal) {
        Objects.requireNonNull(recordKey, "recordKey must not be null");
        Objects.requireNonNull(field, "field must not be null");
        if (ordinal < 0) {
            throw new IllegalArgumentException("ordinal must be non-negative");
        }
        return new AssertionId(recordKey.externalForm() + "#" + field.externalForm() + "#" + ordinal);
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized value
     * @return validated identifier
     */
    @JsonCreator
    public static AssertionId fromJson(String value) {
        return new AssertionId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
