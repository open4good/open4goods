package org.open4goods.datareference.model.normalization;

/** Outcome of normalizing one source assertion. */
public enum NormalizationStatus {
    SUCCESS,
    NO_MAPPING,
    INVALID_NUMBER,
    UNKNOWN_UNIT,
    INCOMPATIBLE_DIMENSION,
    OUT_OF_RANGE,
    UNSUPPORTED_EVIDENCE
}
