package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Normalization based on a sigma (standard-deviation) window around the mean.
 *
 * <p>Computes {@code lowerBound = mean - k*sigma} and {@code upperBound = mean + k*sigma}
 * where {@code k} defaults to {@code 2.0} and is configurable via {@code params.sigmaK}.
 * The value is then mapped linearly from {@code [lower, upper]} onto
 * {@code [scaleMin, scaleMax]}. A zero or near-zero sigma triggers the configured
 * degenerate-distribution policy.
 */
public class SigmaNormalizationStrategy extends AbstractNormalizationStrategy {

    private static final double EPSILON = 0.000001;

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality abs = context.cardinality();
        if (abs == null) {
            throw new ValidationException("Missing cardinality for sigma normalization");
        }

        Double mean = abs.getAvg();
        Double sigma = abs.getStdDev();
        if (mean == null || sigma == null) {
            throw new ValidationException("Missing statistics for sigma normalization");
        }

        if (sigma == 0.0) {
            return handleDegenerate(attributeConfig, "Degenerate distribution for sigma normalization");
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        double k = params.getSigmaK() == null ? 2.0 : params.getSigmaK();

        double lowerBound = mean - k * sigma;
        double upperBound = mean + k * sigma;
        if (Math.abs(upperBound - lowerBound) < EPSILON) {
            return handleDegenerate(attributeConfig, "Degenerate distribution for sigma normalization");
        }

        double normalized = (value - lowerBound) / (upperBound - lowerBound);
        return new NormalizationResult(scaleAndClamp(normalized, attributeConfig), false);
    }
}
