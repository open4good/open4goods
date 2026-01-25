package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.math.BigDecimal;
import java.util.Map;

import org.open4goods.model.StandardiserService;
import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;
import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * Normalization based on a fixed mapping table.
 */
public class FixedMappingNormalizationStrategy implements NormalizationStrategy {

    @Override
    public NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException {
        if (value == null) {
            throw new ValidationException("Empty value in relativization");
        }

        ScoreNormalizationParams params = resolveParams(attributeConfig);
        Map<String, Double> mapping = params.getMapping();
        if (mapping == null || mapping.isEmpty()) {
            throw new ValidationException("Missing mapping for fixed-mapping normalization");
        }

        String key = normalizeKey(value);
        Double mapped = mapping.get(key);
        if (mapped == null) {
            mapped = mapping.get(value.toString());
        }
        if (mapped == null) {
            throw new ValidationException("Missing mapping entry for value " + key + ", attribute : " + attributeConfig);
        }

        double scaled = Math.max(resolveScaleMin(attributeConfig), Math.min(resolveScaleMax(attributeConfig), mapped));
        return new NormalizationResult(scaled, false);
    }

    private String normalizeKey(Double value) {
        if (value == null) {
            return null;
        }
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
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
