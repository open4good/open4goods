package org.open4goods.model.vertical;

/**
 * A position label of the ImpactScore V2: the label of a rated product whose share of rated products it ranks
 * better than is at least {@link #getMinBetterThan()}.
 *
 * <p>Labels are listed best first, with strictly decreasing thresholds and the last one at 0, so that every rated
 * product gets exactly one label (design GOU-1878, sections 1.2 and 2.4).
 */
public class ImpactScorePositionLabel {

	/** Label key, for example {@code excellent}; the wording is resolved by the clients (i18n). */
	private String level;

	/** Lowest share of rated products, between 0 and 1, the product ranks better than to get this label. */
	private Double minBetterThan;

	public ImpactScorePositionLabel() {
	}

	public ImpactScorePositionLabel(String level, Double minBetterThan) {
		this.level = level;
		this.minBetterThan = minBetterThan;
	}

	public String getLevel() {
		return level;
	}

	public void setLevel(String level) {
		this.level = level;
	}

	public Double getMinBetterThan() {
		return minBetterThan;
	}

	public void setMinBetterThan(Double minBetterThan) {
		this.minBetterThan = minBetterThan;
	}
}
