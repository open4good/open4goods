package org.open4goods.services.geocode.dto;

/**
 * DTO carrying MaxMind GeoIP lookup information for an IP address.
 *
 * @param ip IP address that was resolved
 * @param continentName continent name
 * @param continentCode continent ISO code
 * @param countryName country name
 * @param countryIsoCode country ISO code
 * @param registeredCountryName registered country name
 * @param registeredCountryIsoCode registered country ISO code
 * @param cityName city name
 * @param subdivisionName subdivision (region/state) name
 * @param subdivisionIsoCode subdivision ISO code
 * @param postalCode postal code
 * @param latitude latitude coordinate
 * @param longitude longitude coordinate
 * @param accuracyRadiusKm accuracy radius in kilometers
 * @param timeZone time zone identifier
 * @param metroCode metro code (where available)
 * @param isAnonymousProxy flag indicating anonymous proxy usage
 * @param isAnycast flag indicating anycast routing
 */
public record IpGeolocationResponse(
        String ip,
        String continentName,
        String continentCode,
        String countryName,
        String countryIsoCode,
        String registeredCountryName,
        String registeredCountryIsoCode,
        String cityName,
        String subdivisionName,
        String subdivisionIsoCode,
        String postalCode,
        Double latitude,
        Double longitude,
        Integer accuracyRadiusKm,
        String timeZone,
        Integer metroCode,
        Boolean isAnonymousProxy,
        Boolean isAnycast)
{
}
