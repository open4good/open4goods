package org.open4goods.nudgerfrontapi.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A company location (typically its headquarters).
 *
 * @param country ISO-3166-1 alpha-2 country code
 * @param countryName localized country name
 * @param city city name
 * @param latitude latitude
 * @param longitude longitude
 */
@Schema(name = "CompanyLocation", description = "A company location with optional coordinates.")
public record CompanyLocationDto(
        @Schema(description = "Country ISO code.", example = "US", nullable = true)
        String country,
        @Schema(description = "Localized country name.", example = "United States", nullable = true)
        String countryName,
        @Schema(description = "City.", example = "Cupertino", nullable = true)
        String city,
        @Schema(description = "Latitude.", example = "37.33", nullable = true)
        Double latitude,
        @Schema(description = "Longitude.", example = "-122.01", nullable = true)
        Double longitude) {
}
