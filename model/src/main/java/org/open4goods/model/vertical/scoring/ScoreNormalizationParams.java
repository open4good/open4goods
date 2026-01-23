package org.open4goods.model.vertical.scoring;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds method-specific parameters for score normalization.
 */
public class ScoreNormalizationParams {

    /**
     * Sigma factor used by SIGMA normalization.
     */
    private Double sigmaK = 2.0;

    /**
     * Fixed minimum used by MINMAX_FIXED normalization.
     */
    private Double fixedMin;

    /**
     * Fixed maximum used by MINMAX_FIXED normalization.
     */
    private Double fixedMax;

    /**
     * Lower quantile bound used by MINMAX_QUANTILE normalization.
     */
    private Double quantileLow;

    /**
     * Upper quantile bound used by MINMAX_QUANTILE normalization.
     */
    private Double quantileHigh;

    /**
     * Mapping table used by FIXED_MAPPING normalization.
     */
    private Map<String, Double> mapping = new HashMap<>();

    /**
     * Constant value used by CONSTANT normalization.
     */
    private Double constantValue;

    /**
     * Threshold used by BINARY normalization.
     */
    private Double threshold;

    /**
     * Indicates whether BINARY normalization should return max score when value is greater than threshold.
     */
    private Boolean greaterIsPass = true;

    public Double getSigmaK() {
        return sigmaK;
    }

    public void setSigmaK(Double sigmaK) {
        this.sigmaK = sigmaK;
    }

    public Double getFixedMin() {
        return fixedMin;
    }

    public void setFixedMin(Double fixedMin) {
        this.fixedMin = fixedMin;
    }

    public Double getFixedMax() {
        return fixedMax;
    }

    public void setFixedMax(Double fixedMax) {
        this.fixedMax = fixedMax;
    }

    public Double getQuantileLow() {
        return quantileLow;
    }

    public void setQuantileLow(Double quantileLow) {
        this.quantileLow = quantileLow;
    }

    public Double getQuantileHigh() {
        return quantileHigh;
    }

    public void setQuantileHigh(Double quantileHigh) {
        this.quantileHigh = quantileHigh;
    }

    public Map<String, Double> getMapping() {
        return mapping;
    }

    public void setMapping(Map<String, Double> mapping) {
        this.mapping = mapping == null ? new HashMap<>() : mapping;
    }

    public Double getConstantValue() {
        return constantValue;
    }

    public void setConstantValue(Double constantValue) {
        this.constantValue = constantValue;
    }

    public Double getThreshold() {
        return threshold;
    }

    public void setThreshold(Double threshold) {
        this.threshold = threshold;
    }

    public Boolean getGreaterIsPass() {
        return greaterIsPass;
    }

    public void setGreaterIsPass(Boolean greaterIsPass) {
        this.greaterIsPass = greaterIsPass;
    }
}
