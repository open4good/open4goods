package org.open4goods.api.services.backfill;

import java.util.List;

/**
 * Position of a backfill scan (GOU-2266): the point in time (PIT) it reads and the sort values ({@code _shard_doc})
 * of the last product of the last batch fully handled. A resumed run continues right after it, as long as the point
 * in time is still open on the cluster; once it has expired, the scan starts again from the beginning, which the
 * idempotent operations make safe.
 *
 * @param pitId       point in time id
 * @param searchAfter sort values of the last product handled
 */
public record BackfillCursor(String pitId, List<Long> searchAfter) {

	public BackfillCursor {
		searchAfter = searchAfter == null ? List.of() : List.copyOf(searchAfter);
	}
}
