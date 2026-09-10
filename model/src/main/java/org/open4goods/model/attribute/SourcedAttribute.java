package org.open4goods.model.attribute;

import org.apache.commons.lang3.StringUtils;

public class SourcedAttribute {

	/** ISO 639-2 code used when a datasource does not state a language. */
	public static final String UNDETERMINED_LANGUAGE = "und";

	private String dataSourcename;
	private String value;
	private String cleanedValue;
	private String language;
	private Integer icecatTaxonomyId;
	private String name;

	/**
	 * Copies one datasource contribution, keeping the language the datasource
	 * actually stated. A datasource that states no language yields
	 * {@value #UNDETERMINED_LANGUAGE} rather than a serving-domain guess.
	 *
	 * @param attr           attribute as provided by the datasource
	 * @param datasourcename name of the contributing datasource
	 */
	public SourcedAttribute(Attribute attr, String datasourcename) {
		this.dataSourcename = datasourcename;
		this.value = attr.getValue();
		this.icecatTaxonomyId = attr.getIcecatFeatureId();
		this.name=attr.getName();
		this.language = normalizeLanguage(attr.getLanguage());
	}

	/**
	 * @param language language stated by a datasource, possibly absent
	 * @return the stated language, or {@value #UNDETERMINED_LANGUAGE} when none
	 */
	public static String normalizeLanguage(String language) {
		return StringUtils.isBlank(language) ? UNDETERMINED_LANGUAGE : language;
	}

	public SourcedAttribute() {
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;

		SourcedAttribute that = (SourcedAttribute) o;

		return dataSourcename != null ? dataSourcename.equals(that.dataSourcename) : that.dataSourcename == null;
	}

	
	@Override
	public String toString() {
		return dataSourcename + ":" + value;
	}
	@Override
	public int hashCode() {
		return dataSourcename != null ? dataSourcename.hashCode() : 0;
	}

	public String getDataSourcename() {
		return dataSourcename;
	}

	public void setDataSourcename(String dataSourcename) {
		this.dataSourcename = dataSourcename;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public Integer getIcecatTaxonomyId() {
		return icecatTaxonomyId;
	}

	public void setIcecatTaxonomyId(Integer icecatTaxonomyId) {
		this.icecatTaxonomyId = icecatTaxonomyId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getCleanedValue() {
		return cleanedValue;
	}

	public void setCleanedValue(String cleanedValue) {
		this.cleanedValue = cleanedValue;
	}

	/**
	 * @return language stated by the contributing datasource, or
	 *         {@value #UNDETERMINED_LANGUAGE}
	 */
	public String getLanguage() {
		return language;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

}
