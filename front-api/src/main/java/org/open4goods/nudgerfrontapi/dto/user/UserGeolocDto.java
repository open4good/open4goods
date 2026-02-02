package org.open4goods.nudgerfrontapi.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO containing enriched geolocation data for the current user.
 *
 * @param ip IP address used for resolution
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
 * @param anonymousProxy whether the IP is flagged as anonymous proxy
 * @param anycast whether the IP is flagged as anycast
 */
@Schema(name = "UserGeoloc", description = "MaxMind GeoIP geolocation data for the requesting user.")
public record UserGeolocDto(
        @Schema(description = "Resolved IP address.", example = "81.2.69.142")
        String ip,
        @Schema(description = "Continent name.", example = "Europe", nullable = true)
        String continentName,
        @Schema(description = "Continent ISO code.", example = "EU", nullable = true)
        String continentCode,
        @Schema(description = "Country name.", example = "United Kingdom", nullable = true)
        String countryName,
        @Schema(description = "Country ISO code.", example = "GB", nullable = true)
        String countryIsoCode,
        @Schema(description = "Registered country name.", example = "United Kingdom", nullable = true)
        String registeredCountryName,
        @Schema(description = "Registered country ISO code.", example = "GB", nullable = true)
        String registeredCountryIsoCode,
        @Schema(description = "City name.", example = "London", nullable = true)
        String cityName,
        @Schema(description = "Subdivision (region/state) name.", example = "England", nullable = true)
        String subdivisionName,
        @Schema(description = "Subdivision ISO code.", example = "ENG", nullable = true)
        String subdivisionIsoCode,
        @Schema(description = "Postal code.", example = "EC1A", nullable = true)
        String postalCode,
        @Schema(description = "Latitude.", example = "51.5142", nullable = true)
        Double latitude,
        @Schema(description = "Longitude.", example = "-0.0931", nullable = true)
        Double longitude,
        @Schema(description = "Accuracy radius in kilometers.", example = "5", nullable = true)
        Integer accuracyRadiusKm,
        @Schema(description = "Timezone identifier.", example = "Europe/London", nullable = true)
        String timeZone,
        @Schema(description = "Metro code.", example = "0", nullable = true)
        Integer metroCode,
        @Schema(description = "Whether the IP is flagged as anonymous proxy.", example = "false", nullable = true)
        Boolean anonymousProxy,
        @Schema(description = "Whether the IP is flagged as anycast.", example = "false", nullable = true)
        Boolean anycast)
{
}
