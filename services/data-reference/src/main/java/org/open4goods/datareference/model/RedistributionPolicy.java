package org.open4goods.datareference.model;

/**
 * Redistribution permission recorded for a source policy.
 */
public enum RedistributionPolicy {
    /** No redistribution is permitted. */
    PROHIBITED,
    /** Redistribution is permitted only within a value-added service. */
    VALUE_ADDED_ONLY,
    /** Redistribution is permitted by the reviewed source terms. */
    ALLOWED
}
