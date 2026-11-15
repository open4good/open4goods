package org.open4goods.api.services.backfill;

import java.util.List;
import java.util.Map;

/**
 * Counters of a backfill or restore run (GOU-2266).
 *
 * @param operation       name of the {@link BackfillOperation}
 * @param dryRun          true when nothing was written
 * @param restore         true for a restore from a backup file, false for a backfill
 * @param scannedProducts products read from the index (restore: backup lines read)
 * @param matchedProducts products that needed the change (would be or were written); equals the scanned lines in a
 *                        restore
 * @param writtenProducts updates applied by Elasticsearch, always 0 in dry run
 * @param unchangedProducts updates with nothing left to do: {@code noop} (the product changed since it was read) or
 *                        404 (the product was deleted since)
 * @param failedProducts  updates rejected by Elasticsearch, see the logs
 * @param labelCounts     occurrences of each {@link BackfillChange#labels() label} among the matched products, for
 *                        instance the removed keys
 * @param sampleIds       ids of a few matched products, for a manual check
 * @param backupFile      JSONL backup file of the run, null in dry run
 * @param cursor          position after the last batch fully handled, to resume a stopped run; null for a restore
 * @param stopReason      null when the scan reached its end, {@code STOP_REQUESTED} or {@code BACKPRESSURE}
 *                        (Elasticsearch answered 429 to a search, a shard or a bulk item) otherwise
 * @param durationMs      duration of the run
 * @param details         operation-specific counters ({@link BackfillOperation#details()}), empty when none
 */
public record BackfillReport(
		String operation,
		boolean dryRun,
		boolean restore,
		long scannedProducts,
		long matchedProducts,
		long writtenProducts,
		long unchangedProducts,
		long failedProducts,
		Map<String, Long> labelCounts,
		List<String> sampleIds,
		String backupFile,
		BackfillCursor cursor,
		String stopReason,
		long durationMs,
		Map<String, Object> details) {

	/** Stop asked through {@link BackfillService#requestStop()}. */
	public static final String STOP_REQUESTED = "STOP_REQUESTED";

	/** Elasticsearch rejected a request with a 429 (queue full, circuit breaker). */
	public static final String BACKPRESSURE = "BACKPRESSURE";
}
