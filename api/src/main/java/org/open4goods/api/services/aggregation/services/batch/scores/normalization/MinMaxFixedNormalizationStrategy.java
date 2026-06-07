package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Normalization based on fixed min/max bounds declared in YAML config.
 *
 * <p>Maps {@code value} linearly from {@code [fixedMin, fixedMax]} onto
 * {@code [scaleMin, scaleMax]}, clamping to the output range.
 */
public class MinMaxFixedNormalizationStrategy extends AbstractNormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Double fixedMin = params.getFixedMin();
        Double fixedMax = params.getFixedMax();
        if (fixedMin == null || fixedMax == null) {
            throw new ValidationException("Missing fixed bounds for min-max normalization");
        }
        if (fixedMax <= fixedMin) {
            throw new ValidationException("Invalid fixed bounds for min-max normalization");
        }

        double normalized = (value - fixedMin) / (fixedMax - fixedMin);
        return new NormalizationResult(scaleAndClamp(normalized, attributeConfig), false);
    }
}
