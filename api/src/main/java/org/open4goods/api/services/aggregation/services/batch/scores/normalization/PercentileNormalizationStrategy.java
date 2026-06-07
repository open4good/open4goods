package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;

/**
 * Normalization based on mid-rank percentile.
 *
 * <p>Computes the fraction of values strictly below {@code value} plus half of the
 * values equal to it, relative to the total count. The resulting percentile
 * ({@code [0, 1]}) is mapped onto {@code [scaleMin, scaleMax]}.
 */
public class PercentileNormalizationStrategy extends AbstractNormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality abs = context.cardinality();
        if (abs == null) {
            throw new ValidationException("Missing cardinality for percentile normalization");
        }

        Map<Double, Integer> frequencies = context.valueFrequencies();
        Integer totalCount = abs.getCount();
        if (frequencies == null || frequencies.isEmpty() || totalCount == null || totalCount == 0) {
            return handleDegenerate(attributeConfig, "Degenerate distribution for percentile normalization");
        }

        int countBelow = 0;
        int countAt = 0;
        for (Map.Entry<Double, Integer> entry : frequencies.entrySet()) {
            int cmp = Double.compare(entry.getKey(), value);
            if (cmp < 0) {
                countBelow += entry.getValue();
            } else if (cmp == 0) {
                countAt += entry.getValue();
            }
        }

        double percentile = (countBelow + 0.5 * countAt) / totalCount;
        return new NormalizationResult(scaleAndClamp(percentile, attributeConfig), false);
    }
}
