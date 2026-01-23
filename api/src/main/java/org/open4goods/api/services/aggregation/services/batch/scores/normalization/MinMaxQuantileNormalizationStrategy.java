package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Normalization based on quantile bounds derived from observed values.
 */
public class MinMaxQuantileNormalizationStrategy implements NormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Double quantileLow = params.getQuantileLow();
        Double quantileHigh = params.getQuantileHigh();
        if (quantileLow == null || quantileHigh == null) {
            throw new ValidationException("Missing quantile bounds for min-max quantile normalization");
        }
        if (quantileHigh <= quantileLow) {
            throw new ValidationException("Invalid quantile bounds for min-max quantile normalization");
        }

        List<Double> values = expandValues(context.getValueFrequencies());
        if (values.isEmpty()) {
            return handleDegenerate(attributeConfig);
        }

        Collections.sort(values);
        Double lowBound = computeQuantile(values, quantileLow);
        Double highBound = computeQuantile(values, quantileHigh);
        if (lowBound == null || highBound == null || highBound <= lowBound) {
            return handleDegenerate(attributeConfig);
        }

        double normalized = (value - lowBound) / (highBound - lowBound);
        double scaled = normalized * resolveScaleMax(attributeConfig);
        return new NormalizationResult(
                Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), scaled)),
                false);
    }

    private List<Double> expandValues(Map<Double, Integer> frequencies) {
        if (frequencies == null || frequencies.isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        frequencies.forEach((value, count) -> {
            if (count == null || count <= 0) {
                return;
            }
            for (int i = 0; i < count; i += 1) {
                values.add(value);
            }
        });
        return values;
    }

    private Double computeQuantile(List<Double> values, double quantile) {
        if (values.isEmpty()) {
            return null;
        }
        double clamped = Math.max(0.0, Math.min(1.0, quantile));
        int index = (int) Math.round(clamped * (values.size() - 1));
        return values.get(index);
    }

    private NormalizationResult handleDegenerate(AttributeConfig attributeConfig) throws ValidationException {
        ScoreDegeneratePolicy policy = attributeConfig == null || attributeConfig.getScoring() == null
                ? ScoreDegeneratePolicy.NEUTRAL
                : attributeConfig.getScoring().getDegenerateDistributionPolicy();
        if (policy == null || ScoreDegeneratePolicy.NEUTRAL.equals(policy)) {
            return new NormalizationResult(resolveNeutralValue(attributeConfig), false);
        }
        if (ScoreDegeneratePolicy.ERROR.equals(policy)) {
            throw new ValidationException("Degenerate distribution for quantile normalization");
        }
        return new NormalizationResult(resolveNeutralValue(attributeConfig), true);
    }

    private ScoreNormalizationParams resolveParams(AttributeConfig attributeConfig) {
        if (attributeConfig == null || attributeConfig.getScoring() == null) {
            return new ScoreNormalizationParams();
        }
        return attributeConfig.getScoring().getNormalization().getParams();
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
