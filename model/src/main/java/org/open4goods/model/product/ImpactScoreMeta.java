package org.open4goods.model.product;

/**
 * How the last batch assessed the ImpactScore of a product: methodology, date, outcome and coverage of the active
 * matrix.
 *
 * <p>Written by the batch on every product of the scored cohort ({@code offersCount > 0}, not excluded), rated or
 * not, in V1 too; cleared with the scores of excluded or unmatched products; kept, like the score, by a product
 * without offers. The ECOSCORE {@link Score} stays the score itself. Design: {@code docs/impactscore/v2-design.md},
 * section 1.1.
 */
public class ImpactScoreMeta {

	/** Outcome of the assessment. */
	public enum Status {
		/** The product holds an ImpactScore. */
		RATED,
		/** The product holds no ImpactScore, see {@link NotRatedReason}. */
		NOT_RATED
	}

	/** Why a product assessed by the batch is not rated. */
	public enum NotRatedReason {
		/** The coverage of the criteria actually filled in is below the vertical's threshold (V2). */
		LOW_COVERAGE,
		/** No criterion of the active matrix is filled in. */
		NO_REAL_CRITERIA
	}

	/** Methodology applied (1 or 2). Missing on a product: legacy score, treated as version 1. */
	private Integer methodologyVersion;

	/** Fingerprint of the active matrix ({@code ImpactScoreConfig.getMethodologyRevision()}). */
	private String methodologyRevision;

	/** Instant of the batch that assessed the product, epoch milliseconds (same convention as {@code lastChange}). */
	private long scoredAt;

	/** {@code RATED} or {@code NOT_RATED}. */
	private String status;

	/** {@code LOW_COVERAGE} or {@code NO_REAL_CRITERIA}; {@code null} when rated. */
	private String notRatedReason;

	/** C: weight of the criteria actually filled in over the weight of the active matrix, between 0 and 1. */
	private Double realWeightCoverage;

	/** Criteria of the active matrix actually filled in. */
	private Integer realCriteriaCount;

	/** Criteria with a weight above 0 in the active matrix. */
	private Integer totalCriteriaCount;

	public Integer getMethodologyVersion() {
		return methodologyVersion;
	}

	public void setMethodologyVersion(Integer methodologyVersion) {
		this.methodologyVersion = methodologyVersion;
	}

	public String getMethodologyRevision() {
		return methodologyRevision;
	}

	public void setMethodologyRevision(String methodologyRevision) {
		this.methodologyRevision = methodologyRevision;
	}

	public long getScoredAt() {
		return scoredAt;
	}

	public void setScoredAt(long scoredAt) {
		this.scoredAt = scoredAt;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getNotRatedReason() {
		return notRatedReason;
	}

	public void setNotRatedReason(String notRatedReason) {
		this.notRatedReason = notRatedReason;
	}

	public Double getRealWeightCoverage() {
		return realWeightCoverage;
	}

	public void setRealWeightCoverage(Double realWeightCoverage) {
		this.realWeightCoverage = realWeightCoverage;
	}

	public Integer getRealCriteriaCount() {
		return realCriteriaCount;
	}

	public void setRealCriteriaCount(Integer realCriteriaCount) {
		this.realCriteriaCount = realCriteriaCount;
	}

	public Integer getTotalCriteriaCount() {
		return totalCriteriaCount;
	}

	public void setTotalCriteriaCount(Integer totalCriteriaCount) {
		this.totalCriteriaCount = totalCriteriaCount;
	}
}
