package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.constraints.NotNull;

public class RecommandationsConfig {

	/**
	 *  The config for recommandations
	 */
	@NotNull
	private  List<RecommandationCriteria> recommandations = new ArrayList<RecommandationCriteria>();

	public List<RecommandationCriteria> getRecommandations() {
		return recommandations;
	}

	public void setRecommandations(List<RecommandationCriteria> recommandations) {
		this.recommandations = recommandations;
	}




}
