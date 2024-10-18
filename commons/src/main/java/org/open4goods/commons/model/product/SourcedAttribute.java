package org.open4goods.commons.model.product;

import org.open4goods.commons.model.attribute.Attribute;

public class SourcedAttribute {

	private String dataSourcename;
	private String value;
	private String language;
	private Integer icecatTaxonomyId;

	public SourcedAttribute(Attribute attr, String dataspourcename) {
		this.dataSourcename = dataspourcename;
		this.value = attr.getValue();
		this.icecatTaxonomyId = attr.getIcecatFeatureId();
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

}
