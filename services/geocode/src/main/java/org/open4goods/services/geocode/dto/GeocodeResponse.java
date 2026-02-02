package org.open4goods.services.geocode.dto;

import org.open4goods.services.geocode.model.MatchType;

/**
 * Response payload for geocode lookups.
 *
 * @param city requested city name
 * @param country requested country code
 * @param matchedName canonical GeoNames name
 * @param geonameId GeoNames identifier
 * @param latitude latitude in decimal degrees
 * @param longitude longitude in decimal degrees
 * @param population population value (may be 0 when unknown)
 * @param matchType match type used for lookup
 */
public record GeocodeResponse(
        String city,
        String country,
        String matchedName,
        long geonameId,
        double latitude,
        double longitude,
        long population,
        MatchType matchType)
{
}
