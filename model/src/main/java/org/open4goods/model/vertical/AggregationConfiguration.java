package org.open4goods.model.vertical;

/**
 * Configuration describing how an attribute or score should be aggregated.
 *
 * <p>
 * The configuration allows the caller to tailor histogram aggregations by
 * defining the preferred number of buckets and the interval used for range
 * aggregations. Both values are optional in the YAML definition in order to
 * keep backward compatibility with existing configurations.
 * </p>
 */
public class AggregationConfiguration {

    private Integer buckets;

    private Double interval;

    public Integer getBuckets() {
        return buckets;
    }

    public void setBuckets(Integer buckets) {
        this.buckets = buckets;
    }

    public Double getInterval() {
        return interval;
    }

    public void setInterval(Double interval) {
        this.interval = interval;
    }
}
