package org.open4goods.pricehistory.model;

/** Queryable history representations. */
public enum PriceHistoryGranularity {
    /** Sparse immutable price changes retained for 24 months. */
    CHANGE,
    /** Daily provider rollups retained for five years. */
    DAY
}
