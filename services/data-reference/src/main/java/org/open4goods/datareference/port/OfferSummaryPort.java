package org.open4goods.datareference.port;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.projection.OfferSummary;
import org.open4goods.datareference.model.projection.ProjectionReplayInputs;

/** Supplies the current, policy-scoped offer slice for one projection rebuild. */
public interface OfferSummaryPort {

    /**
     * Summarizes current offer heads without consulting a legacy product document.
     *
     * @param gtin product identity
     * @param surface publication surface whose policy is being built
     * @param replayInputs fixed rebuild coordinates
     * @return typed current-offer summary
     */
    OfferSummary summarize(Gtin gtin, ProjectionSurface surface, ProjectionReplayInputs replayInputs);
}
