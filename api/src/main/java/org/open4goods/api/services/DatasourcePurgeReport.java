package org.open4goods.api.services;

import java.util.List;

/**
 * Result of a datasource purge over the product index (GOU-1592).
 *
 * @param datasourceName  datasource whose data was (or would be) removed, exact value of
 *                        {@code AggregatedPrice.datasourceName} and of the {@code datasourceCodes} keys
 * @param scope           what was (or would be) removed: offers only, or every piece of data of the datasource
 *                        (GOU-2101)
 * @param dryRun          true when nothing was written
 * @param scannedProducts products read from the index
 * @param touchedProducts products holding data of the datasource in the scope (would be or were rewritten)
 * @param removedOffers   offers of the datasource removed from {@code price.offers}
 * @param savedProducts   products actually written back, always 0 in dry run
 * @param failedProducts  products skipped because of an unexpected error (see the logs)
 * @param sampleGtins     a few GTINs of touched products, for a manual check
 * @param durationMs      duration of the scan
 */
public record DatasourcePurgeReport(
		String datasourceName,
		DatasourcePurgeScope scope,
		boolean dryRun,
		long scannedProducts,
		long touchedProducts,
		long removedOffers,
		long savedProducts,
		long failedProducts,
		List<String> sampleGtins,
		long durationMs) {
}
