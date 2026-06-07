package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Binary normalization: maps a value to either {@code scaleMax} (pass) or
 * {@code scaleMin} (fail) based on a configured threshold.
 *
 * <p>The direction is controlled by {@code params.greaterIsPass}:
 * {@code true} (default) means {@code value >= threshold} passes;
 * {@code false} means {@code value <= threshold} passes.
 */
public class BinaryNormalizationStrategy extends AbstractNormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Double threshold = params.getThreshold();
        if (threshold == null) {
            throw new ValidationException("Missing threshold for binary normalization");
        }

        Boolean greaterIsPass = params.getGreaterIsPass();
        boolean pass = Boolean.TRUE.equals(greaterIsPass) ? value >= threshold : value <= threshold;
        double scaled = pass ? resolveScaleMax(attributeConfig) : resolveScaleMin(attributeConfig);
        return new NormalizationResult(scaled, false);
    }
}
