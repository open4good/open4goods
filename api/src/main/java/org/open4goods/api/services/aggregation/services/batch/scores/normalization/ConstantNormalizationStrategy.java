package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Returns a constant score declared in YAML config.
 *
 * <p>When {@code params.constantValue} is absent the scale mid-point is used.
 * The result is clamped to {@code [scaleMin, scaleMax]}.
 */
public class ConstantNormalizationStrategy extends AbstractNormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Double constantValue = params.getConstantValue();
        if (constantValue == null) {
            constantValue = resolveNeutralValue(attributeConfig);
        }

        double min = resolveScaleMin(attributeConfig);
        double max = resolveScaleMax(attributeConfig);
        double scaled = Math.max(min, Math.min(max, constantValue));
        return new NormalizationResult(scaled, false);
    }
}
