package org.open4goods.datareference.model;

/**
 * Stable O4G attribute identifier.
 *
 * @param slug independently authored lower-case kebab-case slug
 */
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

    @Override
    public String toString() {
        return externalForm();
    }
}
