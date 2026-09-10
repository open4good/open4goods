package org.open4goods.datareference.port;

import org.open4goods.datareference.model.SourceId;
import org.open4goods.datareference.model.SourceRecordHead;

/**
 * Bulk read over stored heads, for rebuilding projections without providers.
 *
 * <p>Separate from {@link SourceRecordHeadStore} because the two have different
 * failure modes: a point read that fails is an error, while a scan of millions
 * of records has to state whether one bad record stops everything.
 */
public interface SourceRecordReplayScanner {

    /**
     * Scans every stored head.
     *
     * @param request page position, size, ordering and failure behavior
     * @return one page of heads
     */
    ScanPage<SourceRecordHead> scanAll(ScanRequest request);

    /**
     * Scans the heads of one source.
     *
     * @param sourceId source to scan
     * @param request page position, size, ordering and failure behavior
     * @return one page of heads
     */
    ScanPage<SourceRecordHead> scanBySource(SourceId sourceId, ScanRequest request);
}
