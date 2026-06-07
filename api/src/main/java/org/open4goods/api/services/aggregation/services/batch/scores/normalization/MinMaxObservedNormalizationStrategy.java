package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;

/**
 * Normalization based on observed min/max bounds collected from the current batch.
 *
 * <p>Maps {@code value} linearly from {@code [observedMin, observedMax]} onto
 * {@code [scaleMin, scaleMax]}, clamping to the output range.
 * When {@code observedMin == observedMax} the configured degenerate policy applies.
 */
public class MinMaxObservedNormalizationStrategy extends AbstractNormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality cardinality = context == null ? null : context.cardinality();
        if (cardinality == null) {
            throw new ValidationException("Missing cardinality for min-max observed normalization");
        }

        Double observedMin = cardinality.getMin();
        Double observedMax = cardinality.getMax();
        if (observedMin == null || observedMax == null) {
            throw new ValidationException("Missing observed bounds for min-max observed normalization: " + attributeConfig);
        }
        if (observedMax <= observedMin) {
            return handleDegenerate(attributeConfig,
                    "Invalid observed bounds for min-max observed normalization: " + attributeConfig);
        }

        double normalized = (value - observedMin) / (observedMax - observedMin);
        return new NormalizationResult(scaleAndClamp(normalized, attributeConfig), false);
    }
}
