package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;

/**
 * Factory resolving normalization strategies from method identifiers.
 */
public final class NormalizationStrategyFactory {

    private NormalizationStrategyFactory() {
    }

    public static NormalizationStrategy strategyFor(ScoreNormalizationMethod method) {
        if (method == null) {
            return null;
        }
        return switch (method) {
            case SIGMA -> new SigmaNormalizationStrategy();
            case PERCENTILE -> new PercentileNormalizationStrategy();
            case MINMAX_FIXED -> new MinMaxFixedNormalizationStrategy();
            case MINMAX_QUANTILE -> new MinMaxQuantileNormalizationStrategy();
            case FIXED_MAPPING -> new FixedMappingNormalizationStrategy();
            case BINARY -> new BinaryNormalizationStrategy();
            case CONSTANT -> new ConstantNormalizationStrategy();
        };
    }
}
