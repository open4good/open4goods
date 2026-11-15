package org.open4goods.api.config.yml;

/**
 * Clearing of the scores held by products outside the batched verticals (GOU-2133), bound under
 * {@code scoreCleanup} in {@code application.yml}. Products without a vertical, or of a vertical the batch does not
 * run, are never loaded by the vertical batches, so their old scores, ranking and ImpactScore stay published. At the
 * end of the batch, {@code BatchService} counts them and, when {@link #enabled}, clears them.
 */
public class ScoreCleanupProperties {

	/**
	 * When false (default), the batch only counts and logs the products it would clear, and writes nothing.
	 */
	private boolean enabled = false;

	/** Maximum number of products cleared by one batch run; the rest waits for the next run. */
	private int maxProducts = 100_000;

	/** Number of products per bulk update. */
	private int bulkSize = 500;

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public int getMaxProducts() {
		return maxProducts;
	}

	public void setMaxProducts(int maxProducts) {
		this.maxProducts = maxProducts;
	}

	public int getBulkSize() {
		return bulkSize;
	}

	public void setBulkSize(int bulkSize) {
		this.bulkSize = bulkSize;
	}
}
