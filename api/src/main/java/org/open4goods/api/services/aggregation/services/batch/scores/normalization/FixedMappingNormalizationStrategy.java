package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.math.BigDecimal;
import java.util.Map;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;
import org.open4goods.model.vertical.scoring.ScoreNormalizationParams;

/**
 * Normalization based on a fixed key→score mapping table declared in YAML.
 *
 * <p>The double key is normalised to a plain string (trailing zeros stripped,
 * e.g. {@code 1.0 → "1"}) before lookup. The raw {@link Double#toString()} form
 * is tried as a fallback to handle YAML-serialised keys that include a decimal point.
 */
public class FixedMappingNormalizationStrategy extends AbstractNormalizationStrategy {

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
            throw new ValidationException("Missing mapping entry for value " + key + ", attribute: " + attributeConfig);
        }

        double min = resolveScaleMin(attributeConfig);
        double max = resolveScaleMax(attributeConfig);
        double scaled = Math.max(min, Math.min(max, mapped));
        return new NormalizationResult(scaled, false);
    }

    /** Converts a double key to a plain-string form with trailing zeros stripped (e.g. {@code 1.0 → "1"}). */
    private String normalizeKey(Double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }
}
