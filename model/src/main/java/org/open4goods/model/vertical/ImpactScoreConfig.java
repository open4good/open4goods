package org.open4goods.model.vertical;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.open4goods.model.vertical.scoring.ScoreScoringConfig;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * ImpactScore configuration of a vertical, read from {@code verticals/impactscores/<vertical>.yml}.
 *
 * <p>The file holds the frozen V1 matrix ({@link #getCriteriasPonderation()}) and, optionally, the {@code v2} block
 * ({@link #getV2()}); {@link #effectiveMethodologyVersion()} selects the active one. Every reader of the ImpactScore
 * goes through the accessors ({@link #activeCriteriasPonderation()}, {@link #isEprelRequired()}...), never through the
 * raw blocks, except the configuration generation which stays on V1. Design: {@code docs/impactscore/v2-design.md},
 * section 1.2.
 */
public class ImpactScoreConfig {

	/** The ImpactScore methodology of the audit of 2026-11-08 and before: imputed means, EPREL required. */
	public static final int METHODOLOGY_V1 = 1;

	/** The ImpactScore V2 methodology: no imputation, coverage threshold, position labels. */
	public static final int METHODOLOGY_V2 = 2;

	/** Methodology versions a configuration may declare or an environment override may force. */
	public static final List<Integer> SUPPORTED_METHODOLOGY_VERSIONS = List.of(METHODOLOGY_V1, METHODOLOGY_V2);

	/**
	 * Active methodology version declared by the file: 1 (default) or 2. The only key a switch to V2 changes.
	 */
	private Integer methodologyVersion = METHODOLOGY_V1;

	/**
	 * The pondered criterias, composing the eco score: the V1 matrix, frozen. Read it through
	 * {@link #activeCriteriasPonderation()}.
	 */
	private Map<String, Double> criteriasPonderation = new HashMap<>();

	/** The V2 methodology, applied only when the effective version is 2. */
	private ImpactScoreV2Config v2;

	/**
	 * Version forced by the environment ({@code impactscore.methodology-version-overrides}), set at load time.
	 * Never read from nor written to the file.
	 */
	@JsonIgnore
	private Integer methodologyVersionOverride;

	/** Fingerprint of the active matrix, see {@link #getMethodologyRevision()}. Computed, never read from the file. */
	@JsonIgnore
	private String methodologyRevision;

	/**
	 * Minimum number of distinct values required to use sigma scoring.
	 * When the distribution is too discrete (lower than this threshold),
	 * percentile scoring is used to preserve meaningful separation.
	 */
	private Integer minDistinctValuesForSigma;

	////////////////////////
	// Audit / justification
	///////////////////////


	// For auditability. Strange in config file, but to be marbered with with criterias generation
	private String yamlPrompt;

	private String aiJsonResponse;

	/**
	 * Short SHA-256 fingerprint (16 hex chars) of {@link #yamlPrompt} captured at
	 * generation time. Lets reviewers detect when the impactscore YAML was produced
	 * by an outdated prompt template; a mismatch between this revision and the
	 * hash of the current resolved prompt means the file should be regenerated.
	 * Populated by VerticalsGenerationService; do not edit by hand.
	 */
	private String promptRevision;

	private org.open4goods.model.ai.ImpactScoreAiResult aiResult;

	/**
	 * The version applied to the vertical: the environment override, else {@link #getMethodologyVersion()}, else 1.
	 *
	 * @return 1 or 2 once the configuration is validated
	 */
	public int effectiveMethodologyVersion() {
		if (methodologyVersionOverride != null) {
			return methodologyVersionOverride;
		}
		return methodologyVersion == null ? METHODOLOGY_V1 : methodologyVersion;
	}

	/**
	 * Whether an ImpactScore of the given methodology version may be shown: only a score of the vertical's current
	 * version is (design {@code docs/impactscore/v2-design.md}, 4.2).
	 *
	 * @param config             the vertical's configuration, {@code null} when unknown (no rule applied)
	 * @param methodologyVersion the version the product was assessed with ({@code Product.impactScoreMethodologyVersion()})
	 * @return {@code true} when the score may be shown
	 */
	public static boolean isCurrentMethodology(ImpactScoreConfig config, int methodologyVersion) {
		return config == null || config.effectiveMethodologyVersion() == methodologyVersion;
	}

	/**
	 * Whether the effective version is V2.
	 *
	 * @return {@code true} when {@link #effectiveMethodologyVersion()} is 2
	 */
	@JsonIgnore
	public boolean isV2Active() {
		return effectiveMethodologyVersion() == METHODOLOGY_V2;
	}

	/**
	 * The matrix of the effective version: the V1 matrix, or {@code v2.criteriasPonderation} in V2. There is no
	 * fallback to V1 when V2 is active without a {@code v2} block (rejected at load time): the matrix is then empty.
	 *
	 * @return the active weights, never {@code null}
	 */
	public Map<String, Double> activeCriteriasPonderation() {
		Map<String, Double> active;
		if (isV2Active()) {
			active = v2 == null ? null : v2.getCriteriasPonderation();
		} else {
			active = criteriasPonderation;
		}
		return active == null ? Map.of() : active;
	}

	/**
	 * Whether a product without EPREL data is excluded: always in V1, {@code v2.eprelRequired} (default
	 * {@code true}) in V2. The filter cannot be lifted without V2, and going back to V1 restores it.
	 *
	 * @return {@code true} when the EPREL data is required
	 */
	@JsonIgnore
	public boolean isEprelRequired() {
		if (!isV2Active() || v2 == null || v2.getEprelRequired() == null) {
			return true;
		}
		return v2.getEprelRequired();
	}

	/**
	 * The EPREL categories excluded from the vertical in the effective version (GOU-2165).
	 *
	 * @return {@code v2.excludedEprelCategories} in V2, an empty list in V1 (no category excluded)
	 */
	public List<String> excludedEprelCategories() {
		if (!isV2Active() || v2 == null || v2.getExcludedEprelCategories() == null) {
			return List.of();
		}
		return v2.getExcludedEprelCategories();
	}

	/**
	 * The coverage threshold of the effective version.
	 *
	 * @return {@code v2.minRealWeightCoverage} in V2, {@code null} in V1 (no threshold)
	 */
	public Double minRealWeightCoverage() {
		return isV2Active() && v2 != null ? v2.getMinRealWeightCoverage() : null;
	}

	/**
	 * The position labels of the effective version, best first.
	 *
	 * @return {@code v2.positionLabels} in V2, an empty list in V1 (no label)
	 */
	public List<ImpactScorePositionLabel> positionLabels() {
		if (!isV2Active() || v2 == null || v2.getPositionLabels() == null) {
			return List.of();
		}
		return v2.getPositionLabels();
	}

	/**
	 * The smallest rated cohort for which a position label is exposed, in the effective version.
	 *
	 * @return {@code v2.minRatedForPositionLabel} in V2 (default
	 *         {@value ImpactScoreV2Config#DEFAULT_MIN_RATED_FOR_POSITION_LABEL}), {@code null} in V1 (no label)
	 */
	public Integer minRatedForPositionLabel() {
		if (!isV2Active() || v2 == null) {
			return null;
		}
		return v2.getMinRatedForPositionLabel() == null ? ImpactScoreV2Config.DEFAULT_MIN_RATED_FOR_POSITION_LABEL
				: v2.getMinRatedForPositionLabel();
	}

	/**
	 * The position label of a rated product (design section 2.4): the first label, best first, whose threshold the
	 * product reaches, provided the rated cohort reaches {@link #minRatedForPositionLabel()}.
	 *
	 * @param betterThan share of rated products ranked below the product, between 0 and 1
	 * @param ratedCount number of rated products of the vertical
	 * @return the label key, {@code null} in V1 or when the rated cohort is too small
	 */
	public String positionLevel(double betterThan, long ratedCount) {
		Integer minRated = minRatedForPositionLabel();
		if (minRated == null || ratedCount < minRated) {
			return null;
		}
		for (ImpactScorePositionLabel label : positionLabels()) {
			if (label != null && label.getMinBetterThan() != null && label.getMinBetterThan() <= betterThan) {
				return label.getLevel();
			}
		}
		return null;
	}

	/**
	 * The scoring override of a criterion in the effective version.
	 *
	 * @param key the criterion
	 * @return {@code v2.criteriaScoring[key]} in V2, {@code null} in V1 or without override
	 */
	public ScoreScoringConfig criteriaScoring(String key) {
		if (!isV2Active() || v2 == null || v2.getCriteriaScoring() == null) {
			return null;
		}
		return v2.getCriteriaScoring().get(key);
	}

	/**
	 * Short SHA-256 fingerprint (8 hex characters) of the active methodology: the effective version and the active
	 * matrix, entries sorted by criterion. Stored on every assessed product ({@code impactScoreMeta}) to trace the
	 * matrix that produced a score. Computed at load time by {@link #refreshMethodologyRevision()}, and on first read
	 * for configurations built in code.
	 *
	 * @return the fingerprint
	 */
	public String getMethodologyRevision() {
		if (methodologyRevision == null) {
			refreshMethodologyRevision();
		}
		return methodologyRevision;
	}

	/**
	 * Recomputes {@link #getMethodologyRevision()}, after the matrix or the effective version changed.
	 */
	public void refreshMethodologyRevision() {
		StringBuilder canonical = new StringBuilder("v").append(effectiveMethodologyVersion());
		new TreeMap<>(activeCriteriasPonderation()).forEach((key, weight) -> canonical.append('|').append(key)
				.append('=').append(weight == null ? "null" : BigDecimal.valueOf(weight).stripTrailingZeros().toPlainString()));
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical.toString().getBytes(StandardCharsets.UTF_8));
			methodologyRevision = HexFormat.of().formatHex(digest).substring(0, 8);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("SHA-256 is not available", e);
		}
	}

	public Integer getMethodologyVersion() {
		return methodologyVersion;
	}
	public void setMethodologyVersion(Integer methodologyVersion) {
		this.methodologyVersion = methodologyVersion;
		this.methodologyRevision = null;
	}
	public ImpactScoreV2Config getV2() {
		return v2;
	}
	public void setV2(ImpactScoreV2Config v2) {
		this.v2 = v2;
		this.methodologyRevision = null;
	}
	public Integer getMethodologyVersionOverride() {
		return methodologyVersionOverride;
	}
	/**
	 * Forces the methodology version from the environment. The revision is recomputed on next read.
	 *
	 * @param methodologyVersionOverride the forced version, {@code null} to follow the file
	 */
	public void setMethodologyVersionOverride(Integer methodologyVersionOverride) {
		this.methodologyVersionOverride = methodologyVersionOverride;
		this.methodologyRevision = null;
	}

	/**
	 * The raw V1 matrix, as read from the file. Readers use {@link #activeCriteriasPonderation()}; only loading and
	 * the configuration generation (which stays on V1) read it directly.
	 *
	 * @return the V1 matrix
	 */
	public Map<String, Double> getCriteriasPonderation() {
		return criteriasPonderation;
	}
	public void setCriteriasPonderation(Map<String, Double> criteriasPonderation) {
		this.criteriasPonderation = criteriasPonderation;
		this.methodologyRevision = null;
	}
	public Integer getMinDistinctValuesForSigma() {
		return minDistinctValuesForSigma;
	}
	public void setMinDistinctValuesForSigma(Integer minDistinctValuesForSigma) {
		this.minDistinctValuesForSigma = minDistinctValuesForSigma;
	}
	public String getYamlPrompt() {
		return yamlPrompt;
	}
	public void setYamlPrompt(String yamlPrompt) {
		this.yamlPrompt = yamlPrompt;
	}
	public String getAiJsonResponse() {
		return aiJsonResponse;
	}
	public void setAiJsonResponse(String aiJsonResponse) {
		this.aiJsonResponse = aiJsonResponse;
	}
	public String getPromptRevision() {
		return promptRevision;
	}
	public void setPromptRevision(String promptRevision) {
		this.promptRevision = promptRevision;
	}
	public org.open4goods.model.ai.ImpactScoreAiResult getAiResult() {
		return aiResult;
	}
	public void setAiResult(org.open4goods.model.ai.ImpactScoreAiResult aiResult) {
		this.aiResult = aiResult;
	}


}
