package org.open4goods.datareference.port;

import java.time.Instant;
import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.EvaluationSummary;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * Assembles the document a consumer reads.
 *
 * <p>The sole writer of a surface component. Typed offer, evaluation and search
 * inputs arrive already computed; this composes them with resolved reference
 * values and derives nothing of its own.
 */
public interface DomainSliceComposer {

    /**
     * Composes one product document.
     *
     * @param gtin product identity
     * @param surface surface the document will be served on
     * @param replayInputs fixed versions and instant used to build the component
     * @param resolvedValues resolved reference values
     * @param offers current offer state supplied by the offer domain
     * @param evaluation deterministic evaluation output
     * @param search lexical search fields
     * @param builtAt instant to record as the build time
     * @return the assembled document
     */
    ProductReferenceProjection compose(
            Gtin gtin,
            ProjectionSurface surface,
            ProjectionReplayInputs replayInputs,
            List<ResolvedValue> resolvedValues,
            OfferSummary offers,
            EvaluationSummary evaluation,
            SearchSummary search,
            Instant builtAt);
}
