package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

/**
 * Result of a normalization computation, with optional note about legacy fallback.
 */
public record NormalizationResult(Double value, boolean legacy) {
}
