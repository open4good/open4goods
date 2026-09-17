package org.open4goods.datareference.service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.resolution.ResolvedValue;
import org.open4goods.datareference.port.DomainSliceComposer;

/**
 * Builds a surface component in a stable canonical order.
 *
 * <p>The resolver deliberately returns a collection, while the persisted
 * projection is compared during replay. Sorting the values at the single
 * composition boundary means iteration order in a source store cannot alter a
 * projection's canonical JSON representation.
 */
public final class DeterministicDomainSliceComposer implements DomainSliceComposer {

    /**
     * Composes the typed domain slices without deriving or filtering values.
     *
     * <p>Eligibility is the responsibility of the resolver and the input
     * ports. This class owns only the stable assembly of their outputs.
     */
    @Override
    public ProductReferenceProjection compose(
            Gtin gtin,
            ProjectionSurface surface,
            ProjectionReplayInputs replayInputs,
            List<ResolvedValue> resolvedValues,
            OfferSummary offers,
            EvaluationSummary evaluation,
            SearchSummary search,
            Instant builtAt) {
        Objects.requireNonNull(resolvedValues, "resolvedValues must not be null");
        List<ResolvedValue> canonicalValues = resolvedValues.stream()
                .peek(value -> Objects.requireNonNull(value, "resolvedValues must not contain null"))
                .sorted(Comparator.comparing(value -> value.attribute().externalForm()))
                .toList();
        return new ProductReferenceProjection(gtin, surface, replayInputs, builtAt, canonicalValues,
                offers, evaluation, search);
    }
}
