package org.open4goods.nudgerfrontapi.dto.brand;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Distance from the user to a single manufacturing site.
 *
 * @param country ISO country code
 * @param countryName localized country name
 * @param city city name
 * @param type site type
 * @param operator contract manufacturer, when distinct
 * @param latitude site latitude
 * @param longitude site longitude
 * @param distanceKm great-circle distance from the user in kilometers
 */
@Schema(name = "ManufacturingSiteDistance", description = "Distance from the user to a manufacturing site.")
public record ManufacturingSiteDistanceDto(
        @Schema(description = "Country ISO code.", example = "CN")
        String country,
        @Schema(description = "Localized country name.", example = "China", nullable = true)
        String countryName,
        @Schema(description = "City.", example = "Zhengzhou", nullable = true)
        String city,
        @Schema(description = "Site type.", example = "factory")
        String type,
        @Schema(description = "Contract manufacturer.", example = "Foxconn", nullable = true)
        String operator,
        @Schema(description = "Site latitude.", example = "34.74", nullable = true)
        Double latitude,
        @Schema(description = "Site longitude.", example = "113.62", nullable = true)
        Double longitude,
        @Schema(description = "Great-circle distance from the user (km).", example = "8700.0", nullable = true)
        Double distanceKm) {
}
