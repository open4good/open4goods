package org.open4goods.model.vertical;

import java.util.HashMap;
import java.util.Map;

/**
 * Scoring main principle is to compose score from ratings, using ponderations, declared in the map values. Global score is computed after individual scores, so globalscores can be
 * composed of individual scores
 */
public record ScoringAggregationConfig(Map<String, Double> globalScore, Map<String, Map<String, Double>> scores) {

        public ScoringAggregationConfig() {
                this(new HashMap<>(), new HashMap<>());
        }

	public Map<String, Double> getGlobalScore() {
		return globalScore;
	}


	public Map<String, Map<String, Double>> getScores() {
		return scores;
	}






}
