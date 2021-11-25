package org.open4goods.config.yml.ui;

import java.util.HashSet;
import java.util.Set;

public class CompensationConfig {

	/**
	 * The formula that will computes the c02 amount
	 */
	private String spelFormulaCo2Kg;

	/** if defined, compensation will be calculated only if those attributes are present**/
	private Set<String> requiredAttributes = new HashSet<>();

	/** The estimated price needed for a kg carbon compensation
	 * https://www.goodplanet.org/fr/nouvelle-tarification-de-la-compensation-carbone-volontaire/
	 * **/
	private Double kgCarbonCost = 22.0 / 1000.0;

	
	
	
	
	public String getSpelFormulaCo2Kg() {
		return spelFormulaCo2Kg;
	}

	public void setSpelFormulaCo2Kg(final String spelFormulaCo2Kg) {
		this.spelFormulaCo2Kg = spelFormulaCo2Kg;
	}

	public Double getKgCarbonCost() {
		return kgCarbonCost;
	}

	public void setKgCarbonCost(final Double kgCarbonCost) {
		this.kgCarbonCost = kgCarbonCost;
	}

	public Set<String> getRequiredAttributes() {
		return requiredAttributes;
	}

	public void setRequiredAttributes(final Set<String> requiredAttributes) {
		this.requiredAttributes = requiredAttributes;
	}






}
