package org.open4goods.nudgerfrontapi.dto.opendata;

import java.util.Date;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata about available open data datasets.
 */
public record OpenDataMetaDto(
        @Schema(description = "Number of GTIN products", example = "1000")
        long countGtin,
        @Schema(description = "Number of ISBN products", example = "500")
        long countIsbn,
        @Schema(description = "Size of the GTIN dataset", example = "10 MB")
        String gtinFileSize,
        @Schema(description = "Size of the ISBN dataset", example = "2 MB")
        String isbnFileSize,
        @Schema(description = "Last update timestamp of GTIN dataset")
        Date gtinLastUpdated,
        @Schema(description = "Last update timestamp of ISBN dataset")
        Date isbnLastUpdated
) {}
