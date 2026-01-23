package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Returns a constant score configured in YAML.
 */
public class ConstantNormalizationStrategy implements NormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Double constantValue = params.getConstantValue();
        if (constantValue == null) {
            constantValue = resolveNeutralValue(attributeConfig);
        }

        double scaled = Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), constantValue));
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

    private double resolveNeutralValue(AttributeConfig attributeConfig) {
        double min = resolveScaleMin(attributeConfig);
        double max = resolveScaleMax(attributeConfig);
        return (min + max) / 2.0;
    }
}
