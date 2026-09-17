package org.open4goods.pricehistory.model;

/** Condition advertised for one provider offer. */
public enum OfferCondition {
    /** New retail product. */
    NEW,
    /** Used or refurbished product. */
    OCCASION,
    /** The provider did not publish a usable condition. */
    UNKNOWN
}
