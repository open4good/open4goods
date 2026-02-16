package org.open4goods.model.vertical;

import java.util.Objects;

/**
 * Configuration for a hero-level KPI displayed on the dataviz page.
 */
public class DatavizHeroKpi {

    /**
     * The field path to aggregate (e.g. "attributes.indexed.WASHING_CAPACITY").
     */
    private String field;

    /**
     * Label to display for the KPI.
     */
    private String label;

    /**
     * Unit suffix (e.g. "kg", "rpm").
     */
    private String unit;

    /**
     * Aggregation type (e.g. "avg", "sum", "min", "max", "cardinality").
     * Default is "avg".
     */
    private String aggregation = "avg";

    public DatavizHeroKpi() {
    }

    public DatavizHeroKpi(String field, String label, String unit, String aggregation) {
        this.field = field;
        this.label = label;
        this.unit = unit;
        this.aggregation = aggregation;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getAggregation() {
        return aggregation;
    }

    public void setAggregation(String aggregation) {
        this.aggregation = aggregation;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatavizHeroKpi that = (DatavizHeroKpi) o;
        return Objects.equals(field, that.field) &&
                Objects.equals(label, that.label) &&
                Objects.equals(unit, that.unit) &&
                Objects.equals(aggregation, that.aggregation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(field, label, unit, aggregation);
    }
}
