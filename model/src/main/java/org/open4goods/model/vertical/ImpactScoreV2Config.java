package org.open4goods.model.vertical;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

/**
 * The {@code v2} block of an ImpactScore configuration: the ImpactScore V2 methodology of a vertical, kept next to
 * the frozen V1 matrix so that both can coexist (simulation, rollback). It is only applied when
 * {@link ImpactScoreConfig#effectiveMethodologyVersion()} is 2. Design: {@code docs/impactscore/v2-design.md},
 * section 1.2.
 */
public class ImpactScoreV2Config {

	/** Default of {@link #getMinRatedForPositionLabel()}: below 100 rated products, a position label is not shown. */
	public static final int DEFAULT_MIN_RATED_FOR_POSITION_LABEL = 100;

	/** V2 matrix: sums to 1, without {@code DATA_QUALITY} nor {@code ECOSCORE}. */
	private Map<String, Double> criteriasPonderation = new HashMap<>();

	/**
	 * Share of the matrix weight, in ]0, 1], the criteria actually filled in must reach for a product to be rated.
	 * The threshold is inclusive.
	 */
	private Double minRealWeightCoverage;

	/**
	 * Whether a product without EPREL data is excluded. Defaults to {@code true}. When {@code false}, a product
	 * without EPREL data stays only if its coverage reaches {@link #minRealWeightCoverage} (GOU-2165).
	 */
	private Boolean eprelRequired = Boolean.TRUE;

	/**
	 * EPREL categories ({@code eprelDatas.eprelCategory}, compared ignoring case) of products that do not belong to
	 * the vertical and are excluded, for example monitors in the tv vertical (GOU-2165). Empty by default.
	 */
	private List<String> excludedEprelCategories = new ArrayList<>();

	/**
	 * Smallest rated cohort of the vertical for which a position label is exposed; below it, the score and the
	 * position stay, without label. Defaults to {@value #DEFAULT_MIN_RATED_FOR_POSITION_LABEL} (gate 2, GOU-1877).
	 */
	private Integer minRatedForPositionLabel = DEFAULT_MIN_RATED_FOR_POSITION_LABEL;

	/** Position labels, best first, thresholds strictly decreasing, the last one at 0. */
	private List<ImpactScorePositionLabel> positionLabels = new ArrayList<>();

	/** Per-criterion overrides of the attribute's {@code scoring} block, keyed by criterion. */
	private Map<String, ScoreScoringConfig> criteriaScoring = new HashMap<>();

	public Map<String, Double> getCriteriasPonderation() {
		return criteriasPonderation;
	}

	public void setCriteriasPonderation(Map<String, Double> criteriasPonderation) {
		this.criteriasPonderation = criteriasPonderation;
	}

	public Double getMinRealWeightCoverage() {
		return minRealWeightCoverage;
	}

	public void setMinRealWeightCoverage(Double minRealWeightCoverage) {
		this.minRealWeightCoverage = minRealWeightCoverage;
	}

	public Boolean getEprelRequired() {
		return eprelRequired;
	}

	public void setEprelRequired(Boolean eprelRequired) {
		this.eprelRequired = eprelRequired;
	}

	public List<String> getExcludedEprelCategories() {
		return excludedEprelCategories;
	}

	public void setExcludedEprelCategories(List<String> excludedEprelCategories) {
		this.excludedEprelCategories = excludedEprelCategories;
	}

	public Integer getMinRatedForPositionLabel() {
		return minRatedForPositionLabel;
	}

	public void setMinRatedForPositionLabel(Integer minRatedForPositionLabel) {
		this.minRatedForPositionLabel = minRatedForPositionLabel;
	}

	public List<ImpactScorePositionLabel> getPositionLabels() {
		return positionLabels;
	}

	public void setPositionLabels(List<ImpactScorePositionLabel> positionLabels) {
		this.positionLabels = positionLabels;
	}

	public Map<String, ScoreScoringConfig> getCriteriaScoring() {
		return criteriaScoring;
	}

	public void setCriteriaScoring(Map<String, ScoreScoringConfig> criteriaScoring) {
		this.criteriaScoring = criteriaScoring;
	}
}
