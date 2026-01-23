package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Binary normalization based on a threshold.
 */
public class BinaryNormalizationStrategy implements NormalizationStrategy {

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
}
