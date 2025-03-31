package org.open4goods.ui.config.yml;

import java.util.ArrayList;
import java.util.List;

public class FunFactsConfig {
	
	private List<FunFactConfig> facts = new ArrayList<>();
	
	

	public List<FunFactConfig> getFacts() {
		return facts;
	}

	public void setFacts(List<FunFactConfig> funFacts) {
		this.facts = funFacts;
	}

	

}
