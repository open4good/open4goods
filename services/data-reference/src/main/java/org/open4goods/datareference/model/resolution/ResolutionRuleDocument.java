package org.open4goods.datareference.model.resolution;

import java.util.List;
import java.util.Objects;

/**
 * Git-authored collection of versioned, per-attribute, per-surface resolution rules.
 *
 * <p>There is deliberately no document-level field carrying a default source
 * order: every rule names its own attribute and surface, which is what keeps a
 * preference intended for one field from silently leaking into another.
 *
 * @param schemaVersion fixed JSON contract identifier
 * @param rules immutable, per-(attribute, surface) resolution rules
 */
public record ResolutionRuleDocument(String schemaVersion, List<ResolutionRule> rules) {

    /** Current schema identifier for resolution-rule resources. */
    public static final String SCHEMA_VERSION = "https://open4goods.org/schema/resolution-rules-1.json";

    /**
     * Validates the contract version and rule list.
     */
    public ResolutionRuleDocument {
        if (!SCHEMA_VERSION.equals(schemaVersion)) {
            throw new IllegalArgumentException("Unsupported resolution rule schema: " + schemaVersion);
        }
        rules = List.copyOf(Objects.requireNonNull(rules, "rules must not be null"));
        if (rules.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("rules must not contain null");
        }
    }
}
