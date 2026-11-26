package org.open4goods.nudgerfrontapi.dto.opendata;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Column of a published OpenData CSV, as described by the manifest (GOU-2251).
 *
 * @param name        column header
 * @param fillRate    share of rows where the column is filled, between 0 and 1
 * @param unit        unit of the values, null when none
 * @param description label of the column, null when the manifest has none
 * @param source      origin of the values (EPREL, marketplace, Icecat...), null when the manifest has none
 */
@Schema(description = "Column of a published OpenData CSV, as described by the manifest.")
public record OpenDataColumnDto(
        @Schema(description = "Column header.", example = "warranty_year")
        String name,

        @Schema(description = "Share of rows where the column is filled, between 0 and 1.", example = "0.42")
        double fillRate,

        @Schema(description = "Unit of the values.", example = "Year(s)", nullable = true)
        String unit,

        @Schema(description = "Label of the column.", example = "Durée de garantie", nullable = true)
        String description,

        @Schema(description = "Origin of the values.", example = "Attribut WARRANTY (EPREL, Icecat, marchands)",
                nullable = true)
        String source
) {
}
