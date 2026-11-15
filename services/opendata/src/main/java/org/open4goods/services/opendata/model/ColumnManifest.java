package org.open4goods.services.opendata.model;

/**
 * Column of a dataset in its manifest.
 *
 * @param name     CSV header of the column
 * @param fillRate share of data rows with a non-empty value, from 0 to 1 (0 when the dataset has no row)
 */
public record ColumnManifest(String name, double fillRate) {
}
