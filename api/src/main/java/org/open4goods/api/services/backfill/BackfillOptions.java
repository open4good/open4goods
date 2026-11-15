package org.open4goods.api.services.backfill;

/**
 * Parameters of a backfill run (GOU-2266).
 *
 * @param dryRun               when true (the default of the endpoints), counts what would change and writes nothing,
 *                             backup included
 * @param batchSize            products read per page and written per bulk, 1 to {@value #MAX_BATCH_SIZE}
 * @param maxProductsPerSecond reading rate cap, in scanned products per second; 0 means no cap
 * @param resume               when true, continues the last stopped, interrupted or failed run of the same operation
 *                             and mode from its cursor, with the same backup file
 */
public record BackfillOptions(boolean dryRun, int batchSize, int maxProductsPerSecond, boolean resume) {

	/** Upper bound of {@link #batchSize()}: pages of projected sources and bulks stay small. */
	public static final int MAX_BATCH_SIZE = 5_000;

	public BackfillOptions {
		if (batchSize < 1 || batchSize > MAX_BATCH_SIZE) {
			throw new IllegalArgumentException("batchSize must be between 1 and " + MAX_BATCH_SIZE);
		}
		if (maxProductsPerSecond < 0) {
			throw new IllegalArgumentException("maxProductsPerSecond must be 0 (no cap) or positive");
		}
	}
}
