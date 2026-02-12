package org.open4goods.model.vertical;

import java.util.Objects;

/**
 * Configuration overrides for a specific dataviz chart.
 */
public class DatavizChartOverride {

    /**
     * Overridden title for the chart.
     */
    private String title;

    /**
     * Overridden description for the chart.
     */
    private String description;

    public DatavizChartOverride() {
    }

    public DatavizChartOverride(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DatavizChartOverride that = (DatavizChartOverride) o;
        return Objects.equals(title, that.title) &&
                Objects.equals(description, that.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(title, description);
    }
}
