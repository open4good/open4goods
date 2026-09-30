package org.open4goods.datareference.model.registry;

/**
 * How many values one canonical attribute may hold on one product.
 */
public enum CanonicalCardinality {
    /** Exactly one value wins; further candidates are conflicts. */
    SINGLE,
    /** Several values legitimately coexist, such as available colours. */
    MULTIPLE
}
