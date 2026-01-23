package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import org.open4goods.model.exceptions.ValidationException;
import org.open4goods.model.vertical.AttributeConfig;

/**
 * Strategy for normalizing raw attribute values into score scales.
 */
public interface NormalizationStrategy {

    NormalizationResult normalize(Double value, NormalizationContext context, AttributeConfig attributeConfig)
            throws ValidationException;
}
