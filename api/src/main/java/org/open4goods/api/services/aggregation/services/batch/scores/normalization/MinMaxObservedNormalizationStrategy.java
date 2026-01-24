package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Normalization based on observed min/max bounds from the current batch.
 */
public class MinMaxObservedNormalizationStrategy implements NormalizationStrategy {

    /**
     * Normalize a value using observed min/max bounds from the current batch.
     *
     * @param value           the raw value to normalize
     * @param context         the normalization context providing observed statistics
     * @param attributeConfig attribute configuration providing scale bounds
     * @return the normalized result on the configured scale
     * @throws ValidationException when required bounds are missing or invalid
     */
    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality cardinality = context == null ? null : context.getCardinality();
        if (cardinality == null) {
            throw new ValidationException("Missing cardinality for min-max observed normalization");
        }

        Double observedMin = cardinality.getMin();
        Double observedMax = cardinality.getMax();
        if (observedMin == null || observedMax == null) {
            throw new ValidationException("Missing observed bounds for min-max observed normalization");
        }
        if (observedMax <= observedMin) {
            throw new ValidationException("Invalid observed bounds for min-max observed normalization");
        }

        double normalized = (value - observedMin) / (observedMax - observedMin);
        double scaled = normalized * resolveScaleMax(attributeConfig);
        return new NormalizationResult(
                Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), scaled)),
                false);
    }

    /**
     * Resolve the configured scale minimum for the attribute.
     *
     * @param attributeConfig attribute configuration to inspect
     * @return the configured minimum or zero when absent
     */
    private double resolveScaleMin(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMin() == null) {
            return 0.0;
        }
        return scoring.getScale().getMin();
    }

    /**
     * Resolve the configured scale maximum for the attribute.
     *
     * @param attributeConfig attribute configuration to inspect
     * @return the configured maximum or the default maximum rating when absent
     */
    private double resolveScaleMax(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMax() == null) {
            return StandardiserService.DEFAULT_MAX_RATING;
        }
        return scoring.getScale().getMax();
    }
}
