package org.open4goods.api.services.aggregation.services.batch.scores.normalization;

import java.util.Collections;
import java.util.Map;

import org.open4goods.model.rating.Cardinality;

/**
 * Holds context required for score normalization.
 */
public class NormalizationContext {

    private final Cardinality cardinality;
    private final Map<Double, Integer> valueFrequencies;

    public NormalizationContext(Cardinality cardinality, Map<Double, Integer> valueFrequencies) {
        this.cardinality = cardinality;
        this.valueFrequencies = valueFrequencies == null ? Collections.emptyMap() : valueFrequencies;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public Map<Double, Integer> getValueFrequencies() {
        return valueFrequencies;
    }
}
