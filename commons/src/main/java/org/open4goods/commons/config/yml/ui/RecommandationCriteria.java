package org.open4goods.commons.config.yml.ui;

import java.util.ArrayList;
import java.util.List;

public class RecommandationCriteria {

	private List<RecommandationChoice> choices = new ArrayList<>();

	/**
	 * The class suffix to apply (warning, danger, primary, default, success, ...)
	 */
	private String panelType ="success";



	public List<RecommandationChoice> getChoices() {
		return choices;
	}

	public void setChoices(final List<RecommandationChoice> choices) {
		this.choices = choices;
	}

	public String getPanelType() {
		return panelType;
	}

	public void setPanelType(final String panelType) {
		this.panelType = panelType;
	}




}
