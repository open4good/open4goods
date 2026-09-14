package org.open4goods.datareference.model.projection;

import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.value.DecimalValue;

/**
 * Deterministic evaluation output derived from eligible resolved values.
 *
 * @param ruleVersion version of the evaluation rule set
 * @param evaluatedAt fixed replay instant, never a wall-clock refresh time
 * @param scores named score values keyed by canonical attribute
 * @param cohortStatistics named cohort statistics used by the evaluation
 * @param missingInputs eligible inputs absent at {@code evaluatedAt}
 */
public record EvaluationSummary(RuleVersion ruleVersion, Instant evaluatedAt,
        Map<CanonicalAttributeId, DecimalValue> scores, Map<String, DecimalValue> cohortStatistics,
        List<CanonicalAttributeId> missingInputs) {

    /** Copies ordered evaluation output and rejects ambiguous fields. */
    public EvaluationSummary {
        Objects.requireNonNull(ruleVersion, "ruleVersion must not be null");
        Objects.requireNonNull(evaluatedAt, "evaluatedAt must not be null");
        scores = immutableValues(scores, "scores");
        cohortStatistics = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(
                cohortStatistics, "cohortStatistics must not be null")));
        missingInputs = List.copyOf(Objects.requireNonNull(missingInputs, "missingInputs must not be null"));
    }

    private static Map<CanonicalAttributeId, DecimalValue> immutableValues(
            Map<CanonicalAttributeId, DecimalValue> values, String name) {
        Map<CanonicalAttributeId, DecimalValue> checked = new LinkedHashMap<>();
        for (Map.Entry<CanonicalAttributeId, DecimalValue> entry : Objects.requireNonNull(values, name + " must not be null").entrySet()) {
            checked.put(Objects.requireNonNull(entry.getKey(), name + " must not contain null keys"),
                    Objects.requireNonNull(entry.getValue(), name + " must not contain null values"));
        }
        return Collections.unmodifiableMap(checked);
    }
}
