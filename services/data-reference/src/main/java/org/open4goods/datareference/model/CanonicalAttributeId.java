package org.open4goods.datareference.model;

/**
 * Stable O4G attribute identifier.
 *
 * @param slug independently authored lower-case kebab-case slug
 */
import com.fasterxml.jackson.annotation.JsonCreator;

public record CanonicalAttributeId(String slug) implements CanonicalConceptId {

    /**
     * Validates the attribute identifier.
     */
    public CanonicalAttributeId {
        slug = CanonicalConceptId.requireSlug(slug);
    }

    @Override
    public String kind() {
        return "attribute";
    }

    /**
     * Rebuilds the identifier from its serialized form.
     *
     * @param value serialized identifier such as {@code o4g:attribute:width}
     * @return validated identifier
     */
    @JsonCreator
    public static CanonicalAttributeId fromJson(String value) {
        if (!(CanonicalConceptId.parse(value) instanceof CanonicalAttributeId id)) {
            throw new IllegalArgumentException("Not an O4G attribute identifier: " + value);
        }
        return id;
    }

    @Override
    public String toString() {
        return externalForm();
    }
}
