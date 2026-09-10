package org.open4goods.datareference.model;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable identifier for an independently authored O4G class or attribute.
 */
public sealed interface CanonicalConceptId permits CanonicalAttributeId, CanonicalClassId {

    Pattern SLUG_PATTERN = Pattern.compile("[a-z0-9]+(?:-[a-z0-9]+)*");

    /**
     * Returns the concept slug without its O4G prefix.
     *
     * @return lower-case kebab-case slug
     */
    String slug();

    /**
     * Returns the identifier kind used in its external form.
     *
     * @return {@code class} or {@code attribute}
     */
    String kind();

    /**
     * Returns the stable serialized form of this identifier.
     *
     * @return identifier such as {@code o4g:attribute:width}
     */
    default String externalForm() {
        return "o4g:" + kind() + ":" + slug();
    }

    /**
     * Parses a serialized canonical identifier.
     *
     * @param value serialized identifier
     * @return typed canonical identifier
     * @throws IllegalArgumentException when the prefix, kind, or slug is invalid
     */
    static CanonicalConceptId parse(String value) {
        Objects.requireNonNull(value, "value must not be null");
        if (value.startsWith("o4g:class:")) {
            return new CanonicalClassId(value.substring("o4g:class:".length()));
        }
        if (value.startsWith("o4g:attribute:")) {
            return new CanonicalAttributeId(value.substring("o4g:attribute:".length()));
        }
        throw new IllegalArgumentException("Unsupported O4G concept identifier: " + value);
    }

    /**
     * Validates a concept slug.
     *
     * @param slug slug to validate
     * @return the unchanged valid slug
     * @throws IllegalArgumentException when the slug is not lower-case kebab-case
     */
    static String requireSlug(String slug) {
        Objects.requireNonNull(slug, "slug must not be null");
        if (!SLUG_PATTERN.matcher(slug).matches()) {
            throw new IllegalArgumentException("Canonical concept slug must be lower-case kebab-case: " + slug);
        }
        return slug;
    }
}
