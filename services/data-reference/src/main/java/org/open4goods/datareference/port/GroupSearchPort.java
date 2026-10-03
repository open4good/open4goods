package org.open4goods.datareference.port;

import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;

/**
 * Prefix candidate search against the compact group index (GOU-199).
 *
 * <p>Returns both {@code CONFIRMED} and {@code CANDIDATE} entries -- see
 * {@link org.open4goods.datareference.model.grouping.GroupConfirmation} --
 * since a prefix match surfaced for curation is, by construction, not yet a
 * membership claim. Callers that only want confirmed groups filter on
 * {@link GroupIndexEntry#confirmation()}.
 */
public interface GroupSearchPort {

    /**
     * Searches groups whose search tokens start with the request's prefix.
     *
     * @param request prefix, group type, page position and size
     * @param surface surface whose index is queried
     * @return one page of matching entries, ordered by group id for stable pagination
     */
    ScanPage<GroupIndexEntry> searchByPrefix(GroupSearchRequest request, ProjectionSurface surface);
}
