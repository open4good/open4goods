package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Base class for all {@link NormalizationStrategy} implementations.
 *
 * <p>Provides the shared helper methods that every strategy needs:
 * scale-bound resolution, neutral-value computation, degenerate-distribution
 * handling, and params extraction. Concrete strategies only need to implement
 * {@link #normalize}.
 */
public abstract class AbstractNormalizationStrategy implements NormalizationStrategy {

    /**
     * Returns the configured lower bound of the output scale, defaulting to {@code 0.0}.
     */
    protected final double resolveScaleMin(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMin() == null) {
            return 0.0;
        }
        return scoring.getScale().getMin();
    }

    /**
     * Returns the configured upper bound of the output scale, defaulting to
     * {@link StandardiserService#DEFAULT_MAX_RATING}.
     */
    protected final double resolveScaleMax(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMax() == null) {
            return StandardiserService.DEFAULT_MAX_RATING;
        }
        return scoring.getScale().getMax();
    }

    /**
     * Returns the mid-point of the configured scale — used as the fallback value
     * when a distribution is degenerate.
     */
    protected final double resolveNeutralValue(AttributeConfig attributeConfig) {
        return (resolveScaleMin(attributeConfig) + resolveScaleMax(attributeConfig)) / 2.0;
    }

    /**
     * Resolves normalization parameters from {@code attributeConfig}, returning a
     * default (empty) instance when the config or its scoring block is absent.
     */
    protected final ScoreNormalizationParams resolveParams(AttributeConfig attributeConfig) {
        if (attributeConfig == null || attributeConfig.getScoring() == null) {
            return new ScoreNormalizationParams();
        }
        return attributeConfig.getScoring().getNormalization().getParams();
    }

    /**
     * Applies the configured {@link ScoreDegeneratePolicy} and returns an appropriate
     * {@link NormalizationResult}:
     * <ul>
     *   <li>{@code NEUTRAL} (default) — returns the scale mid-point, not flagged as legacy</li>
     *   <li>{@code ERROR} — throws {@link ValidationException} with {@code message}</li>
     *   <li>any other value — returns the scale mid-point, flagged as legacy-fallback</li>
     * </ul>
     *
     * @param attributeConfig config used to resolve the policy and neutral value
     * @param message         exception message used when policy is {@code ERROR}
     */
    protected final NormalizationResult handleDegenerate(AttributeConfig attributeConfig, String message)
            throws ValidationException {
        ScoreDegeneratePolicy policy = attributeConfig == null || attributeConfig.getScoring() == null
                ? ScoreDegeneratePolicy.NEUTRAL
                : attributeConfig.getScoring().getDegenerateDistributionPolicy();
        if (policy == null || ScoreDegeneratePolicy.NEUTRAL.equals(policy)) {
            return new NormalizationResult(resolveNeutralValue(attributeConfig), false);
        }
        if (ScoreDegeneratePolicy.ERROR.equals(policy)) {
            throw new ValidationException(message);
        }
        return new NormalizationResult(resolveNeutralValue(attributeConfig), true);
    }

    /**
     * Maps a {@code [0, 1]} normalized value onto the configured {@code [scaleMin, scaleMax]}
     * range and clamps the result to that range.
     */
    protected final double scaleAndClamp(double normalized, AttributeConfig attributeConfig) {
        double min = resolveScaleMin(attributeConfig);
        double max = resolveScaleMax(attributeConfig);
        double scaled = min + normalized * (max - min);
        return Math.max(min, Math.min(max, scaled));
    }
}
