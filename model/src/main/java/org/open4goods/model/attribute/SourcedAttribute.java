package org.open4goods.model.attribute;

public class SourcedAttribute {

	private String dataSourcename;
	private String value;
	private String cleanedValue;
	private String language;
	private Integer icecatTaxonomyId;
	private String name;

	public SourcedAttribute(Attribute attr, String datasourcename) {
		this.dataSourcename = datasourcename;
		this.value = attr.getValue();
		this.icecatTaxonomyId = attr.getIcecatFeatureId();
		this.name=attr.getName();
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

}
