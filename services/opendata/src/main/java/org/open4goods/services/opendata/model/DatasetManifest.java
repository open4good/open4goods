package org.open4goods.services.opendata.model;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Manifest of one dataset, published alone ({@code gtin-manifest.json}, {@code isbn-manifest.json}, GOU-2239) and as an
 * entry of {@link OpenDataManifest}.
 *
 * @param schemaVersion version of the manifest contract ({@link OpenDataManifest#SCHEMA_VERSION})
 * @param id            dataset identifier ({@code gtin}, {@code isbn})
 * @param file          public object name of the zip
 * @param url           public download URL, absent when no public base URL is configured
 * @param format        media type of the file
 * @param license       SPDX identifier of the licence of the data
 * @param bytes         size of the zip in bytes
 * @param sha256        lowercase hexadecimal SHA-256 of the zip
 * @param rows          data rows of the CSV (header excluded)
 * @param indexCount    products of the matching barcode types in the index at the start of the pass
 * @param generatedAt   end of the writing of this dataset, ISO 8601 UTC
 * @param columns       CSV columns, in file order, with their fill rate
 * @param sample        public object name of the CSV sample (header and the first 20 rows), absent when none
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record DatasetManifest(
        int schemaVersion,
        String id,
        String file,
        String url,
        String format,
        String license,
        long bytes,
        String sha256,
        long rows,
        long indexCount,
        String generatedAt,
        List<ColumnManifest> columns,
        String sample) {

    /** Media type of a dataset: one UTF-8 CSV in a zip. */
    public static final String FORMAT = "text/csv+zip";
}
