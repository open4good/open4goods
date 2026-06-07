package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.EnumMap;
import java.util.Map;

import org.open4goods.model.vertical.scoring.ScoreNormalizationMethod;

/**
 * Factory resolving normalization strategies from method identifiers.
 *
 * <p>All strategy implementations are stateless, so a single singleton per method is
 * allocated at class-load time and reused across all calls.
 */
public final class NormalizationStrategyFactory {

    private static final Map<ScoreNormalizationMethod, NormalizationStrategy> STRATEGIES;

    static {
        EnumMap<ScoreNormalizationMethod, NormalizationStrategy> m = new EnumMap<>(ScoreNormalizationMethod.class);
        m.put(ScoreNormalizationMethod.SIGMA,            new SigmaNormalizationStrategy());
        m.put(ScoreNormalizationMethod.PERCENTILE,       new PercentileNormalizationStrategy());
        m.put(ScoreNormalizationMethod.MINMAX_FIXED,     new MinMaxFixedNormalizationStrategy());
        m.put(ScoreNormalizationMethod.MINMAX_OBSERVED,  new MinMaxObservedNormalizationStrategy());
        m.put(ScoreNormalizationMethod.MINMAX_QUANTILE,  new MinMaxQuantileNormalizationStrategy());
        m.put(ScoreNormalizationMethod.FIXED_MAPPING,    new FixedMappingNormalizationStrategy());
        m.put(ScoreNormalizationMethod.BINARY,           new BinaryNormalizationStrategy());
        m.put(ScoreNormalizationMethod.CONSTANT,         new ConstantNormalizationStrategy());
        STRATEGIES = m;
    }

    private NormalizationStrategyFactory() {
    }

    /**
     * Returns the singleton {@link NormalizationStrategy} for the given method.
     *
     * @param method the normalisation algorithm; {@code null} returns {@code null}
     * @return the strategy instance, or {@code null} if {@code method} is {@code null}
     */
    public static NormalizationStrategy strategyFor(ScoreNormalizationMethod method) {
        if (method == null) {
            return null;
        }
        return STRATEGIES.get(method);
    }
}
