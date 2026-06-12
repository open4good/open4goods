package org.open4goods.nudgerfrontapi.dto.brand;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * User-to-manufacturing distance for a product. Per-user and not cacheable;
 * the static manufacturing chain is exposed separately via {@code GET /brands/{brandName}}.
 *
 * @param userCountry resolved user country code
 * @param userCity resolved user city
 * @param latitude resolved user latitude
 * @param longitude resolved user longitude
 * @param sites per-site distances (nearest first)
 */
@Schema(name = "ManufacturingDistance", description = "Distance from the requesting user to the product's manufacturing sites.")
public record ManufacturingDistanceDto(
        @Schema(description = "Resolved user country code.", example = "FR", nullable = true)
        String userCountry,
        @Schema(description = "Resolved user city.", example = "Lyon", nullable = true)
        String userCity,
        @Schema(description = "Resolved user latitude.", example = "45.76", nullable = true)
        Double latitude,
        @Schema(description = "Resolved user longitude.", example = "4.84", nullable = true)
        Double longitude,
        @Schema(description = "Per-site distances, nearest first.")
        List<ManufacturingSiteDistanceDto> sites) {
}
