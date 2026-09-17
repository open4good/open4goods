package org.open4goods.datareference.model.resolution;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.ProjectionSurface;

/** Immutable lookup over Git-authored, per-concept resolution rules. */
public final class ResolutionRuleRegistry {

    private final Map<Coordinate, ResolutionRule> rules;

    /**
     * Builds a registry and rejects competing rules for a coordinate.
     *
     * @param authoredRules rules read from the versioned registry
     */
    public ResolutionRuleRegistry(List<ResolutionRule> authoredRules) {
        Map<Coordinate, ResolutionRule> indexed = new LinkedHashMap<>();
        for (ResolutionRule rule : List.copyOf(Objects.requireNonNull(authoredRules, "authoredRules must not be null"))) {
            ResolutionRule nonNullRule = Objects.requireNonNull(rule, "authoredRules must not contain null");
            Coordinate coordinate = new Coordinate(nonNullRule.attribute(), nonNullRule.surface());
            if (indexed.putIfAbsent(coordinate, nonNullRule) != null) {
                throw new IllegalArgumentException("duplicate resolution rule: " + coordinate);
            }
        }
        rules = Map.copyOf(indexed);
    }

    /**
     * Finds the sole rule governing an attribute and surface.
     *
     * @param attribute canonical field
     * @param surface publication surface
     * @return configured rule, or empty when the field must not be projected
     */
    public Optional<ResolutionRule> find(CanonicalAttributeId attribute, ProjectionSurface surface) {
        return Optional.ofNullable(rules.get(new Coordinate(attribute, surface)));
    }

    private record Coordinate(CanonicalAttributeId attribute, ProjectionSurface surface) {
    }
}
