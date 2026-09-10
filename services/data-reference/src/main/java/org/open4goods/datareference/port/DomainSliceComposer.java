package org.open4goods.datareference.port;

import java.time.Instant;
import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.DomainSlice;
import org.open4goods.datareference.model.projection.ProductReferenceProjection;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * Assembles the document a consumer reads.
 *
 * <p>The sole writer of a projection. Offer summaries, evaluation outputs and
 * search fields arrive as already-computed slices; this composes them with the
 * resolved reference values and derives nothing of its own, so that no field
 * has two producers that can disagree.
 */
public interface DomainSliceComposer {

    /**
     * Composes one product document.
     *
     * @param gtin product identity
     * @param surface surface the document will be served on
     * @param resolvedValues resolved reference values
     * @param slices contributions supplied by other domains
     * @param builtAt instant to record as the build time
     * @return the assembled document
     */
    ProductReferenceProjection compose(
            Gtin gtin,
            ProjectionSurface surface,
            List<ResolvedValue> resolvedValues,
            List<DomainSlice> slices,
            Instant builtAt);
}
