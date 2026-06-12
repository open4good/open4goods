package org.open4goods.nudgerfrontapi.dto.brand;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * A manufacturing site relevant to a product category.
 *
 * @param categories vertical ids the site applies to (empty = all categories)
 * @param country ISO-3166-1 alpha-2 country code
 * @param countryName localized country name
 * @param city city name
 * @param latitude latitude
 * @param longitude longitude
 * @param type site type (factory / assembly / hq)
 * @param operator contract manufacturer operating the site, when distinct
 * @param sources sourcing references (attribution)
 */
@Schema(name = "ManufacturingSite", description = "A place where the company manufactures or assembles products.")
public record ManufacturingSiteDto(
        @Schema(description = "Vertical ids this site applies to; empty means all categories.")
        List<String> categories,
        @Schema(description = "Country ISO code.", example = "CN")
        String country,
        @Schema(description = "Localized country name.", example = "China", nullable = true)
        String countryName,
        @Schema(description = "City.", example = "Zhengzhou", nullable = true)
        String city,
        @Schema(description = "Latitude.", example = "34.74", nullable = true)
        Double latitude,
        @Schema(description = "Longitude.", example = "113.62", nullable = true)
        Double longitude,
        @Schema(description = "Site type.", example = "factory")
        String type,
        @Schema(description = "Contract manufacturer operating the site.", example = "Foxconn", nullable = true)
        String operator,
        @Schema(description = "Sourcing references for attribution.")
        List<SourcedReferenceDto> sources) {
}
