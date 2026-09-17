package org.open4goods.datareference.model.resolution;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.RuleVersion;
import org.open4goods.datareference.model.SourceId;

/**
 * Git-authored selection policy for one canonical attribute on one surface.
 *
 * <p>There is deliberately no global source order. A source can participate
 * only where the concept-and-surface rule names it, which prevents an EPREL or
 * Icecat preference intended for one field leaking into another.
 *
 * @param attribute canonical field selected by this rule
 * @param surface publication surface selected by this rule
 * @param version immutable rule coordinate
 * @param rankedSources sources in decreasing configured preference
 * @param regulatoryAuthority source authoritative for this specific field, or {@code null}
 */
public record ResolutionRule(
        CanonicalAttributeId attribute,
        ProjectionSurface surface,
        RuleVersion version,
        List<SourceId> rankedSources,
        SourceId regulatoryAuthority) {

    /** Validates the explicit, concept-specific source order. */
    public ResolutionRule {
        Objects.requireNonNull(attribute, "attribute must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(version, "version must not be null");
        rankedSources = List.copyOf(Objects.requireNonNull(rankedSources, "rankedSources must not be null"));
        if (rankedSources.isEmpty()) {
            throw new IllegalArgumentException("a resolution rule must name at least one source");
        }
        Set<SourceId> uniqueSources = new HashSet<>(rankedSources);
        if (uniqueSources.size() != rankedSources.size() || uniqueSources.contains(null)) {
            throw new IllegalArgumentException("rankedSources must contain each source exactly once");
        }
        if (regulatoryAuthority != null && !uniqueSources.contains(regulatoryAuthority)) {
            throw new IllegalArgumentException("regulatory authority must be among ranked sources");
        }
    }

    /**
     * Returns the zero-based source rank, or {@code -1} for a source that this
     * rule does not permit to participate.
     *
     * @param sourceId source to locate
     * @return configured rank or {@code -1}
     */
    public int rankOf(SourceId sourceId) {
        return rankedSources.indexOf(sourceId);
    }
}
