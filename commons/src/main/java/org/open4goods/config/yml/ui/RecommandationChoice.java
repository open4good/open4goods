package org.open4goods.config.yml.ui;

public class RecommandationChoice {

	/** Name, that will be used as ID for html radios and i18n translation **/
	private String name;

	/**
	 * The corresponding elastic query fragment
	 */
	private String queryFragment;

	/** If true, will be the default choice **/
	private Boolean defaultChoice = false;


	/**
	 * The lg-* to apply to criterias for cells disposition
	 */
	private Integer divid = 2;

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getQueryFragment() {
		return queryFragment;
	}

	public void setQueryFragment(final String queryFragment) {
		this.queryFragment = queryFragment;
	}

	public Boolean getDefaultChoice() {
		return defaultChoice;
	}

	public void setDefaultChoice(final Boolean defaultChoice) {
		this.defaultChoice = defaultChoice;
	}

	public Integer getDivid() {
		return divid;
	}

	public void setDivid(final Integer divid) {
		this.divid = divid;
	}





}
