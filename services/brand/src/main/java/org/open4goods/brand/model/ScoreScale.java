package org.open4goods.brand.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Numeric scale of a {@link CompanyScore}, used to normalise heterogeneous
 * provider scores onto a comparable 0-100 range.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScoreScale {

    private Double min;
    private Double max;
    private boolean higherIsBetter = true;

    public ScoreScale() {
    }

    public ScoreScale(Double min, Double max, boolean higherIsBetter) {
        this.min = min;
        this.max = max;
        this.higherIsBetter = higherIsBetter;
    }

    public Double getMin() {
        return min;
    }

    public void setMin(Double min) {
        this.min = min;
    }

    public Double getMax() {
        return max;
    }

    public void setMax(Double max) {
        this.max = max;
    }

    public boolean isHigherIsBetter() {
        return higherIsBetter;
    }

    public void setHigherIsBetter(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }
}
