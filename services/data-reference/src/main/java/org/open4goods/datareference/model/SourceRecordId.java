package org.open4goods.datareference.model;

import java.util.Objects;

/**
 * Identifier a source gives to one of its own records.
 *
 * <p>Provider spelling is preserved: an identifier is only meaningful to the
 * source that issued it, so it is trimmed but never case-folded or rewritten.
 *
 * @param value identifier exactly as issued by the source
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SourceRecordId(@JsonValue String value) {

    /** Longest identifier accepted, guarding stores against unbounded keys. */
    public static final int MAX_LENGTH = 512;

    /**
     * Validates the provider identifier without altering its spelling.
     */
    public SourceRecordId {
        Objects.requireNonNull(value, "source record id must not be null");
        value = value.trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("source record id must not be blank");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("source record id must not exceed " + MAX_LENGTH + " characters");
        }
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized value
     * @return validated identifier
     */
    @JsonCreator
    public static SourceRecordId fromJson(String value) {
        return new SourceRecordId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
