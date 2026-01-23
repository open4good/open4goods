package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.rating.Cardinality;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreDegeneratePolicy;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Normalization based on sigma bounds around the mean.
 */
public class SigmaNormalizationStrategy implements NormalizationStrategy {

    private static final double EPSILON = 0.000001;

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        Cardinality abs = context.getCardinality();
        if (abs == null) {
            throw new ValidationException("Missing cardinality for sigma normalization");
        }

        Double mean = abs.getAvg();
        Double sigma = abs.getStdDev();

        if (mean == null || sigma == null) {
            throw new ValidationException("Missing statistics for sigma normalization");
        }

        ScoreDegeneratePolicy policy = resolveDegeneratePolicy(attributeConfig);
        if (sigma == 0.0) {
            return handleDegenerate(policy, attributeConfig);
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        double k = params.getSigmaK() == null ? 2.0 : params.getSigmaK();

        double lowerBound = mean - (k * sigma);
        double upperBound = mean + (k * sigma);

        if (Math.abs(upperBound - lowerBound) < EPSILON) {
            return handleDegenerate(policy, attributeConfig);
        }

        double normalized = (value - lowerBound) / (upperBound - lowerBound);
        double scaled = normalized * resolveScaleMax(attributeConfig);
        double clamped = Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), scaled));
        return new NormalizationResult(
                clamped,
                false);
    }

    private NormalizationResult handleDegenerate(ScoreDegeneratePolicy policy, AttributeConfig attributeConfig)
            throws ValidationException {
        if (policy == null || ScoreDegeneratePolicy.NEUTRAL.equals(policy)) {
            return new NormalizationResult(resolveNeutralValue(attributeConfig), false);
        }
        if (ScoreDegeneratePolicy.ERROR.equals(policy)) {
            throw new ValidationException("Degenerate distribution for sigma normalization");
        }
        return new NormalizationResult(resolveNeutralValue(attributeConfig), true);
    }

    private ScoreDegeneratePolicy resolveDegeneratePolicy(AttributeConfig attributeConfig) {
        if (attributeConfig == null || attributeConfig.getScoring() == null) {
            return ScoreDegeneratePolicy.NEUTRAL;
        }
        return attributeConfig.getScoring().getDegenerateDistributionPolicy();
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
