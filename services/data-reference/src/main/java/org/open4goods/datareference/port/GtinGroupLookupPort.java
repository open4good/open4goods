package org.open4goods.datareference.port;

import java.util.List;

import org.open4goods.datareference.model.Gtin;
import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;

/**
 * GTIN-to-groups lookup against the compact group index (GOU-199).
 */
public interface GtinGroupLookupPort {

    /**
     * Returns every group one GTIN leaf confirms membership of, on one surface.
     *
     * @param gtin product identity
     * @param surface surface whose index is queried
     * @return the confirmed entries, in no particular order; empty when the GTIN
     *     confirms no group on that surface
     */
    List<GroupIndexEntry> findByGtin(Gtin gtin, ProjectionSurface surface);
}
