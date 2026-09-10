package org.open4goods.datareference.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Identity of a data source, independent of any provider-specific model.
 *
 * <p>A source is the party that made an observation, not the transport that
 * carried it: {@code icecat}, {@code eprel} and one merchant feed are distinct
 * sources even when a single importer fetches them.
 *
 * @param value lower-case source identifier
 */
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public record SourceId(@JsonValue String value) {

    private static final Pattern PATTERN = Pattern.compile("[a-z0-9]+(?:[.\\-][a-z0-9]+)*");

    /**
     * Validates and canonicalizes the source identifier.
     */
    public SourceId {
        Objects.requireNonNull(value, "source id must not be null");
        value = value.trim().toLowerCase(Locale.ROOT);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(
                    "source id must be lower-case alphanumeric separated by '.' or '-': " + value);
        }
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized value
     * @return validated identifier
     */
    @JsonCreator
    public static SourceId fromJson(String value) {
        return new SourceId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
