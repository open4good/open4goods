package org.open4goods.model.data;

import java.util.ArrayList;
import java.util.List;

import org.open4goods.model.Localisable;

/**
 * Strong icecat mapping, thanks to them !
 */
public class FeatureGroup {
	
	private Integer categoryFeatureId;
	private Integer categoryFeatureGroupId;
	
	private Integer icecatCategoryFeatureGroupId;
	
	/**
	 * Erased name if needed
	 * @param categoryFeatureGroupId
	 */
	private Localisable name;
	
	private List<Integer> featuresId = new ArrayList<>();
	
	
	public FeatureGroup() {
	
	}
	public FeatureGroup(int categoryFeatureGroupId) {
		this.icecatCategoryFeatureGroupId = categoryFeatureGroupId;
	
	}
	public void setCategoryFeatureId(Integer categoryFeatureId) {
		this.categoryFeatureId = categoryFeatureId;
	}
	public Integer getCategoryFeatureGroupId() {
		return categoryFeatureGroupId;
	}
	public void setCategoryFeatureGroupId(Integer categoryFeatureGroupId) {
		this.categoryFeatureGroupId = categoryFeatureGroupId;
	}
	public Integer getIcecatCategoryFeatureGroupId() {
		return icecatCategoryFeatureGroupId;
	}
	public void setIcecatCategoryFeatureGroupId(Integer icecatFeatureGroupId) {
		this.icecatCategoryFeatureGroupId = icecatFeatureGroupId;
	}
	public Localisable getName() {
		return name;
	}
	public void setName(Localisable name) {
		this.name = name;
	}
	public List<Integer> getFeaturesId() {
		return featuresId;
	}
	public void setFeaturesId(List<Integer> featuresId) {
		this.featuresId = featuresId;
	}
	public Integer getCategoryFeatureId() {
		return categoryFeatureId;
	}
	
	
	
}
