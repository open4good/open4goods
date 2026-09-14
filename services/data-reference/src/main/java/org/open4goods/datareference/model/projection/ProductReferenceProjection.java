package org.open4goods.datareference.model.projection;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.open4goods.datareference.model.CanonicalAttributeId;
import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * One surface component within a product-reference storage envelope.
 *
 * <p>Built per surface rather than filtered on read: the same product legally
 * carries different content on the public site, the B2B API and the open-data
 * export, and a single document filtered at read time is one forgotten filter
 * away from publishing content that was never licensed for that surface.
 *
 * @param gtin product identity, the leaf identity of the model
 * @param surface surface this document may be served on
 * @param replayInputs versioned inputs used to resolve and evaluate the component
 * @param builtAt instant the document was assembled
 * @param resolvedValues reference values, ordered by canonical attribute
 * @param offers current offer state supplied by the offer domain
 * @param evaluation deterministic score and cohort output supplied by evaluation
 * @param search lexical search fields supplied by search composition
 */
public record ProductReferenceProjection(
        Gtin gtin,
        ProjectionSurface surface,
        ProjectionReplayInputs replayInputs,
        Instant builtAt,
        List<ResolvedValue> resolvedValues,
        OfferSummary offers,
        EvaluationSummary evaluation,
        SearchSummary search) {

    /**
     * Validates the document and rejects a repeated canonical attribute.
     */
    public ProductReferenceProjection {
        Objects.requireNonNull(gtin, "gtin must not be null");
        Objects.requireNonNull(surface, "surface must not be null");
        Objects.requireNonNull(replayInputs, "replayInputs must not be null");
        Objects.requireNonNull(builtAt, "builtAt must not be null");
        resolvedValues = List.copyOf(Objects.requireNonNull(resolvedValues, "resolvedValues must not be null"));
        Set<CanonicalAttributeId> seen = new HashSet<>();
        for (ResolvedValue resolved : resolvedValues) {
            Objects.requireNonNull(resolved, "resolvedValues must not contain null");
            if (!seen.add(resolved.attribute())) {
                throw new IllegalArgumentException("duplicate resolved attribute: " + resolved.attribute());
            }
        }
        Objects.requireNonNull(offers, "offers must not be null");
        Objects.requireNonNull(evaluation, "evaluation must not be null");
        Objects.requireNonNull(search, "search must not be null");
    }
}
