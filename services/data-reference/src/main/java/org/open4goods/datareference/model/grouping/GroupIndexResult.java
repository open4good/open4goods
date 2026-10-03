package org.open4goods.datareference.model.grouping;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of one {@link org.open4goods.datareference.service.GroupIndexer} build.
 *
 * @param entries the rebuilt compact index, one entry per group id still carrying members
 * @param lineage split events detected against the previous membership, empty on a first build
 */
public record GroupIndexResult(List<GroupIndexEntry> entries, List<GroupLineageEvent> lineage) {

    /** Defensively copies both collections. */
    public GroupIndexResult {
        entries = List.copyOf(Objects.requireNonNull(entries, "entries must not be null"));
        lineage = List.copyOf(Objects.requireNonNull(lineage, "lineage must not be null"));
    }
}
