package org.open4goods.commons.config.yml;

import java.util.HashMap;
import java.util.Map;

import org.checkerframework.checker.units.qual.K;
import org.open4goods.commons.model.Localisable;

public class ImpactScoreConfig {

	/**
	 * The pondered criterias, composing the eco score
	 */
	private Map<String, Double> criteriasPonderation = new HashMap<>();

	////////////////////////
	// Audit / justification
	///////////////////////
	
	private Localisable<String, ImpactScoreTexts> texts = new Localisable<>();
	
	
	// TODO : The validate method (check sums is 1)
	public Map<String, Double> getCriteriasPonderation() {
		return criteriasPonderation;
	}
	public void setCriteriasPonderation(Map<String, Double> criteriasPonderation) {
		this.criteriasPonderation = criteriasPonderation;
	}
	public Localisable<String, ImpactScoreTexts> getTexts() {
		return texts;
	}
	public void setTexts(Localisable<String, ImpactScoreTexts> texts) {
		this.texts = texts;
	}
	

}
