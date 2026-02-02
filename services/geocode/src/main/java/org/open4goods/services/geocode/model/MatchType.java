package org.open4goods.services.geocode.model;

/**
 * Describes which name variant produced a geocode match.
 */
public enum MatchType
{
    /**
     * Matched on the primary GeoNames name.
     */
    PRIMARY,
    /**
     * Matched on the ASCII name.
     */
    ASCII,
    /**
     * Matched on an alternate name.
     */
    ALTERNATE
}
