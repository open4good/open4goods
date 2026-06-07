package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.Collections;
import java.util.Map;

import org.open4goods.model.rating.Cardinality;

/**
 * Immutable snapshot of batch-wide statistics needed for score normalization.
 *
 * <p>{@code valueFrequencies} is guaranteed non-null (falls back to an empty map
 * when the caller passes {@code null}).
 *
 * @param cardinality      observed min/max/mean/stddev across the batch; may be {@code null}
 *                         for strategies that do not require it (e.g. CONSTANT)
 * @param valueFrequencies value→count frequency map; required by PERCENTILE and MINMAX_QUANTILE
 */
public record NormalizationContext(Cardinality cardinality, Map<Double, Integer> valueFrequencies) {

    public NormalizationContext {
        if (valueFrequencies == null) {
            valueFrequencies = Collections.emptyMap();
        }
    }
}
