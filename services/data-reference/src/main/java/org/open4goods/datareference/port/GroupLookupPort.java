package org.open4goods.datareference.port;

import java.util.List;
import java.util.Optional;

import org.open4goods.datareference.model.ProjectionSurface;
import org.open4goods.datareference.model.grouping.GroupId;
import org.open4goods.datareference.model.grouping.GroupIndexEntry;

/**
 * Exact and batch lookup against the compact group index (GOU-199).
 *
 * <p>A surface keeps its own index, built only from that surface's own
 * {@code GroupAssignment}s (see {@link org.open4goods.datareference.model.grouping.GroupIndexEntry}),
 * so these methods never need to redact a shared entry after the fact: a
 * group id with no entry for a surface is simply absent from that surface's
 * index, exactly as if it had never been confirmed there.
 */
public interface GroupLookupPort {

    /**
     * Looks up one group by its exact id.
     *
     * @param groupId stable group identifier
     * @param surface surface whose index is queried
     * @return the entry, or empty when it does not exist on that surface
     */
    Optional<GroupIndexEntry> find(GroupId groupId, ProjectionSurface surface);

    /**
     * Looks up several groups by id in one call.
     *
     * @param groupIds stable group identifiers
     * @param surface surface whose index is queried
     * @return the entries that exist on that surface, in the order their ids were given
     */
    List<GroupIndexEntry> findAll(List<GroupId> groupIds, ProjectionSurface surface);
}
