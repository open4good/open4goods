package org.open4goods.model.product;

/**
 * Classifies a GTIN by its GS1 prefix range, indicating the numbering authority or product category.
 *
 * <p>GS1 prefix ranges are defined at https://www.gs1.org/standards/id-keys/company-prefix
 */
public enum Gs1Class {

    /** Standard trade item (most physical products). */
    GTIN,

    /** ISBN-13 book (978/979 prefix, excluding 979-0). */
    ISBN_BOOKLAND,

    /** ISMN music publication (979-0 prefix). */
    ISMN_MUSIC,

    /** ISSN periodical or serial publication (977 prefix). */
    ISSN_PERIODICAL,

    /**
     * Restricted circulation / in-store / variable-weight item.
     * Covers prefixes 020-029, 040-049, and 200-299.
     */
    RESTRICTED_INTERNAL,

    /** Coupon (prefixes 980-999). */
    COUPON,

    /** Classification cannot be determined from available data. */
    UNKNOWN
}
