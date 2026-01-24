package org.open4goods.model.vertical.scoring;

/**
 * Supported normalization methods for attribute-driven scores.
 */
public enum ScoreNormalizationMethod {
    SIGMA,
    PERCENTILE,
    MINMAX_FIXED,
    MINMAX_OBSERVED,
    MINMAX_QUANTILE,
    FIXED_MAPPING,
    BINARY,
    CONSTANT
}
