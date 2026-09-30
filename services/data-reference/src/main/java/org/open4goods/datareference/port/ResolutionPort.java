package org.open4goods.datareference.port;

import java.time.Instant;
import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.SourceRecordHead;
import org.open4goods.datareference.model.resolution.ResolvedValue;

/**
 * Chooses one value per canonical field from everything sources asserted.
 *
 * <p>The surface and the instant are arguments, not context, because usage
 * policy is evaluated inside resolution: evidence a policy forbids is removed
 * before selection, so it cannot win, cannot influence a derivation, and cannot
 * appear in a conflict report that gets exposed outside operations.
 */
public interface ResolutionPort {

    /**
     * Resolves every canonical field of one product for one surface.
     *
     * @param gtin product identity
     * @param heads current heads of every record attached to the product
     * @param surface surface the result will be published on
     * @param at instant the policies are evaluated at
     * @return resolved values, ordered by canonical attribute
     */
    List<ResolvedValue> resolve(Gtin gtin, List<SourceRecordHead> heads, ProjectionSurface surface, Instant at);
}
