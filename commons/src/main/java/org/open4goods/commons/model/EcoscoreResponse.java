package org.open4goods.commons.model;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EcoscoreResponse {
    @JsonProperty("analysis")
    private String analysis;

    @JsonProperty("score_composition")
    private Map<String, Double> scoreComposition;

    @JsonProperty("criticalReview")
    private String criticalReview;

	public String getAnalysis() {
		return analysis;
	}

	public void setAnalysis(String analysis) {
		this.analysis = analysis;
	}

	public Map<String, Double> getScoreComposition() {
		return scoreComposition;
	}

	public void setScoreComposition(Map<String, Double> scoreComposition) {
		this.scoreComposition = scoreComposition;
	}

	public String getCriticalReview() {
		return criticalReview;
	}

	public void setCriticalReview(String criticalReview) {
		this.criticalReview = criticalReview;
	}



    // Getters and Setters
    
}
