package org.open4goods.datareference.model.value;

/**
 * Closed set of canonical product-reference value types.
 */
public enum CanonicalValueType {
    /** Localized human-readable text. */
    LOCALIZED_TEXT,
    /** Boolean fact. */
    BOOLEAN,
    /** Arbitrary-precision integer. */
    INTEGER,
    /** Arbitrary-precision decimal. */
    DECIMAL,
    /** Dimensioned decimal quantity with a UCUM unit. */
    QUANTITY,
    /** Code from a named code system. */
    CODE,
    /** Calendar date without a time zone. */
    DATE,
    /** Uniform resource identifier. */
    URI
}
