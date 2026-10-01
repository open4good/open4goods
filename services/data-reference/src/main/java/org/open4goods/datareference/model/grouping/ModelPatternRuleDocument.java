package org.open4goods.datareference.model.grouping;

import java.util.List;
import java.util.Objects;

/**
 * Git-authored collection of versioned, reviewed family pattern rules.
 *
 * @param schemaVersion fixed JSON contract identifier
 * @param rules immutable, individually validated pattern rules
 */
public record ModelPatternRuleDocument(String schemaVersion, List<ModelPatternRule> rules) {

    /** Current schema identifier for model-pattern-rule resources. */
    public static final String SCHEMA_VERSION = "https://open4goods.org/schema/model-pattern-rules-1.json";

    /**
     * Validates the contract version and rule list.
     */
    public ModelPatternRuleDocument {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new ModelPatternRuleValidationException("Unsupported model pattern rule schema: " + schemaVersion);
        }
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        if (rules.stream().anyMatch(Objects::isNull)) {
            throw new ModelPatternRuleValidationException("rules must not contain null");
        }
    }
}
