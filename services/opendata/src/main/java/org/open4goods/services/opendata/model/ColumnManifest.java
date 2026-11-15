package org.open4goods.services.opendata.model;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Column of a dataset in its manifest.
 *
 * @param name        CSV header of the column
 * @param fillRate    share of data rows with a non-empty value, from 0 to 1 (0 when the dataset has no row)
 * @param unit        unit of the values, absent when the column has none or the dataset does not describe it
 * @param description label of the column (impact datasets, GOU-2250), absent when not described
 * @param source      origin of the values, absent when not described
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ColumnManifest(String name, double fillRate, String unit, String description, String source) {

    /**
     * Column without description (GTIN and ISBN datasets).
     */
    public ColumnManifest(String name, double fillRate) {
        this(name, fillRate, null, null, null);
    }

    /**
     * @param rate fill rate measured while writing
     * @return this column with the measured fill rate
     */
    public ColumnManifest withFillRate(double rate) {
        return new ColumnManifest(name, rate, unit, description, source);
    }
}
