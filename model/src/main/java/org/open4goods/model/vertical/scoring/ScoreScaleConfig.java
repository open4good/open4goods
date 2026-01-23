package org.open4goods.model.vertical.scoring;

/**
 * Defines the normalization scale boundaries.
 */
public class ScoreScaleConfig {

    /**
     * Minimum value for the normalized score scale.
     */
    private Double min = 0.0;

    /**
     * Maximum value for the normalized score scale.
     */
    private Double max = 5.0;

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
}
