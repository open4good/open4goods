package org.open4goods.commons.services.ai;

import java.util.Map;

public class SamplePromptEntity {
	
	private String analysis;
	private String score_composition;
	private String critical_review;
	private Map<String,Double> impactScoreConfig;
	public String getAnalysis() {
		return analysis;
	}
	public void setAnalysis(String analysis) {
		this.analysis = analysis;
	}
	public String getScore_composition() {
		return score_composition;
	}
	public void setScore_composition(String score_composition) {
		this.score_composition = score_composition;
	}
	public String getCritical_review() {
		return critical_review;
	}
	public void setCritical_review(String critical_review) {
		this.critical_review = critical_review;
	}
	public Map<String, Double> getimpactScoreConfig() {
		return impactScoreConfig;
	}
	public void setimpactScoreConfig(Map<String, Double> impactScoreConfig) {
		this.impactScoreConfig = impactScoreConfig;
	}
	
	

}
