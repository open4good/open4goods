package org.open4goods.services.geocode.model;

/**
 * Immutable representation of a GeoNames city entry.
 *
 * @param geonameId GeoNames identifier
 * @param name canonical city name
 * @param asciiName ASCII-friendly city name
 * @param countryCode ISO-2 country code
 * @param latitude latitude in decimal degrees
 * @param longitude longitude in decimal degrees
 * @param population population value (may be 0 when unknown)
 * @param featureClass GeoNames feature class for tie-break logic
 */
public record CityRecord(
        long geonameId,
        String name,
        String asciiName,
        String countryCode,
        double latitude,
        double longitude,
        long population,
        String featureClass)
{
}
