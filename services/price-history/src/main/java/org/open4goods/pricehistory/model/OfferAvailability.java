package org.open4goods.pricehistory.model;

/** Availability state independently observed for a provider offer. */
public enum OfferAvailability {
    /** The provider currently lists the offer as available. */
    AVAILABLE,
    /** The provider currently lists the offer as unavailable. */
    UNAVAILABLE,
    /** The feed did not publish a usable availability value. */
    UNKNOWN
}
