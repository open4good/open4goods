package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Normalization based on quantile bounds derived from observed values.
 *
 * <p>Computes the {@code quantileLow} and {@code quantileHigh} percentiles of the
 * observed value distribution, then maps {@code value} linearly onto
 * {@code [scaleMin, scaleMax]}. Clamping ensures the result stays within the output
 * range even for extreme outliers.
 *
 * <p><b>Note:</b> The value distribution is expanded from the frequency map on every
 * call — appropriate for batch sizes up to a few thousand products.
 */
public class MinMaxQuantileNormalizationStrategy extends AbstractNormalizationStrategy {

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

        List<Double> values = expandValues(context.valueFrequencies());
        if (values.isEmpty()) {
            return handleDegenerate(attributeConfig, "Degenerate distribution for quantile normalization");
        }

        Collections.sort(values);
        Double lowBound = computeQuantile(values, quantileLow);
        Double highBound = computeQuantile(values, quantileHigh);
        if (lowBound == null || highBound == null || highBound <= lowBound) {
            return handleDegenerate(attributeConfig, "Degenerate distribution for quantile normalization");
        }

        double normalized = (value - lowBound) / (highBound - lowBound);
        return new NormalizationResult(scaleAndClamp(normalized, attributeConfig), false);
    }

    private List<Double> expandValues(Map<Double, Integer> frequencies) {
        if (frequencies == null || frequencies.isEmpty()) {
            return List.of();
        }
        List<Double> values = new ArrayList<>();
        frequencies.forEach((v, count) -> {
            if (count != null && count > 0) {
                for (int i = 0; i < count; i++) {
                    values.add(v);
                }
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
}
