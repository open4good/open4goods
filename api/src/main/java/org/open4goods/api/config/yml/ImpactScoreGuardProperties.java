package org.open4goods.api.config.yml;

import java.util.HashMap;
import java.util.Map;

/**
 * Thresholds of the post-batch ImpactScore checks (GOU-1895), bound under {@code impactScoreGuard} in
 * {@code application.yml}. {@link #defaults} applies to every vertical; {@link #verticals} overrides it per
 * vertical id, field by field (a non-null value wins).
 */
public class ImpactScoreGuardProperties {

	/** Master switch. When false, no check runs and nothing can block an indexation. */
	private boolean enabled = true;

	/** Thresholds applied to every vertical. */
	private Thresholds defaults = new Thresholds();

	/** Per-vertical overrides, keyed by vertical id. Null fields fall back to {@link #defaults}. */
	private Map<String, Thresholds> verticals = new HashMap<>();

	/**
	 * Resolves the effective thresholds of a vertical.
	 *
	 * @param verticalId the vertical id
	 * @return thresholds with every field set
	 */
	public Thresholds forVertical(String verticalId) {
		Thresholds base = defaults == null ? new Thresholds() : defaults;
		Thresholds override = verticals == null ? null : verticals.get(verticalId);
		return override == null ? base : base.overriddenBy(override);
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public Thresholds getDefaults() {
		return defaults;
	}

	public void setDefaults(Thresholds defaults) {
		this.defaults = defaults;
	}

	public Map<String, Thresholds> getVerticals() {
		return verticals;
	}

	public void setVerticals(Map<String, Thresholds> verticals) {
		this.verticals = verticals;
	}

	/**
	 * Thresholds of one vertical. Fields are boxed so an override can leave them unset.
	 */
	public static class Thresholds {

		/** Blocking: minimum p95 - p5 spread, in ImpactScore points (0-20), below which the scale is compressed. */
		private Double minSpread = 1.0;

		/** Alert: maximum day-to-day move of the median, in ImpactScore points. */
		private Double maxMedianShift = 2.0;

		/** Alert: maximum day-to-day move of the mean real-weight coverage (1 - imputed share), in points of 0-1. */
		private Double maxCoverageShift = 0.10;

		/** Alert: maximum day-to-day drop of the rated or scored product count, as a ratio (0.2 = 20 %). */
		private Double maxCountDrop = 0.20;

		/** Alert: maximum day-to-day drop of the EPREL-matched product count, as a ratio. */
		private Double maxEprelDrop = 0.20;

		/** Minimum rated products for the distribution checks to be meaningful. */
		private Integer minProductsForDistribution = 20;

		Thresholds overriddenBy(Thresholds o) {
			Thresholds t = new Thresholds();
			t.minSpread = o.minSpread != null ? o.minSpread : minSpread;
			t.maxMedianShift = o.maxMedianShift != null ? o.maxMedianShift : maxMedianShift;
			t.maxCoverageShift = o.maxCoverageShift != null ? o.maxCoverageShift : maxCoverageShift;
			t.maxCountDrop = o.maxCountDrop != null ? o.maxCountDrop : maxCountDrop;
			t.maxEprelDrop = o.maxEprelDrop != null ? o.maxEprelDrop : maxEprelDrop;
			t.minProductsForDistribution = o.minProductsForDistribution != null ? o.minProductsForDistribution
					: minProductsForDistribution;
			return t;
		}

		public Double getMinSpread() {
			return minSpread;
		}

		public void setMinSpread(Double minSpread) {
			this.minSpread = minSpread;
		}

		public Double getMaxMedianShift() {
			return maxMedianShift;
		}

		public void setMaxMedianShift(Double maxMedianShift) {
			this.maxMedianShift = maxMedianShift;
		}

		public Double getMaxCoverageShift() {
			return maxCoverageShift;
		}

		public void setMaxCoverageShift(Double maxCoverageShift) {
			this.maxCoverageShift = maxCoverageShift;
		}

		public Double getMaxCountDrop() {
			return maxCountDrop;
		}

		public void setMaxCountDrop(Double maxCountDrop) {
			this.maxCountDrop = maxCountDrop;
		}

		public Double getMaxEprelDrop() {
			return maxEprelDrop;
		}

		public void setMaxEprelDrop(Double maxEprelDrop) {
			this.maxEprelDrop = maxEprelDrop;
		}

		public Integer getMinProductsForDistribution() {
			return minProductsForDistribution;
		}

		public void setMinProductsForDistribution(Integer minProductsForDistribution) {
			this.minProductsForDistribution = minProductsForDistribution;
		}
	}
}
