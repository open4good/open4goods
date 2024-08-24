package org.open4goods.commons.model.dto;

import org.open4goods.commons.config.yml.ui.VerticalConfig;

public class ExpandedTaxonomy {
	
	private Integer taxonomyId;
	
	private String taxonomyName;
	
	private Long total;
	
	private VerticalConfig associatedVertical;


	

	public Integer getTaxonomyId() {
		return taxonomyId;
	}

	public void setTaxonomyId(Integer taxonomyId) {
		this.taxonomyId = taxonomyId;
	}

	public String getTaxonomyName() {
		return taxonomyName;
	}

	public void setTaxonomyName(String taxonomyName) {
		this.taxonomyName = taxonomyName;
	}

	public Long getTotal() {
		return total;
	}

	public void setTotal(Long total) {
		this.total = total;
	}

	public VerticalConfig getAssociatedVertical() {
		return associatedVertical;
	}

	public void setAssociatedVertical(VerticalConfig associatedVertical) {
		this.associatedVertical = associatedVertical;
	}
	
	
	

}
