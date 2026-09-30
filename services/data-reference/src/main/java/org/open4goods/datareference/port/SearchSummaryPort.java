package org.open4goods.datareference.port;

import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;
import org.open4goods.datareference.model.projection.SearchSummary;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/** Builds lexical search fields exclusively from values eligible for a surface. */
public interface SearchSummaryPort {

    /**
     * Produces lexical terms from already-resolved values.
     *
     * <p>Raw assertions are deliberately absent from this boundary. A source
     * value denied for a publication surface therefore cannot be reintroduced
     * through search text.
     *
     * @param gtin product identity
     * @param surface publication surface
     * @param replayInputs fixed rebuild coordinates
     * @param resolvedValues policy-filtered resolved values
     * @return lexical search summary
     */
    SearchSummary summarize(Gtin gtin, ProjectionSurface surface, ProjectionReplayInputs replayInputs,
            List<ResolvedValue> resolvedValues);
}
