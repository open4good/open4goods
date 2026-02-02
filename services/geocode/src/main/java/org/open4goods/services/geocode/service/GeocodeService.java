package org.open4goods.services.geocode.service;

import org.open4goods.services.geocode.model.CityMatch;

/**
 * Service API for geocoding city names.
 */
public interface GeocodeService
{
    /**
     * Resolves a city name to a GeoNames record.
     *
     * @param city city name
     * @param countryCode ISO-2 country code
     * @return city match
     */
    CityMatch resolve(String city, String countryCode);
}
