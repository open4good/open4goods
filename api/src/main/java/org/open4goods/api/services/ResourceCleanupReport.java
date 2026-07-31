package org.open4goods.api.services;

import org.apache.commons.io.FileUtils;

/**
 * Outcome of one orphan resource cleanup run.
 *
 * @param mode                   mode the run was executed with
 * @param aborted                true when the safety floor rejected the run, in which case no file was touched
 * @param abortReason            human readable cause when {@code aborted}, null otherwise
 * @param indexedProductCount    product count read from Elasticsearch before streaming
 * @param streamedProductCount   products actually streamed
 * @param activeKeyCount         distinct cache keys reachable from those products
 * @param scannedFileCount       files examined inside the product resource shards
 * @param skippedOutOfScopeCount files ignored because they live outside the shard layout (foreign caches)
 * @param preservedCount         files kept because a product still references them
 * @param gracePeriodCount       files kept because they were written within the grace period
 * @param orphanCount            files classified as orphaned
 * @param orphanBytes            cumulated size of the orphans
 * @param processedCount         orphans actually moved or deleted (always 0 in DRY_RUN)
 * @param failureCount           orphans that could not be moved or deleted
 */
public record ResourceCleanupReport(
		ResourceCleanupMode mode,
		boolean aborted,
		String abortReason,
		long indexedProductCount,
		long streamedProductCount,
		long activeKeyCount,
		long scannedFileCount,
		long skippedOutOfScopeCount,
		long preservedCount,
		long gracePeriodCount,
		long orphanCount,
		long orphanBytes,
		long processedCount,
		long failureCount) {

	public static ResourceCleanupReport aborted(ResourceCleanupMode mode, String reason, long indexedProductCount,
			long streamedProductCount) {
		return new ResourceCleanupReport(mode, true, reason, indexedProductCount, streamedProductCount, 0, 0, 0, 0, 0, 0,
				0, 0, 0);
	}

	/**
	 * @return the orphan volume in a human readable form
	 */
	public String reclaimableSize() {
		return FileUtils.byteCountToDisplaySize(orphanBytes);
	}
}
