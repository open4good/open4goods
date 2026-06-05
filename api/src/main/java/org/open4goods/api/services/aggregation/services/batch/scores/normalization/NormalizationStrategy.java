package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;

/**
 * Strategy for normalising a raw attribute value into a [0, MAX_RATING] score scale.
 *
 * <p>Eight implementations cover the following algorithms:
 * {@code SIGMA}, {@code PERCENTILE}, {@code MINMAX_FIXED}, {@code MINMAX_OBSERVED},
 * {@code MINMAX_QUANTILE}, {@code FIXED_MAPPING}, {@code BINARY}, {@code CONSTANT}.
 * The correct implementation is selected by
 * {@link NormalizationStrategyFactory#strategyFor(org.open4goods.model.vertical.scoring.ScoreNormalizationMethod)}.
 */
public interface NormalizationStrategy {

    /**
     * Normalises {@code value} using batch statistics from {@code context} and
     * scale / mapping configuration from {@code attributeConfig}.
     *
     * @param value           raw numeric value to normalise; may be {@code null}
     *                        for some strategies (e.g. CONSTANT) but most throw
     *                        {@link ValidationException} when {@code null}
     * @param context         batch-wide statistics (cardinality, value frequencies)
     * @param attributeConfig vertical attribute configuration providing scale bounds,
     *                        mapping tables, and normalization parameters
     * @return normalised value in [0, MAX_RATING] plus a legacy-fallback flag
     * @throws ValidationException if the value cannot be normalised (e.g. null input,
     *                             missing configuration, zero-range bounds)
     */
    NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException;
}
