package org.open4goods.pricehistory.model;

/** Reason an immutable price-history event was appended. */
public enum PriceChangeKind {
    /** First observation of an offer. */
    FIRST_SEEN,
    /** Price, currency, condition or availability changed. */
    CHANGED,
    /** A complete feed no longer contained a previously present offer. */
    DISAPPEARED,
    /** A previously disappeared offer was observed again. */
    REAPPEARED,
    /** A neutral, one-time legacy minimum-price backfill. */
    LEGACY_BACKFILL
}
