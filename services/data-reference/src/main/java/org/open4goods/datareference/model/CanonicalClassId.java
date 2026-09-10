package org.open4goods.datareference.model;

/**
 * Stable O4G product class identifier.
 *
 * @param slug independently authored lower-case kebab-case slug
 */
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

    @Override
    public String toString() {
        return externalForm();
    }
}
