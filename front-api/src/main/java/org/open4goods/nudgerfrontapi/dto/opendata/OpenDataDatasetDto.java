package org.open4goods.nudgerfrontapi.dto.opendata;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a single OpenData dataset (either GTIN or ISBN).
 * <p>
 * {@code recordCount}, {@code recordCountValue}, {@code lastUpdated}, {@code fileSize} and {@code sha256} come from the
 * entry of the dataset in the published {@code manifest.json} (GOU-2290), never from the live index: they are null
 * while the manifest is not configured or was never read successfully.
 * </p>
 */
@Schema(description = "Detailed description of a single OpenData dataset. Values read from the published manifest are "
        + "null while it is not configured or not readable.")
public record OpenDataDatasetDto(
        @Schema(description = "Identifier of the dataset.", allowableValues = {"GTIN", "ISBN"}, example = "GTIN")
        String type,

        @Schema(description = "Rows of the published file (manifest rows), formatted using the requested locale; null "
                + "when unknown.", example = "115 786 654", nullable = true)
        String recordCount,

        @Schema(description = "Raw rows of the published file (manifest rows); null when unknown.",
                example = "115786654", nullable = true)
        Long recordCountValue,

        @Schema(description = "Generation date of the published file (manifest generatedAt), formatted using the "
                + "requested locale in the Europe/Paris time zone; null when unknown.",
                example = "22 nov. 2026, 03:55:00", nullable = true)
        String lastUpdated,

        @Schema(description = "Size of the published zip (manifest bytes), formatted using the requested locale; null "
                + "when unknown.", example = "6,4 GB", nullable = true)
        String fileSize,

        @Schema(description = "Lowercase hexadecimal SHA-256 of the published zip (manifest sha256); null when unknown.",
                example = "9f86d081884c7d659a2feaa0c55ad015a3bf4f1b2b0b822cd15d6c15b0f00a08", nullable = true)
        String sha256,

        @Schema(description = "Absolute URL of the published zip (open-data-config.public-base-url); null when no public "
                + "base URL is configured.", format = "uri", example = "https://data.nudger.fr/gtin-open-data.zip",
                nullable = true)
        String downloadUrl,

        @ArraySchema(schema = @Schema(description = "Column header available in the dataset.", example = "gtin"))
        List<String> headers
) {
}
