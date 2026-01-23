package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.Map;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Normalization based on mid-rank percentile.
 */
public class PercentileNormalizationStrategy implements NormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality abs = context.getCardinality();
        if (abs == null) {
            throw new ValidationException("Missing cardinality for percentile normalization");
        }

        Map<Double, Integer> frequencies = context.getValueFrequencies();
        Integer totalCount = abs.getCount();
        if (frequencies == null || frequencies.isEmpty() || totalCount == null || totalCount == 0) {
            return handleDegenerate(attributeConfig);
        }

        int countBelow = 0;
        int countAt = 0;

        for (Map.Entry<Double, Integer> entry : frequencies.entrySet()) {
            int comparison = Double.compare(entry.getKey(), value);
            if (comparison < 0) {
                countBelow += entry.getValue();
            } else if (comparison == 0) {
                countAt += entry.getValue();
            }
        }

        double percentile = (countBelow + (0.5 * countAt)) / totalCount;
        double scaled = percentile * resolveScaleMax(attributeConfig);
        return new NormalizationResult(
                Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), scaled)),
                false);
    }

    private NormalizationResult handleDegenerate(AttributeConfig attributeConfig) throws ValidationException {
        ScoreDegeneratePolicy policy = attributeConfig == null || attributeConfig.getScoring() == null
                ? ScoreDegeneratePolicy.NEUTRAL
                : attributeConfig.getScoring().getDegenerateDistributionPolicy();
        if (policy == null || ScoreDegeneratePolicy.NEUTRAL.equals(policy)) {
            return new NormalizationResult(resolveNeutralValue(attributeConfig), false);
        }
        if (ScoreDegeneratePolicy.ERROR.equals(policy)) {
            throw new ValidationException("Degenerate distribution for percentile normalization");
        }
        return new NormalizationResult(resolveNeutralValue(attributeConfig), true);
    }

    private double resolveScaleMin(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMin() == null) {
            return 0.0;
        }
        return scoring.getScale().getMin();
    }

    private double resolveScaleMax(AttributeConfig attributeConfig) {
        ScoreScoringConfig scoring = attributeConfig == null ? null : attributeConfig.getScoring();
        if (scoring == null || scoring.getScale() == null || scoring.getScale().getMax() == null) {
            return StandardiserService.DEFAULT_MAX_RATING;
        }
        return scoring.getScale().getMax();
    }

    private double resolveNeutralValue(AttributeConfig attributeConfig) {
        double min = resolveScaleMin(attributeConfig);
        double max = resolveScaleMax(attributeConfig);
        return (min + max) / 2.0;
    }
}
