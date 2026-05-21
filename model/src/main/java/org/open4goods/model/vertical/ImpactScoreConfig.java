package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.Map;

public class ImpactScoreConfig {

	/**
	 * The pondered criterias, composing the eco score
	 */
	private Map<String, Double> criteriasPonderation = new HashMap<>();

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

	public Map<String, Double> getCriteriasPonderation() {
		return criteriasPonderation;
	}
	public void setCriteriasPonderation(Map<String, Double> criteriasPonderation) {
		this.criteriasPonderation = criteriasPonderation;
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
