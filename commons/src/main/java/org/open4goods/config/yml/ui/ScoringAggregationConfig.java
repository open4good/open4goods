package org.open4goods.config.yml.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Scoring main principle is to compose score from ratings, using ponderations, declared in the map values. Global score is computed after individual scores, so globalscores can be
 * composed of individual scores
 * @author goulven
 *
 */
public class ScoringAggregationConfig {

	private Map<String,Double> globalScore = new HashMap<>();

	private Map<String, Map<String,Double>> scores = new HashMap<>();

	public Map<String, Double> getGlobalScore() {
		return globalScore;
	}

	public void setGlobalScore(final Map<String, Double> globalScore) {
		this.globalScore = globalScore;
	}

	public Map<String, Map<String, Double>> getScores() {
		return scores;
	}

	public void setScores(final Map<String, Map<String, Double>> scores) {
		this.scores = scores;
	}





}
