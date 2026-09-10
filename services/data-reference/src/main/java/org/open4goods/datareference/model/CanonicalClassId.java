package org.open4goods.datareference.model;

/**
 * Stable O4G product class identifier.
 *
 * @param slug independently authored lower-case kebab-case slug
 */
import com.fasterxml.jackson.annotation.JsonCreator;

public record CanonicalClassId(String slug) implements CanonicalConceptId {

    /**
     * Validates the class identifier.
     */
    public CanonicalClassId {
        slug = CanonicalConceptId.requireSlug(slug);
    }

    @Override
    public String kind() {
        return "class";
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized identifier such as {@code o4g:class:width}
     * @return validated identifier
     */
    @JsonCreator
    public static CanonicalClassId fromJson(String value) {
        if (!(CanonicalConceptId.parse(value) instanceof CanonicalClassId id)) {
            throw new IllegalArgumentException("Not an O4G class identifier: " + value);
        }
        return id;
    }

    @Override
    public String toString() {
        return externalForm();
    }
}
