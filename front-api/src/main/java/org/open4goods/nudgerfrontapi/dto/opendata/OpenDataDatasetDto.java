package org.open4goods.nudgerfrontapi.dto.opendata;

import java.util.List;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO describing a single OpenData dataset (either GTIN or ISBN).
 */
@Schema(description = "Detailed description of a single OpenData dataset.")
public record OpenDataDatasetDto(
        @Schema(description = "Identifier of the dataset.", allowableValues = {"GTIN", "ISBN"}, example = "GTIN")
        String type,

        @Schema(description = "Number of rows in the dataset formatted using the requested locale.", example = "150\u00a0000")
        String recordCount,

        @Schema(description = "Raw number of rows in the dataset.", example = "1500000")
        long recordCountValue,

        @Schema(description = "Last update timestamp formatted using the requested locale.",
                example = "12 mars 2024, 10:15:42", nullable = true)
        String lastUpdated,

        @Schema(description = "Human readable size of the dataset formatted using the requested locale.", example = "512\u00a0MB")
        String fileSize,

        @Schema(description = "Absolute URL that can be used to download the dataset.", format = "uri",
                example = "https://nudger.fr/opendata/gtin-open-data.zip")
        String downloadUrl,

        @ArraySchema(schema = @Schema(description = "Column header available in the dataset.", example = "gtin"))
        List<String> headers
) {
}
